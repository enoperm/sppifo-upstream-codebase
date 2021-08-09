package ch.ethz.systems.netbench.core.run;

public class MainSimulation {

    public static void main(String args[]) {
        if (args.length == 0) {
            System.err.println("Please specify which experiments to run!");
        }

        MainFromProperties.main(args);
    }

}
