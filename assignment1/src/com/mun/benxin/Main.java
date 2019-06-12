package com.mun.benxin;

public class Main {

    public static void main(String[] args) {

        try {
            if (args.length == 0) throw new NumberFormatException();
            int simulationSeconds = Integer.parseInt(args[0]);
            if (args.length >=2){
                switch (args[1]){
                    case "sim1" : runDriveThroughSimulation(simulationSeconds); break;
                    case "sim2" : runGrocerySimulation(simulationSeconds); break;
                    default: System.out.println(args[1] + "does not indicate any simulation.. exiting");
                }
            }
            else {
                System.out.println("Running DriveThrough simulation first, then Grocery");
                Thread.sleep(1000);
                runDriveThroughSimulation(simulationSeconds);
                runGrocerySimulation(simulationSeconds);
            }
        }
        catch (InterruptedException | NumberFormatException ex){
            System.out.println("Wrong Inputs");
            System.out.println("usage: the program accepts one or two parameters");
            System.out.println("First parameter is simulation length in seconds (integer) (mandatory)");
            System.out.println("Second parameter indicates the simulation you wish to run\n" +
                    "sim1 for DriveTrough, sim2 for Grocery \n" +
                    "if no second parameter provided, both simulation will be run and print results...");
            System.out.println("program exiting");
        }
    }

    private static void runGrocerySimulation(int simulationSeconds) {
        new GroceryStore.Builder("Sobeys")
                .avalibleLines(3)
                .lineCapacity(2)
                .maxServeTime(600)
                .minServeTime(300)
                .simulationSeconds(simulationSeconds)
                .build()
                .openBusiness();
    }

    private static void runDriveThroughSimulation(int simulationSeconds) {
        new DriveThrough.Builder("Pop eye")
                .staffCount(3)
                .minFulfillmentTime(300)
                .maxFulfillmentTime(600)
                .lineCapacity(3)
                .simulationSeconds(simulationSeconds)
                .build()
                .startBusiness();
    }
}
