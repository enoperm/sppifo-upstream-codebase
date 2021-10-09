package ch.ethz.systems.netbench.xpt.sppifo.adaptations;

import ch.ethz.systems.netbench.core.config.NBProperties;

import java.util.*;
import java.util.function.*;

public class SpringAdaptationAlgorithm implements AdaptationAlgorithm, BoundsInitializationAlgorithm {
    private enum SamplingMode {
        COUNT_INVERSIONS, COUNT_PACKETS
    }

    private double[] bounds;
    private int perceivedMaxRank;
    private Map<Integer, Double> inversionCounts;
    private Map<Integer, Integer> lastRankByQueue;
    private Map<Integer, Double> packetCounts;

    private SamplingMode samplingMode;
    private double alpha;
    private double sensitivity;

    public SpringAdaptationAlgorithm(NBProperties settings) {
        this.alpha = settings.getDoublePropertyOrFail("spring_alpha");
        this.sensitivity = settings.getDoublePropertyOrFail("spring_sensitivity");
        this.samplingMode = SamplingMode.valueOf(settings.getPropertyOrFail("spring_sample_mode"));
        this.inversionCounts = new HashMap<Integer, Double>();
        this.packetCounts = new HashMap<Integer, Double>();
        this.lastRankByQueue = new HashMap<Integer, Integer>();
        this.perceivedMaxRank = Integer.MIN_VALUE;
    }

    private Map<Integer, Double> getBozonCounts() {
        switch(this.samplingMode) {
        case COUNT_INVERSIONS:
            return this.inversionCounts;

        case COUNT_PACKETS:
            return this.packetCounts;

        default:
            throw new RuntimeException("unreachable");
        }
    }

    @Override
    public Map<Integer, Integer> nextBounds(Map<Integer, Integer> currentBounds, int destinationIndex, int rank) {
        if(this.perceivedMaxRank < rank) this.perceivedMaxRank = rank;
        this.packetCounts.put(destinationIndex, this.packetCounts.getOrDefault(destinationIndex, 0.0) * (1 - alpha) + alpha);

        boolean isInversion = this.lastRankByQueue.getOrDefault(destinationIndex, Integer.MIN_VALUE) > rank;
        this.inversionCounts.put(
            destinationIndex,
            this.inversionCounts.getOrDefault(destinationIndex, 0.0) * (1 - this.alpha)
                + (isInversion ? this.alpha : 0)
        );

        double[] forces = new double[this.bounds.length];
        Map<Integer, Double> bozonCounts = this.getBozonCounts();
        for(int i = 0; i < forces.length; ++i) {
            forces[i] = bozonCounts.getOrDefault(i, 0.0);
        }

        // in this version, bounds do not push around each other.
        for(int i = forces.length - 1; i > 0; --i) {
            double delta = this.sensitivity * (forces[i] - forces[i - 1]);
            // upper bound starts from perceived max rank
            // or number of queues - 1 (ie. initial value for top bound),
            // whichever is higher.
            double max = Math.max(this.perceivedMaxRank, this.bounds.length - 1);
            // if there is a queue above, make sure we do not cross or overlap it.
            if(i < forces.length - 1) max = Math.min(max, this.bounds[i + 1] - 1);

            this.bounds[i] = Math.min(max, this.bounds[i] + delta);
            this.bounds[i] = Math.max(this.bounds[i - 1] + 1, this.bounds[i]);
        }

        Map<Integer, Integer> nextMapping = new HashMap<Integer, Integer>();
        for(int i = 0; i < bounds.length; ++i) {
            nextMapping.put(i, (int)Math.ceil(this.bounds[i]));
        }

        return nextMapping;
    }

    @Override
    public void initBounds(Map<Integer, Integer> destination, int perQueueCapacity) {
        int numQueues = destination.size();
        this.bounds = new double[numQueues];

        for(int i = 0; i < numQueues; ++i) {
            // initialize bounds to first n ranks.
            // algorithm should adapt to arbitrary ranks later on.
            destination.put(new Integer(i), new Integer(i));
            this.bounds[i] = (double)i;
        }
    }
}
