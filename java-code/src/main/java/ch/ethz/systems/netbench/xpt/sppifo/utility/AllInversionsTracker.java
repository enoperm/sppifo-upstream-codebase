package ch.ethz.systems.netbench.xpt.sppifo.utility;

import ch.ethz.systems.netbench.core.log.Logger;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionsTracker;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionInformation;

import java.io.IOException;

public class AllInversionsTracker implements InversionsTracker {
    private Logger logger;
    public AllInversionsTracker(Logger logger) {
        if(logger == null) throw new NullPointerException();
        this.logger = logger;
    }

    @Override
    public InversionInformation process(int interfaceId, long packetCount, int[][] queues, int emittedRank, int dequeuedFrom)
    throws IOException {
        int minRank = emittedRank;
        int q = 0;
        for(; q < queues.length; ++q) {
            for(int p = 0; p < queues[q].length; ++p) {
                int r = queues[q][p];
                minRank = r < minRank ? r : minRank;
            }
        }
        int cost = emittedRank - minRank;

        if(cost > 0) {
            this.logger.emitRow(interfaceId, emittedRank, cost, packetCount);
            return new InversionInformation(emittedRank, minRank, dequeuedFrom, q);
        }
        return null;
    }
}
