package ch.ethz.systems.netbench.xpt.sppifo.adaptations;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.xpt.sppifo.ports.SPPIFO.SPPIFOQueue;

import java.util.*;
import java.util.function.*;

public class SpringQueueFillAdaptationAlgorithm
implements
    AdaptationAlgorithm,
    BoundsInitializationAlgorithm,
    QueueInspectingAlgorithm
{
    private double[] bounds;
    private int perceivedMaxRank;
    private double sensitivity;

    private SPPIFOQueue queueImplementation;

    public SpringQueueFillAdaptationAlgorithm(NBProperties settings) {
        this.sensitivity = settings.getDoublePropertyOrFail("spring_sensitivity");
        this.perceivedMaxRank = Integer.MIN_VALUE;
        this.queueImplementation = queueImplementation;
    }

    private double[] getQueueCounts() {
        double[] counts = new double[bounds.length];
        for(int i = 0; i < bounds.length; ++i) {
            // should never throw in this code path.
            counts[i] = (double)(this.queueImplementation.getQueueCount(i).getAsInt());
        }
        return counts;
    }

    public void setQueueImplementation(SPPIFOQueue impl) {
        this.queueImplementation = impl;
    }

    @Override
    public Map<Integer, Integer> nextBounds(Map<Integer, Integer> currentBounds, int destinationIndex, int rank) {
        if(this.perceivedMaxRank < rank) this.perceivedMaxRank = rank;

        double[] forces = this.getQueueCounts();

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
            destination.put(i, i);
            this.bounds[i] = (double)i;
        }
    }
}
