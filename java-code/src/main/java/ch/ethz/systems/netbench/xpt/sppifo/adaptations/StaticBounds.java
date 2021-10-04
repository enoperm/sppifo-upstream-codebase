package ch.ethz.systems.netbench.xpt.sppifo.adaptations;

import ch.ethz.systems.netbench.core.config.NBProperties;

import java.util.*;
import java.util.function.*;

public class StaticBounds implements AdaptationAlgorithm, BoundsInitializationAlgorithm {
    private int[] bounds;

    public StaticBounds(NBProperties settings) {
        String allBoundsStr = settings.getPropertyOrFail("sppifo_queue_bounds");
        String[] boundsStr = allBoundsStr.split(",");
        this.bounds = new int[boundsStr.length];
        for(int i = 0; i < this.bounds.length; ++i) {
            this.bounds[i] = Integer.parseInt(boundsStr[i]);
        }
    }

    @Override
    public Map<Integer, Integer> nextBounds(Map<Integer, Integer> currentBounds, int destinationIndex, int rank) {
        Map<Integer, Integer> nextMapping = new HashMap<Integer, Integer>();
        this.initBounds(nextMapping, -1);

        return nextMapping;
    }

    @Override
    public void initBounds(Map<Integer, Integer> destination, int perQueueCapacity) {
        for(int i = 0; i < this.bounds.length; ++i) {
            destination.put(i, this.bounds[i]);
        }
    }
}
