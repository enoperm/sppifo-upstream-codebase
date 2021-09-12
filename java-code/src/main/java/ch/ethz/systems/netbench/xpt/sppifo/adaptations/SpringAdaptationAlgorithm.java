package ch.ethz.systems.netbench.xpt.sppifo.adaptations;

import ch.ethz.systems.netbench.core.config.NBProperties;

import java.util.*;
import java.util.function.*;

public class SpringAdaptationAlgorithm implements AdaptationAlgorithm, InversionTracker, BoundsInitializationAlgorithm {
    private class CostFunctionInput {
        public int bound;
        public int cost;
        public int previousBound;
        public int rank;
    }

    private Map<Integer, Double> inversionCounts;
    private long samplingInterval;
    private long packetCounter;
    private double alpha;
    private double sensitivity;
    private int perceivedMaxRank;

    public SpringAdaptationAlgorithm(NBProperties settings) {
        this.alpha = settings.getDoublePropertyOrFail("spring_alpha");
        this.sensitivity = settings.getDoublePropertyOrFail("spring_sensitivity");
        this.samplingInterval = settings.getLongPropertyOrFail("spring_sample_interval"); 
        this.perceivedMaxRank = 0;
        this.inversionCounts = new HashMap<Integer, Double>();
        this.packetCounter = 0;
    }

    private double scalingFactor(int numQueues) {
        return
            (this.sensitivity * this.perceivedMaxRank) /
            (this.samplingInterval * (double)numQueues);
    }

    @Override
    public Map<Integer, Integer> nextBounds(Map<Integer, Integer> currentBounds, int destinationIndex, int rank) {
        ++this.packetCounter;
        if(this.perceivedMaxRank < rank) this.perceivedMaxRank = rank;
        if(this.packetCounter < samplingInterval) return currentBounds;

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
        for(int i = 0; i < forces.length; ++i) {
            forces[i] = this.inversionCounts.getOrDefault(i, 0.0);
        }

        // in this version, bounds do not push around each other.
        double limit = Math.min(this.perceivedMaxRank, next[next.length - 1]);
        for(int i = 1; i < forces.length; ++i) {
            double delta = forces[i] - forces[i - 1];
            delta *= scale;
            double currentLimit = limit;
            if(i < forces.length - 1) currentLimit = Math.min(currentLimit, next[i + 1]);
            next[i] = Math.min(currentLimit, Math.max(next[i - 1], next[i] + delta));
        }

        Map<Integer, Integer> nextMapping = new HashMap<Integer, Integer>();
        for(int i = 0; i < next.length; ++i) {
            nextMapping.put(i, (int)Math.round(next[i]));
        }

        // forget about some inversions
        double retainRatio = 1 - this.alpha;
        for(Map.Entry<Integer, Double> entry: this.inversionCounts.entrySet()) {
            this.inversionCounts.put(entry.getKey(), entry.getValue() * retainRatio);
            this.packetCounter = 0;
        }

        return nextMapping;
    }

    @Override
    public void inversionInQueue(int queueIndex) {
        this.inversionCounts.put(queueIndex, this.inversionCounts.getOrDefault(queueIndex, 0.0) + 1);
    }

    @Override
    public void initBounds(Map<Integer, Integer> destination, int perQueueCapacity) {
        int numQueues = destination.size();

        for(Map.Entry<Integer, Integer> entry: destination.entrySet()) {
            // initialize bounds to first n ranks.
            // algorithm should adapt to arbitrary ranks later on.
            int queueIndex = entry.getKey();
            destination.put(queueIndex, queueIndex);
        }
    }
}
