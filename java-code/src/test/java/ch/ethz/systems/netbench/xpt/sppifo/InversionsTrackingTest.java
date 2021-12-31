package ch.ethz.systems.netbench.xpt.sppifo.ports.SPPIFO;

import ch.ethz.systems.netbench.xpt.sppifo.utility.AllInversionsTracker;
import ch.ethz.systems.netbench.xpt.sppifo.utility.ImmediateInversionsTracker;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionsTracker;
import ch.ethz.systems.netbench.xpt.sppifo.utility.InversionInformation;

import ch.ethz.systems.netbench.core.log.Logger;
import ch.ethz.systems.netbench.core.log.WriterLogger;

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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InversionsTrackingTest {
    private ByteArrayOutputStream getNewSink() {
        return new ByteArrayOutputStream();
    }

    private OutputStreamWriter writerOf(OutputStream output) {
        return new OutputStreamWriter(output);
    }

    private InversionsTracker getImmediateTracker(Writer sink) {
        return new ImmediateInversionsTracker(new WriterLogger(sink));
    }

    private InversionsTracker getAllInversionsTracker(Writer sink) {
        return new AllInversionsTracker(new WriterLogger(sink));
    }

    @Test
    public void allInversionsTrackerCountsWithinQueues() throws Exception {
        ByteArrayOutputStream sink = getNewSink();
        OutputStreamWriter writer = writerOf(sink); 

        InversionsTracker it = getAllInversionsTracker(writer);
        InversionInformation ii = null;

        ii = it.process(1, 0, new int[][]{
            new int[]{0},
            new int[]{}
        }, 1, 0);

        assertNotNull(ii);
        assertEquals(1, ii.higher);
        assertEquals(0, ii.lower);

        writer.flush();

        assertEquals(8, sink.size());
        assertEquals(
            "1,1,1,0\n",

            sink.toString()
        );
    }

    @Test
    public void allInversionsTrackerCountsAcrossQueues() throws Exception {
        ByteArrayOutputStream sink = getNewSink();
        OutputStreamWriter writer = writerOf(sink); 

        InversionsTracker it = getAllInversionsTracker(writer);
        InversionInformation ii = null;

        ii = it.process(1, 0, new int[][]{
            new int[]{0},
            new int[]{}
        }, 1, 0);

        assertNotNull(ii);
        assertEquals(1, ii.higher);
        assertEquals(0, ii.lower);

        writer.flush();

        assertEquals(8, sink.size());
        assertEquals(
            "1,1,1,0\n",

            sink.toString()
        );
    }

    @Test
    public void immediateInversionsTrackerDoesNotCountAcrossQueues() throws Exception {
        ByteArrayOutputStream sink = getNewSink();
        OutputStreamWriter writer = writerOf(sink); 

        InversionsTracker it = getImmediateTracker(writer);
        InversionInformation ii;

        ii = it.process(1, 0, new int[][]{
            new int[]{},
            new int[]{0}
        }, 1, 0);
        assertEquals(null, ii);

        writer.flush();

        assertEquals(0, sink.size());
    }

    @Test
    public void immediateInversionsTrackerCountsWithinQueues() throws Exception {
        ByteArrayOutputStream sink = getNewSink();
        OutputStreamWriter writer = writerOf(sink); 

        InversionsTracker it = getImmediateTracker(writer);
        InversionInformation ii = null;

        ii = it.process(1, 0, new int[][]{
            new int[]{0},
            new int[]{}
        }, 1, 0);

        writer.flush();

        assertNotNull(ii);
        assertEquals(1, ii.higher);
        assertEquals(0, ii.lower);
        assertEquals(8, sink.size());

        ii = it.process(1, 1, new int[][]{
            new int[]{1},
            new int[]{}
        }, 0, 0);
        assertEquals(null, ii);

        ii = it.process(1, 3, new int[][]{
            new int[]{1},
            new int[]{}
        }, 5, 0);
        assertNotNull(ii);
        assertEquals(5, ii.higher);
        assertEquals(1, ii.lower);

        writer.flush();

        assertEquals(16, sink.size());
        assertEquals(
            "1,1,1,0\n" +
            "1,5,4,3\n",

            sink.toString()
        );
    }

    //private static PriorityHeader rankedPacket(long rank) {
    //    PriorityHeader packet = mock(PriorityHeader.class);
    //    when(packet.getPriority()).thenReturn(rank);
    //    return packet;
    //}
}
