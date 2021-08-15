package ch.ethz.systems.netbench.xpt.sppifo.ports.SPPIFO;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;

public class SPPIFOOutputPortGenerator extends OutputPortGenerator {

    private final long numberQueues;
    private final long sizePerQueuePackets;
    private final String stepSize;

    // TODO: these should take an NBConfiguration instead,
    // so as to make configuration more flexible
    // as well as to make InfrastructureSelector more readable.
    public SPPIFOOutputPortGenerator(long numberQueues, long sizePerQueuePackets, String stepSize) throws Exception {
        this.numberQueues = numberQueues;
        this.sizePerQueuePackets = sizePerQueuePackets;
        this.stepSize = stepSize;
        SimulationLogger.logInfo("Port", "SPPIFO(numberQueues=" + numberQueues + ", sizePerQueuePackets=" + sizePerQueuePackets +
                ", stepSize=" + stepSize + ")");
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) throws Exception {
        return new SPPIFOOutputPort(ownNetworkDevice, towardsNetworkDevice, link, numberQueues, sizePerQueuePackets, stepSize);
    }

}
