package ch.ethz.systems.netbench.xpt.sppifo.utility;

import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionInformation;

import java.io.IOException;

public interface InversionsTracker {
    public InversionInformation process(int interfaceId, long packetCount, int[][] queues, int emittedRank, int dequeuedFrom)
        throws IOException;
}
