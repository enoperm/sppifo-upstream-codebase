package ch.ethz.systems.netbench.xpt.sppifo.adaptations;

import java.util.Map;

public interface BoundsInitializationAlgorithm {
    void initBounds(Map<Integer, Integer> destination, int perQueueCapacity);
}
