package ch.ethz.systems.netbench.xpt.sppifo.ports.SPPIFO;

import ch.ethz.systems.netbench.xpt.sppifo.adaptations.AdaptationAlgorithm;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionsTracker;

import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.network.Packet;


public class SPPIFOOutputPort extends OutputPort {

    public SPPIFOOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link, long numberQueues, long sizePerQueuePackets, AdaptationAlgorithm adaptationAlgorithm, long queueboundTrackingInterval, InversionsTracker inversionsTracker) throws Exception {
        super(ownNetworkDevice, targetNetworkDevice, link, new SPPIFOQueue(numberQueues, sizePerQueuePackets, ownNetworkDevice, adaptationAlgorithm, queueboundTrackingInterval, inversionsTracker));
    }

    /**
     * Enqueue the given packet.
     *
     * @param packet    Packet instance
     */
    @Override
    public void enqueue(Packet packet) {

        // Enqueue packet
        potentialEnqueue(packet);
    }
}
