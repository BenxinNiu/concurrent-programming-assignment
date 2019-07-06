package com.mun.benxin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        Intersection intersection = new Intersection(new LightsControl());

        // Simulation one with no human error
        List<Lane> location = Arrays.asList(
                Lane.LANE_A, Lane.LANE_B, Lane.LANE_D, Lane.LANE_B,
                Lane.LANE_A, Lane.LANE_C, Lane.LANE_A);
        runSimulation(intersection, location, false);

        // Simulation two with human error
        location = Arrays.asList(
                Lane.LANE_A, Lane.LANE_C, Lane.LANE_B, Lane.LANE_B,
                Lane.LANE_B, Lane.LANE_C, Lane.LANE_A,
                Lane.LANE_D, Lane.LANE_C, Lane.LANE_B);
        runSimulation(intersection, location, true);

    }

    public static void runSimulation(Intersection intersection, List<Lane> vehicleLocation, boolean humanError){
        System.out.println("\nSimulation starts with humanError " + humanError + "\n");
        try {Thread.sleep(3000);} catch (InterruptedException ex) {}
        List<Vehicle> vehiclePool = new ArrayList<>();
        int carId = 1;
        for (Lane lane: vehicleLocation) {
            vehiclePool.add(new Vehicle("Car"+carId++, lane, intersection, humanError));
        }
        intersection.setVehiclePool(vehiclePool).liveTraffic();
        System.out.println("\nSimulation finished with humanError " + humanError + "\n\n");
    }
}
