package com.mun.benxin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Intersection {

    private boolean ALaneDepartureBusy = false;
    private boolean BLaneDepartureBusy = false;
    private boolean CLaneDepartureBusy = false;
    private boolean DLaneDepartureBusy = false;

    private Map<Lane, ReadWriteLock> laneDepartureLock = new HashMap<>();
    private Map<Lane, Semaphore> lineUpLock = new HashMap<>();
    private LightsControl lightsControl;

    private List<Vehicle> vehiclePool;

    public Intersection(LightsControl lightsControl) {
        this.lightsControl = lightsControl;
        Lane.stream().forEach(lane -> {
            this.laneDepartureLock.put(lane, new ReentrantReadWriteLock());
            this.lineUpLock.put(lane, new Semaphore(1, false));
        });
    }

    public Intersection setVehiclePool(List<Vehicle> vehiclePool) {
        this.vehiclePool = vehiclePool;
        return this;
    }

    public void liveTraffic() {
        List<Thread> pool = new ArrayList<>();
        this.vehiclePool.forEach(v->{
            v.printInfo();
            pool.add(new Thread(()->v.drive()));
        });

        pool.forEach(Thread::start);

        while (!this.isTrafficClear(pool)) {
            try {
                Thread.sleep(4000);
                this.lightsControl.toggleLights(500);
            }
            catch (InterruptedException ex){}
        }
    }

    private boolean isTrafficClear(List<Thread> pool) {
        for (Thread t: pool) {
            if(t.isAlive())
                return false;
        }
        return true;
    }


    public void setDepartureStatus(Lane lane, boolean status) {
        switch (lane) {
            case LANE_A: this.ALaneDepartureBusy = status; break;
            case LANE_B: this.BLaneDepartureBusy = status; break;
            case LANE_C: this.CLaneDepartureBusy = status; break;
            case LANE_D: this.DLaneDepartureBusy = status; break;
            default: break;
        }
    }

    public boolean getDepartureStatus(Lane lane) {
        boolean isBusy;
        switch (lane) {
            case LANE_A:
                this.laneDepartureLock.get(Lane.LANE_A).readLock().lock();
                isBusy = this.ALaneDepartureBusy;
                this.laneDepartureLock.get(Lane.LANE_A).readLock().unlock();
                break;
            case LANE_B:
                this.laneDepartureLock.get(Lane.LANE_B).readLock().lock();
                isBusy = this.BLaneDepartureBusy;
                this.laneDepartureLock.get(Lane.LANE_B).readLock().unlock();
                break;
            case LANE_C:
                this.laneDepartureLock.get(Lane.LANE_C).readLock().lock();
                isBusy = this.CLaneDepartureBusy;
                this.laneDepartureLock.get(Lane.LANE_C).readLock().unlock();
                break;
            case LANE_D:
                this.laneDepartureLock.get(Lane.LANE_D).readLock().lock();
                isBusy = this.DLaneDepartureBusy;
                this.laneDepartureLock.get(Lane.LANE_D).readLock().unlock();
                break;
            default: isBusy = true;
        }
        return isBusy;
    }


    public Map<Lane, ReadWriteLock> getLaneDepartureLock() {
        return laneDepartureLock;
    }

    public Map<Lane, Semaphore> getLineUpLock() {
        return lineUpLock;
    }

    public LightsControl getLightsControl() {
        return lightsControl;
    }
}
