package com.mun.benxin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LightsControl {

    private Map<Lane, Lights> status = new HashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private List<Lane> lightChange = new ArrayList<>();

    public enum Lights {
        RED, GREEN, YELLOW
    }

    public LightsControl() {
        Lane.stream().forEach(l -> {
            this.status.put(l, Lights.RED);
            this.lightChange.add(l);
        });
        this.updateLight(this.lightChange.get(0),this.lightChange.get(1),Lights.GREEN);
    }

    public void toggleLights(int transition) {
        this.lock.writeLock().lock();
        this.updateLight(this.lightChange.get(0), this.lightChange.get(1), Lights.YELLOW);
        this.lock.writeLock().unlock();

        try { Thread.sleep(transition); }
        catch (InterruptedException e) { e.printStackTrace(); }

        this.lock.writeLock().lock();
        this.updateLight(this.lightChange.get(0), this.lightChange.get(1), Lights.RED);
        this.updateLight(this.lightChange.get(2), this.lightChange.get(3), Lights.GREEN);
        this.swapChangeLightOrder();
        this.lock.writeLock().unlock();
    }

    private void updateLight(Lane lane1, Lane lane2, Lights light) {
        this.status.replace(lane1, light);
        this.status.replace(lane2, light);
    }

    public Lights checkLight(Lane lane) {
        this.lock.readLock().lock();
        Lights light = this.status.get(lane);
        this.lock.readLock().unlock();
        return light;
    }

    private void swapChangeLightOrder(){
        Lane laneOne = this.lightChange.get(0);
        Lane laneTwo = this.lightChange.get(1);
        this.lightChange.set(0, this.lightChange.get(2));
        this.lightChange.set(1, this.lightChange.get(3));
        this.lightChange.set(2, laneOne);
        this.lightChange.set(3, laneTwo);
    }

}
