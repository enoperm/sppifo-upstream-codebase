package ch.ethz.systems.netbench.xpt.sppifo.utility;

import ch.ethz.systems.netbench.core.log.Logger;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionsTracker;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionInformation;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

public class ImmediateInversionsTracker implements InversionsTracker {
    private Logger logger;
    private Map<Integer, Map<Integer, Integer>> lastObservedFromInterfaceAndQueue;

    public ImmediateInversionsTracker(Logger logger) {
        if(logger == null) throw new NullPointerException();
        this.logger = logger;
        this.lastObservedFromInterfaceAndQueue = new HashMap<Integer, Map<Integer, Integer>>();
    }

    public Map<Integer, Integer> mapForNewInterface() {
        return new HashMap<Integer, Integer>();
    }

    @Override
    public InversionInformation process(int interfaceId, long packetCount, int[][] queues, int emittedRank, int dequeuedFrom)
    throws IOException {
        Map<Integer, Integer> lastObservedFromQueue = this.lastObservedFromInterfaceAndQueue.getOrDefault(interfaceId, null);
        // avoid instantiating superflous new object with getOrDefault when value already exists.
        if(lastObservedFromQueue == null) {
            lastObservedFromQueue = mapForNewInterface();
            this.lastObservedFromInterfaceAndQueue.put(interfaceId, lastObservedFromQueue);
        }

        int lastRank = lastObservedFromQueue.getOrDefault(dequeuedFrom, emittedRank);
        int cost = lastRank - emittedRank;

        lastObservedFromQueue.put(dequeuedFrom, emittedRank);

        if(cost > 0) {
            logger.emitRow(interfaceId, lastRank, cost, packetCount);
            return new InversionInformation(lastRank, emittedRank, dequeuedFrom, dequeuedFrom);
        }
        return null;
    }
}
