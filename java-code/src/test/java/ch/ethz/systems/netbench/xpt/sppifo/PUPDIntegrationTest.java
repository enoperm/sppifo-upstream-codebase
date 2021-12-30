package ch.ethz.systems.netbench.xpt.sppifo.ports.SPPIFO;

import ch.ethz.systems.netbench.xpt.sppifo.adaptations.PUPD;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionsTracker;
import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.xpt.tcpbase.PriorityHeader;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * This test was added during the extraction of the Adapatation Algorithm
 * responsibility from SPPIFOQueue, was written against the original PUPD logic,
 * and is meant to ensure the extracted PUPD implementation behaves the same.
 */
@RunWith(MockitoJUnitRunner.class)
public class PUPDIntegrationTest {
    @Mock
    private NetworkDevice networkDevice;

    @Mock
    private InversionsTracker inversionsTracker;

    private PriorityHeader[] testSequence = new PriorityHeader[]{
        rankedPacket(13), rankedPacket(7), rankedPacket(3)
    };

    @Test
    public void testPUPD1() throws Exception {
        SPPIFOQueue sppifo = getTestUnit("1");
        List<Map<Integer, Integer>> boundsOverTime = this.feedTestSequence(sppifo);

        List<Map<Integer, Integer>> expectedBounds = new ArrayList<Map<Integer, Integer>>();
        {
            expectedBounds.add(new HashMap<Integer, Integer>() {{
                put(0,  0);
                put(1, 13);
            }});
            expectedBounds.add(new HashMap<Integer, Integer>() {{
                put(0,  7);
                put(1, 13);
            }});
            expectedBounds.add(new HashMap<Integer, Integer>() {{
                put(0,  3);
                put(1, 12);
            }});
        }

        for(int t = 0; t < boundsOverTime.size(); ++t) {
            Map<Integer, Integer> expected = expectedBounds.get(t);
            Map<Integer, Integer> observed = boundsOverTime.get(t);
            if(!expected.equals(observed)) {
                System.err.println("t = " + t);
                assertEquals(expected, observed);
            }
        }
    }

    @Test
    public void testPUPDCost() throws Exception {
        SPPIFOQueue sppifo = getTestUnit("cost");
        List<Map<Integer, Integer>> boundsOverTime = this.feedTestSequence(sppifo);

        List<Map<Integer, Integer>> expectedBounds = new ArrayList<Map<Integer, Integer>>();
        {
            expectedBounds.add(new HashMap<Integer, Integer>() {{
                put(0,  0);
                put(1, 13);
            }});
            expectedBounds.add(new HashMap<Integer, Integer>() {{
                put(0,  7);
                put(1, 13);
            }});
            expectedBounds.add(new HashMap<Integer, Integer>() {{
                put(0,  3);
                put(1,  9);
            }});
        }

        for(int t = 0; t < boundsOverTime.size(); ++t) {
            Map<Integer, Integer> expected = expectedBounds.get(t);
            Map<Integer, Integer> observed = boundsOverTime.get(t);
            if(!expected.equals(observed)) {
                System.err.println("t = " + t);
                assertEquals(expected, observed);
            }
        }
    }

    @Test
    public void testPUPDRank() throws Exception {
        SPPIFOQueue sppifo = getTestUnit("rank");
        List<Map<Integer, Integer>> boundsOverTime = this.feedTestSequence(sppifo);

        List<Map<Integer, Integer>> expectedBounds = new ArrayList<Map<Integer, Integer>>();
        {
            expectedBounds.add(new HashMap<Integer, Integer>() {{
                put(0,  0);
                put(1, 13);
            }});
            expectedBounds.add(new HashMap<Integer, Integer>() {{
                put(0,  7);
                put(1, 13);
            }});
            expectedBounds.add(new HashMap<Integer, Integer>() {{
                put(0,  3);
                put(1, 10);
            }});
        }

        for(int t = 0; t < boundsOverTime.size(); ++t) {
            Map<Integer, Integer> expected = expectedBounds.get(t);
            Map<Integer, Integer> observed = boundsOverTime.get(t);
            if(!expected.equals(observed)) {
                System.err.println("t = " + t);
                assertEquals(expected, observed);
            }
        }
    }

    @Test
    public void testPUPDQueueBound() throws Exception {
        SPPIFOQueue sppifo = getTestUnit("queueBound");
        List<Map<Integer, Integer>> boundsOverTime = this.feedTestSequence(sppifo);

        List<Map<Integer, Integer>> expectedBounds = new ArrayList<Map<Integer, Integer>>();
        {
            expectedBounds.add(new HashMap<Integer, Integer>() {{
                put(0,  0);
                put(1, 13);
            }});
            expectedBounds.add(new HashMap<Integer, Integer>() {{
                put(0,  7);
                put(1, 13);
            }});
            expectedBounds.add(new HashMap<Integer, Integer>() {{
                put(0, 3);
                put(1, 3);
            }});
        }

        for(int t = 0; t < boundsOverTime.size(); ++t) {
            Map<Integer, Integer> expected = expectedBounds.get(t);
            Map<Integer, Integer> observed = boundsOverTime.get(t);
            if(!expected.equals(observed)) {
                System.err.println("t = " + t);
                assertEquals(expected, observed);
            }
        }
    }

    private SPPIFOQueue getTestUnit(String pushdownBehaviour) throws Exception {
        return new SPPIFOQueue(2, 10, this.networkDevice, new PUPD(pushdownBehaviour), 1, this.inversionsTracker);
    }

    private List<Map<Integer, Integer>> feedTestSequence(SPPIFOQueue q) {
        List<Map<Integer, Integer>> bounds = new ArrayList<Map<Integer, Integer>>();

        for(PriorityHeader packet: this.testSequence) {
            q.offer(packet);
            bounds.add(q.getQueueBounds());
        }

        return bounds;
    }

    private static PriorityHeader rankedPacket(long rank) {
        PriorityHeader packet = mock(PriorityHeader.class);
        when(packet.getPriority()).thenReturn(rank);
        return packet;
    }
}
