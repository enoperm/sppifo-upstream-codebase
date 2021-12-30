package ch.ethz.systems.netbench.xpt.sppifo.utility;

public class InversionInformation {
    public final int higher;
    public final int lower;

    public final int higherQueue;
    public final int lowerQueue;

    public InversionInformation(int h, int l, int hq, int lq) {
        this.higher = h;
        this.lower = l;

        this.higherQueue = hq;
        this.lowerQueue = lq;
    }
}
