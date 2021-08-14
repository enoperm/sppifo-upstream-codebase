package ch.ethz.systems.netbench.xpt.sppifo.adaptations;

import java.util.*;
import java.util.function.*;

public class PUPD implements AdaptationAlgorithm {
    private class CostFunctionInput {
        public int bound;
        public int cost;
        public int previousBound;
        public int rank;
    }

    private Function<CostFunctionInput, Integer> costFunction;

    public PUPD(String costFn) throws Exception {
        switch(costFn) {
        case "cost":
            this.costFunction = (CostFunctionInput in) -> {
                return in.cost;
            };
            break;

        case "1":
            this.costFunction = (CostFunctionInput in) -> {
                return 1;
            };
            break;

        case "rank":
            this.costFunction = (CostFunctionInput in) -> {
                return in.rank;
            };
            break;
 
        case "queueBound":
            this.costFunction = (CostFunctionInput in) -> {
                return in.bound - in.previousBound;
            };
            break;

        default:
            throw new Exception("unimplemented cost function: " + costFn);
        }
    }

    @Override
    public Map<Integer, Integer> nextBounds(Map<Integer, Integer> currentBounds, int destinationIndex, int rank) {
        Map<Integer, Integer> next = new HashMap<Integer, Integer>();

        // Deep copy the map, because java generics only handle reference types,
        // but we do not want any accidental reference semantics.
        // TODO: This probably should be an array of ints instead,
        // but don't want to stray far from the original SPPIFOQueue design yet.
        for(Map.Entry<Integer, Integer> entry: currentBounds.entrySet()) {
            int idx = entry.getKey();
            int bound = entry.getValue();

            next.put(idx, bound);
        }

        // Push up, as well as push down in lowest ranked queue.
        next.put(destinationIndex, rank);

        int cost = currentBounds.get(destinationIndex) - rank;
        if (cost > 0) {
            // Push down higher ranked queues
            CostFunctionInput in = new CostFunctionInput();
            in.cost = cost;
            in.rank = rank;
            for (int w = currentBounds.size() - 1; w > destinationIndex; --w){
                in.bound = next.get(w);
                in.previousBound = next.get(w - 1);
                next.put(w, in.bound - this.costFunction.apply(in));
            }
        }

        return next;
    }
}
