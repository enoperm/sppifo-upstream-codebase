package ch.ethz.systems.netbench.xpt.sppifo.adaptations;

import java.util.Map;

public interface AdaptationAlgorithm  {
    Map<Integer, Integer> nextBounds(Map<Integer, Integer> currentBounds, int destinationIndex, int rank);
}
