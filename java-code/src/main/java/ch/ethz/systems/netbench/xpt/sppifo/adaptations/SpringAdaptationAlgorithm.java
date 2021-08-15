package ch.ethz.systems.netbench.xpt.sppifo.adaptations;

import java.util.*;
import java.util.function.*;

public class SpringAdaptationAlgorithm implements AdaptationAlgorithm, InversionTracker {
    private class CostFunctionInput {
        public int bound;
        public int cost;
        public int previousBound;
        public int rank;
    }

    private Map<Integer, Long> inversionCounts;
    private double[] previousDelta;
    private long samplingInterval;
    private long packetCounter;
    private double alpha;
    private double sensitivity;

    public SpringAdaptationAlgorithm() {
        // TODO: take configuration from caller..
        this.alpha = 0.0;
        this.sensitivity = 1.0;
        this.samplingInterval = 1000; 
        this.rotateStats(null);
    }

    private void rotateStats(double[] delta) {
        this.inversionCounts = new HashMap<Integer, Long>();
        this.packetCounter = 0;
        this.previousDelta = delta;
    }

    private double scalingFactor(int numQueues) {
        // TODO: for now, I hardcoded maxrank at 100
        // because that seems to be the behaviour of the packet generator,
        // but this does not seem to have been explicitly configured in the measurement config.
        return
            (this.sensitivity * 100) /
            (this.samplingInterval * (double)numQueues);
    }

    @Override
    public Map<Integer, Integer> nextBounds(Map<Integer, Integer> currentBounds, int destinationIndex, int rank) {
        ++this.packetCounter;
        if(this.packetCounter < samplingInterval) return currentBounds;

        int[] next = new int[currentBounds.size()];

        for(int i = 0; i < next.length; ++i) {
            next[i] = currentBounds.get(i);
        }

        // allocating space for an additional force,
        // to avoid having to special-case the first bound that should not move.
        double[] delta = new double[next.length + 2];
        for(int i = 0; i < delta.length - 2; ++i) {
            delta[i + 1] = this.inversionCounts.getOrDefault(i, 0L) * this.alpha;
        }

        if(this.previousDelta != null) {
            for(int i = 0; i < delta.length - 2; ++i) {
                delta[i + 1] += this.inversionCounts.getOrDefault(i, 0L) * (1 - this.alpha);
            }
            // duplicate edges to represent the borders of the rank space
            delta[0] = delta[1];
        }

        double scale = this.scalingFactor(next.length);
        double[] forces = new double[delta.length - 1];

        for(int i = 0; i < forces.length; ++i) {
            forces[i] = (delta[i + 1] - delta[i]) * scale;
        }

        for(int i = 0; i < next.length; ++i) {
            next[i] = (int)Math.round(next[i] + forces[i]);
        }

        // put bounds in order and convert to return type.
        // TODO: change return type.
        Arrays.sort(next);
        Map<Integer, Integer> nextMapping = new HashMap<Integer, Integer>();
        for(int i = 0; i < next.length; ++i) {
            nextMapping.put(i, next[i]);
        }

        rotateStats(delta);

        return nextMapping;
    }

    @Override
    public void inversionInQueue(int queueIndex) {
        this.inversionCounts.put(queueIndex, this.inversionCounts.getOrDefault(queueIndex, 0L) + 1);
    }
}