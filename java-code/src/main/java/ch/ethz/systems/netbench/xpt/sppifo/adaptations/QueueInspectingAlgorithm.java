package ch.ethz.systems.netbench.xpt.sppifo.adaptations;

import ch.ethz.systems.netbench.xpt.sppifo.ports.SPPIFO.SPPIFOQueue;

import java.util.Map;

public interface QueueInspectingAlgorithm  {
    void setQueueImplementation(SPPIFOQueue impl);
}
