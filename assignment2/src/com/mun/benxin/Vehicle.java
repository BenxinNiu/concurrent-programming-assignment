package com.mun.benxin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Vehicle {
    private Direction direction;
    private String name;
    private Lane destination;
    private Lane origination;
    private Map<Lane, Map<Direction, Lane>> navigation = new HashMap<>();
    private boolean humanError;
    private Intersection intersection;
    private boolean finished = false;

    public enum Direction {
        STRAIGHT, RIGHT, LEFT
    }

    public Vehicle(String name, Lane origination, Intersection intersection, boolean humanError) {
        this.name = name;
        this.origination = origination;
        this.humanError = humanError;
        this.intersection = intersection;
        this.navigation = Lane.initNavigationMap();
        this.setDirectionAndDestination();
    }

    private void setDirectionAndDestination() {
        int index = ThreadLocalRandom.current().nextInt(0, 3);
        switch (index) {
            case 0: this.direction = Direction.LEFT; break;
            case 1: this.direction = Direction.RIGHT; break;
            case 2: this.direction = Direction.STRAIGHT; break;
            default: this.direction = Direction.STRAIGHT; break;
        }
        this.destination = this.navigation.get(this.origination).get(this.direction);
    }

    public void drive() {
         while (!this.finished) {
             try {
                 LightsControl.Lights current = this.intersection.getLightsControl().checkLight(this.origination);
                 if (current == LightsControl.Lights.GREEN || current == LightsControl.Lights.YELLOW){
                     this.intersection.getLineUpLock().get(this.origination).acquire();
                     switch (this.direction) {
                         case STRAIGHT: this.driveStraight(); break;
                         case LEFT: this.driveLeft(); break;
                         case RIGHT: this.driveRight(false, false); break;
                         default: break;
                     }
                     this.intersection.getLineUpLock().get(this.origination).release();
                 }
                 else {
                     this.driveRight(true, this.humanError);
                 }
             }
             catch (Exception ex) {}
         }
    }

    private void driveStraight() {
        this.depart();
    }

    private void driveLeft() {
        boolean hasIncoming;
        boolean showMsg = true;
        do {
            switch (this.origination) {
                case LANE_A: hasIncoming = this.intersection.getDepartureStatus(Lane.LANE_C); break;
                case LANE_B: hasIncoming = this.intersection.getDepartureStatus(Lane.LANE_D); break;
                case LANE_C: hasIncoming = this.intersection.getDepartureStatus(Lane.LANE_A); break;
                case LANE_D: hasIncoming = this.intersection.getDepartureStatus(Lane.LANE_B); break;
                default: hasIncoming = false;
            }
            if (hasIncoming&&showMsg) {
                showMsg=false;
                System.out.println(this.name + " is turning left and waiting in " + this.origination);
            }
        } while (hasIncoming);

        LightsControl.Lights current = this.intersection.getLightsControl().checkLight(this.origination);
        if (current == LightsControl.Lights.GREEN || current == LightsControl.Lights.YELLOW)
            this.depart();
    }

    private void driveRight(boolean wait, boolean humanError) {
        if(!wait) {
            this.depart();
        }
        else {
            boolean hasIncoming;
            do {
                if(!humanError) this.transition(3000);
                switch (this.origination) {
                    case LANE_A: hasIncoming = this.intersection.getDepartureStatus(Lane.LANE_B); break;
                    case LANE_B: hasIncoming = this.intersection.getDepartureStatus(Lane.LANE_C); break;
                    case LANE_C: hasIncoming = this.intersection.getDepartureStatus(Lane.LANE_D); break;
                    case LANE_D: hasIncoming = this.intersection.getDepartureStatus(Lane.LANE_A); break;
                    default: hasIncoming = false;
                }
                if (humanError && hasIncoming) {
                    this.finished = true;
                    hasIncoming =false;
                    System.out.println("\n" + this.name + " had collision at " + this.destination + "\n");
                    return;
                }
                if (hasIncoming) System.out.println(this.name + " is turning right and waiting in " + this.origination);
            } while (hasIncoming);
            this.depart();
        }
    }

    private void depart() {
        this.intersection.getLaneDepartureLock().get(this.origination).writeLock().lock();
        this.intersection.setDepartureStatus(this.origination, true);
        System.out.println(this.name + " is leaving " + this.origination + " to " + this.destination);
        this.intersection.getLaneDepartureLock().get(this.origination).writeLock().unlock();

        this.transition(1000);
        this.intersection.getLaneDepartureLock().get(this.origination).writeLock().lock();
        this.intersection.setDepartureStatus(this.origination, false);
        System.out.println(this.name + " entered " + this.destination);
        this.intersection.getLaneDepartureLock().get(this.origination).writeLock().unlock();

        this.finished = true;
    }

    private void transition(int time) {
        try { Thread.sleep(time); }
        catch (InterruptedException ex) { System.out.println("Interrupted");}
    }

    public Vehicle printInfo() {
        System.out.println(this.name);
        System.out.println("Direction: "+this.direction);
        System.out.println("Origination: "+this.origination);
        System.out.println("Destination: "+this.destination);
        System.out.println("");
        return this;
    }
}
