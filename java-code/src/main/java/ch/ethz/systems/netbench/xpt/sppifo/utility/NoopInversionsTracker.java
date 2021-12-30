package ch.ethz.systems.netbench.xpt.sppifo.utility;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionsTracker;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionInformation;

public class NoopInversionsTracker implements InversionsTracker {
    public NoopInversionsTracker () {}

    @Override
    public InversionInformation process(int interfaceId, long packetCount, int[][] queues, int emittedRank, int dequeuedFrom) {
        return null;
    }
}
