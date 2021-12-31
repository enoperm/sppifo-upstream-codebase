package ch.ethz.systems.netbench.xpt.sppifo.utility;

import ch.ethz.systems.netbench.core.log.Logger;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionsTracker;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionInformation;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

public class ImmediateInversionsTracker implements InversionsTracker {
    private Logger logger;

    public ImmediateInversionsTracker(Logger logger) {
        if(logger == null) throw new NullPointerException();
        this.logger = logger;
    }

    @Override
    public InversionInformation process(int interfaceId, long packetCount, int[][] queues, int emittedRank, int dequeuedFrom)
    throws IOException {
        int queueLength = queues[dequeuedFrom].length;
        int nextRank = queueLength > 0
                     ? queues[dequeuedFrom][queueLength-1]
                     : emittedRank;

        int cost = emittedRank - nextRank;

        if(cost > 0) {
            logger.emitRow(interfaceId, emittedRank, cost, packetCount);
            return new InversionInformation(emittedRank, nextRank, dequeuedFrom, dequeuedFrom);
        }
        return null;
    }
}
