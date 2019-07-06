package com.mun.benxin;


import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public enum Lane {
    LANE_A("lane A"),
    LANE_C("lane C"),
    LANE_B("lane B"),
    LANE_D("lane D");

    private String lane;

    Lane(String lane) {
        lane = lane;
    }

    public static Stream<Lane> stream() {
        return Stream.of(Lane.values());
    }

    public static Map<Lane, Map<Vehicle.Direction, Lane>> initNavigationMap() {
        Map<Lane, Map<Vehicle.Direction, Lane>> navigation = new HashMap<>();
        Map<Vehicle.Direction, Lane> laneA = new HashMap<>();
        Map<Vehicle.Direction, Lane> laneB = new HashMap<>();
        Map<Vehicle.Direction, Lane> laneC = new HashMap<>();
        Map<Vehicle.Direction, Lane> laneD = new HashMap<>();

        laneA.put(Vehicle.Direction.LEFT, Lane.LANE_B);
        laneA.put(Vehicle.Direction.RIGHT, Lane.LANE_D);
        laneA.put(Vehicle.Direction.STRAIGHT, Lane.LANE_C);

        laneB.put(Vehicle.Direction.LEFT, Lane.LANE_C);
        laneB.put(Vehicle.Direction.RIGHT, Lane.LANE_A);
        laneB.put(Vehicle.Direction.STRAIGHT, Lane.LANE_D);

        laneC.put(Vehicle.Direction.LEFT, Lane.LANE_D);
        laneC.put(Vehicle.Direction.RIGHT, Lane.LANE_B);
        laneC.put(Vehicle.Direction.STRAIGHT, Lane.LANE_A);

        laneD.put(Vehicle.Direction.LEFT, Lane.LANE_A);
        laneD.put(Vehicle.Direction.RIGHT, Lane.LANE_C);
        laneD.put(Vehicle.Direction.STRAIGHT, Lane.LANE_B);

        navigation.put(Lane.LANE_A, laneA);
        navigation.put(Lane.LANE_B, laneB);
        navigation.put(Lane.LANE_C, laneC);
        navigation.put(Lane.LANE_D, laneD);

        return navigation;

    }
}
