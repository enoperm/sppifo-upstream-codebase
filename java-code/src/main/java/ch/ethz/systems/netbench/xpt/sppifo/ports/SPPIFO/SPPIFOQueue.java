package ch.ethz.systems.netbench.xpt.sppifo.ports.SPPIFO;

import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.Packet;
// TODO: do not need more than PrioritHeader, do we?
// FIXME: since PriorityHeader extends a TCP interface, we can't test with alternate L4 protocols such as SCTP or UDP.
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;
import ch.ethz.systems.netbench.xpt.tcpbase.PriorityHeader;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.xpt.sppifo.adaptations.AdaptationAlgorithm;
import ch.ethz.systems.netbench.xpt.sppifo.adaptations.BoundsInitializationAlgorithm;
import ch.ethz.systems.netbench.xpt.sppifo.adaptations.InversionTracker;
import ch.ethz.systems.netbench.xpt.sppifo.adaptations.QueueInspectingAlgorithm;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionsTracker;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionInformation;

import java.util.*;
import java.util.OptionalInt;
import java.util.stream.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

// General SPPIFO implementation to be used, for instance, when the ranks are specified from the end-host.
public class SPPIFOQueue implements Queue {

    private final ArrayList<ArrayBlockingQueue<Object>> queueList;
    private final Map<Integer, Integer> queueBounds;
    private final InversionsTracker inversionsTracker;
    private ReentrantLock reentrantLock;
    private int ownId;
    private AdaptationAlgorithm adaptationAlgorithm;
    private long packetCount;
    private long queueboundTrackingInterval;

    public SPPIFOQueue(long numQueues, long perQueueCapacity, NetworkDevice ownNetworkDevice, AdaptationAlgorithm adaptationAlgorithm, long queueboundTrackingInterval, InversionsTracker inversionsTracker) throws Exception {
        this.queueList = new ArrayList((int)numQueues);
        this.reentrantLock = new ReentrantLock();
        this.queueBounds = new HashMap<Integer, Integer>();
        this.adaptationAlgorithm = adaptationAlgorithm;
        this.packetCount = 0;
        this.queueboundTrackingInterval = queueboundTrackingInterval;
        this.inversionsTracker = inversionsTracker;

        ArrayBlockingQueue<Object> fifo;
        for (int i = 0; i < numQueues; i++){
            fifo = new ArrayBlockingQueue<Object>((int)perQueueCapacity);
            queueList.add(fifo);
            queueBounds.put(i, 0);
        }
        this.ownId = ownNetworkDevice.getIdentifier();

        if(this.adaptationAlgorithm instanceof BoundsInitializationAlgorithm) {
            BoundsInitializationAlgorithm bia = (BoundsInitializationAlgorithm)this.adaptationAlgorithm;
            // TODO: this class takes longs, but the queue bounds are ints,
            // so there are configurations we could be asked for but not represent.
            // Either make these ints or represent bounds with longs as well.
            // we pass queueBounds along because it is final and we cannot reassign.
            // number of queues is same as the initial size of this map.
            bia.initBounds(this.queueBounds, (int)perQueueCapacity);
            if(SimulationLogger.hasQueueBoundTrackingEnabled()) {
                for (int c = queueList.size()-1; c >= 0; --c) {
                    SimulationLogger.logQueueBound(this.ownId, c, this.queueBounds.get(c));
                }
            }
        }

        if(this.adaptationAlgorithm instanceof QueueInspectingAlgorithm) {
            QueueInspectingAlgorithm qia = (QueueInspectingAlgorithm)this.adaptationAlgorithm;
            qia.setQueueImplementation(this);
        }
    }

