package ch.ethz.systems.netbench.xpt.sppifo.ports.SPPIFO;

import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.xpt.sppifo.adaptations.*;
import ch.ethz.systems.netbench.xpt.sppifo.utility.*;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;

public class SPPIFOOutputPortGenerator extends OutputPortGenerator {

    private final long numberQueues;
    private final long sizePerQueuePackets;
    private final long queueboundTrackingInterval;
    private final String stepSize;
    private final String inversionModel;
    private final NBProperties settings;

    // TODO: these should take an NBConfiguration instead,
    // so as to make configuration more flexible
    // as well as to make InfrastructureSelector more readable.
    public SPPIFOOutputPortGenerator(NBProperties settings) throws Exception {
        this.settings = settings;
        this.numberQueues = settings.getLongPropertyOrFail("output_port_number_queues");
        this.sizePerQueuePackets = settings.getLongPropertyOrFail("output_port_max_size_per_queue_packets");
        this.stepSize = settings.getPropertyOrFail("output_port_step_size");
        this.queueboundTrackingInterval = settings.getLongPropertyWithDefault("sppifo_queuebound_log_interval", 1);

        this.inversionModel = settings.getPropertyOrFail("sppifo_inversion_model");
        if(SimulationLogger.hasInversionsTrackingEnabled()) {
            this.validateInversionModel();
        }

        SimulationLogger.logInfo(
            "Port",
            "SPPIFO(numberQueues=" + numberQueues +
            ", sizePerQueuePackets=" + sizePerQueuePackets +
            ", stepSize=" + stepSize + ")"
        );
    }

    private void validateInversionModel() throws Exception {
        switch(this.inversionModel) {
        case "INVERSIONS_ALL":
        case "INVERSIONS_QUEUE_IMMEDIATE":
            break;

        default:
            throw new Exception("Unsupported inversion model: " + this.inversionModel);
        }
    }

    @Override
    public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) throws Exception {

        AdaptationAlgorithm adaptationAlgorithm = null;
        switch(stepSize) {
        // PUPD:
        case "1":
        case "cost":
        case "rank":
        case "queueBound":
            adaptationAlgorithm = new PUPD(stepSize);
            break;

        // Spring heuristic:
        case "spring":
            adaptationAlgorithm = new SpringAdaptationAlgorithm(this.settings);
            break;

        // Spring heuristic utilizing fill
        case "spring-queue-fill":
            adaptationAlgorithm = new SpringQueueFillAdaptationAlgorithm(this.settings);
            break;

        // Static queue bounds:
        case "static":
            adaptationAlgorithm = new StaticBounds(this.settings);
            break;

        default:
            throw new Exception("ERROR: SP-PIFO step size " + stepSize + " is not supported.");
        }

        return new SPPIFOOutputPort(
            ownNetworkDevice, towardsNetworkDevice,
            link, numberQueues, sizePerQueuePackets,
            adaptationAlgorithm, queueboundTrackingInterval,
            InversionsTrackerFactory.newInversionsTracker(this.inversionModel)
        );
    }

}
