package ch.ethz.systems.netbench.xpt.sppifo.adaptations;

import ch.ethz.systems.netbench.core.config.NBProperties;

import java.util.*;
import java.util.function.*;

public class SpringAdaptationAlgorithm implements AdaptationAlgorithm, InversionTracker, BoundsInitializationAlgorithm {
    private enum SamplingMode {
        COUNT_INVERSIONS, COUNT_PACKETS, SUM_RANKS, SUM_INVERSION_MAGNITUDES
    }

    private class CostFunctionInput {
        public int bound;
        public int cost;
        public int previousBound;
        public int rank;
    }

    private Map<Integer, Double> inversionCounts;
    private Map<Integer, Double> inversionMagnitudes;
    private Map<Integer, Double> packetCounts;
    private Map<Integer, Double> rankSums;

    private SamplingMode samplingMode;
    private double alpha;
    private double sensitivity;
    private int perceivedMaxRank;

    public SpringAdaptationAlgorithm(NBProperties settings) {
        this.alpha = settings.getDoublePropertyOrFail("spring_alpha");
        this.sensitivity = settings.getDoublePropertyOrFail("spring_sensitivity");
        this.samplingMode = SamplingMode.valueOf(settings.getPropertyOrFail("spring_sample_mode"));
        this.perceivedMaxRank = 0;
        this.inversionCounts = new HashMap<Integer, Double>();
        this.packetCounts = new HashMap<Integer, Double>();
        this.inversionMagnitudes = new HashMap<Integer, Double>();
        this.rankSums = new HashMap<Integer, Double>();
    }

    private double scalingFactor(int numQueues) {
        return
            (this.sensitivity * this.perceivedMaxRank) /
            numQueues
        ;
    }

    private Map<Integer, Double> getBozonCounts() {
        switch(this.samplingMode) {
        case COUNT_INVERSIONS:
            return this.inversionCounts;

        case COUNT_PACKETS:
            return this.packetCounts;

        case SUM_INVERSION_MAGNITUDES:
            return this.inversionMagnitudes;

        case SUM_RANKS:
            return this.rankSums;

        default:
            throw new RuntimeException("unreachable");
        }
    }

    @Override
    public Map<Integer, Integer> nextBounds(Map<Integer, Integer> currentBounds, int destinationIndex, int rank) {
        if(this.perceivedMaxRank < rank) this.perceivedMaxRank = rank;
        this.packetCounts.put(destinationIndex, this.packetCounts.getOrDefault(destinationIndex, 0.0) + 1);
        this.packetCounts.put(destinationIndex, this.rankSums.getOrDefault(destinationIndex, 0.0) + rank);

        // this may be zero in the current implementation,
        // but only if all observed packets had a rank of zero,
        // in which case a single queue does a perfect job,
        // so it is not a problem if bounds do not change.
        double scale = this.scalingFactor(currentBounds.size());

        double[] next = new double[currentBounds.size()];
        for(int i = 0; i < next.length; ++i) {
            next[i] = (double)currentBounds.get(i);
        }

        double[] forces = new double[next.length];
        Map<Integer, Double> bozonCounts = this.getBozonCounts();

        // forget about some force-carrying particles
        double retainRatio = 1 - this.alpha;
        for(Map.Entry<Integer, Double> entry: bozonCounts.entrySet()) {
            bozonCounts.put(entry.getKey(), entry.getValue() * retainRatio);
        }

        for(int i = 0; i < forces.length; ++i) {
            double recorded = bozonCounts.getOrDefault(i, 0.0);
            forces[i] = recorded;
        }

        // in this version, bounds do not push around each other.
        double limit = this.perceivedMaxRank;
        for(int i = 1; i < forces.length; ++i) {
            double delta = forces[i] - forces[i - 1];
            delta *= scale;
            double currentLimit = limit;
            if(i < forces.length - 1) currentLimit = Math.min(currentLimit, next[i + 1] - 1);
            next[i] = Math.min(currentLimit, next[i]);
            next[i] = Math.max(next[i - 1] + 1, next[i] + delta);
        }

        Map<Integer, Integer> nextMapping = new HashMap<Integer, Integer>();
        for(int i = 0; i < next.length; ++i) {
            nextMapping.put(i, (int)Math.round(next[i]));
        }

        return nextMapping;
    }

    @Override
    public void inversionInQueue(int queueIndex, int inversionMagnitude) {
        double count = this.inversionCounts.getOrDefault(queueIndex, 0.0);
        double magnitude = this.inversionMagnitudes.getOrDefault(queueIndex, 0.0);
        this.inversionCounts.put(queueIndex, count + 1);
        this.inversionMagnitudes.put(queueIndex, inversionMagnitude + magnitude);
    }

    @Override
    public void initBounds(Map<Integer, Integer> destination, int perQueueCapacity) {
        int numQueues = destination.size();

        for(int i = 0; i < numQueues; ++i) {
            // initialize bounds to first n ranks.
            // algorithm should adapt to arbitrary ranks later on.
            destination.put(new Integer(i), new Integer(i));
        }
    }
}