    // Packet dropped and null returned if selected queue exceeds its size
    @Override
    public boolean offer(Object o) {
        // Extract rank from header
        PriorityHeader header = (PriorityHeader) o;
        int rank = (int)header.getPriority();

        this.reentrantLock.lock();
        try {
            // Mapping based on queue bounds
            int currentQueueBound;
            for (int q=queueList.size()-1; q>=0; q--){
                currentQueueBound = (int)queueBounds.get(q);
                if ((currentQueueBound <= rank) || q==0) {
                    boolean result = queueList.get(q).offer(o);
                    if (!result) return false;

                    Map<Integer, Integer> newBounds = this.adaptationAlgorithm.nextBounds(this.queueBounds, q, rank);

                    for(Map.Entry<Integer, Integer> entry: newBounds.entrySet()) {
                        int idx = entry.getKey();
                        int bound = entry.getValue();

                        this.queueBounds.put(idx, bound);
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.reentrantLock.unlock();
        }
        return false;
    }

    @Override
    public int size() {
        int size = 0;
        for (int q=0; q<queueList.size(); q++){
            size += queueList.get(q).size();
        }
        return size;
    }

    public OptionalInt getQueueCount(int index) {
        try {
            this.reentrantLock.lock();

            if(this.queueList.size() >= index) {
                return OptionalInt.empty();
            }

            Queue<Object> queue = this.queueList.get(index);
            return OptionalInt.of(queue.size());
        } finally {
            this.reentrantLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        boolean empty = true;
        for (int q=0; q<queueList.size(); q++){
            if(!queueList.get(q).isEmpty()){
                empty = false;
            }
        }
        return empty;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public Object[] toArray(Object[] objects) {
        return new Object[0];
    }

    @Override
    public boolean add(Object o) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean addAll(Collection collection) {
        return false;
    }

    @Override
    public void clear() { }

    @Override
    public boolean retainAll(Collection collection) {
        return false;
    }

    @Override
    public boolean removeAll(Collection collection) {
        return false;
    }

    @Override
    public boolean containsAll(Collection collection) {
        return false;
    }

    @Override
    public Object remove() {
        return null;
    }

    @Override
    public Object poll() {
        this.reentrantLock.lock();
        try {
            PriorityHeader p;
            for (int q=0; q<queueList.size(); q++){
                p = (PriorityHeader) queueList.get(q).poll();
                if (p != null) {
                    PriorityHeader header = p;
                    int rank = (int)header.getPriority();
                    // System.err.println("SPPIFO: Dequeued packet with rank" + rank + ", from queue " + q + ". Queue size: " + queueList.get(q).size());

                    // Log rank of packet enqueued and queue selected if enabled
                    if(SimulationLogger.hasRankMappingEnabled()){
                        SimulationLogger.logRankMapping(this.ownId, rank, q);
                    }

                    boolean shouldLogQueueBound = (packetCount % this.queueboundTrackingInterval) == 0;
                    if(shouldLogQueueBound && SimulationLogger.hasQueueBoundTrackingEnabled()) {
                        for (int c=queueList.size()-1; c>=0; c--){
                            SimulationLogger.logQueueBound(this.ownId, c, this.queueBounds.get(c));
                        }
                    }

                    int[][] queuedRanks = new int[queueList.size()][];
                    for(int i = 0; i < queueList.size(); ++i) {
                        Object[] waiting = queueList.get(i).toArray();
                        queuedRanks[i] = new int[waiting.length];
                        for(int j = 0; j < waiting.length; ++j) {
                            queuedRanks[i][j] = (int)((PriorityHeader)waiting[j]).getPriority();
                        }
                    }

                    // Check whether there is an inversion: a packet with smaller rank in queue than the one polled
                    InversionInformation ii = this.inversionsTracker.process(this.ownId, this.packetCount, queuedRanks, rank, q);
                    if(ii != null) {
                        // TODO: rename InversionTracker to avoid confusion,
                        // this interface is for adaptation algorithms to optionally implement,
                        // if they use inversions as an input.
                        if(this.adaptationAlgorithm instanceof InversionTracker) {
                            InversionTracker t = (InversionTracker)this.adaptationAlgorithm;
                            t.inversionInQueue(ii.lowerQueue, ii.higher - ii.lower);
                        }
                    }

                    return p;
                }
            }
            return null;
        } catch (Exception e){
            e.printStackTrace(System.err);
            return null;
        } finally {
            this.packetCount += 1;
            this.reentrantLock.unlock();
        }
    }

    @Override
    public Object element() {
        return null;
    }

    @Override
    public Object peek() {
        return null;
    }

    public Map<Integer, Integer> getQueueBounds() {
        Map<Integer, Integer> copy = new HashMap<Integer, Integer>();
        for(Map.Entry<Integer, Integer> entry: this.queueBounds.entrySet()) {
            int idx = entry.getKey();
            int bound = entry.getValue();

            copy.put(idx, bound);
        }

        return copy;
    }
}
