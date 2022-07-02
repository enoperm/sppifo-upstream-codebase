package ch.ethz.systems.netbench.xpt.sppifo.utility;

import ch.ethz.systems.netbench.xpt.sppifo.utility.*;
import ch.ethz.systems.netbench.core.log.SimulationLogger;

public class InversionsTrackerFactory {
    private InversionsTrackerFactory() {}

    public static InversionsTracker newInversionsTracker(String inversionModel) throws Exception {
        if(!SimulationLogger.hasInversionsTrackingEnabled()) {
            return new NoopInversionsTracker();
        }

        switch(inversionModel) {
        case "INVERSIONS_ALL":
            return new AllInversionsTracker(SimulationLogger.getGlobalInversionsLogger());
        case "INVERSIONS_QUEUE_IMMEDIATE":
            return new ImmediateInversionsTracker(SimulationLogger.getGlobalInversionsLogger());

        default:
            throw new Exception("Unsupported inversion model: " + inversionModel);
        }
    }
}
