package com.mun.benxin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GroceryStore {
    private List<List<Customer>> checkoutLines;
    private List<Customer> customersWithService;
    private int currentBestLine;
    private int customerCount = 0;

    private int lineCapacity;
    private int avalibleLines;
    private int minServeTime;
    private int maxServeTime;
    private long simulationSeconds;

    private List<ReadWriteLock> locks;
    private ReadWriteLock currentBestLineLock;

    public GroceryStore(Builder builder) {
        this.locks = new ArrayList<>();
        this.checkoutLines = new ArrayList<>();
        this.customersWithService = new ArrayList<>();
        this.currentBestLine = 0;

        this.avalibleLines = builder.avalibleLines;
        this.lineCapacity = builder.lineCapacity;
        this.minServeTime = builder.minServeTime;
        this.maxServeTime = builder.maxServeTime;
        this.simulationSeconds = builder.simulationSecond + System.currentTimeMillis();

        this.currentBestLineLock = new ReentrantReadWriteLock();
        for (int i=0; i<this.avalibleLines+1; i++) {
            this.checkoutLines.add(new ArrayList<>());
            this.locks.add(new ReentrantReadWriteLock());
        }
    }

    public void openBusiness() {
        boolean simulationFinished = false;
        List<Thread> pool = new ArrayList<>();
        pool.add(new Thread(this::assignCustomer));
        for (int i=0; i<this.avalibleLines; i++){
            int lineId = i; // make this effective final for lambda reference
            pool.add(new Thread(()->serve(lineId)));
        }
        pool.forEach(Thread::start);

        while(!simulationFinished) {
            int aliveCount = 0;
            for(Thread t: pool){
                if (t.isAlive()) aliveCount++;
            }
            simulationFinished = aliveCount == 0;
        }

        new SimulationReport.Builder("Grocery")
                .customersWithService(this.customersWithService)
                .customerCount(this.customerCount)
                .maxFulfillmentTime(this.maxServeTime)
                .minFulfillmentTime(this.minServeTime)
                .build().printSimResult();
    }

    private void serve (int lineNumber) {
        while (System.currentTimeMillis() < this.simulationSeconds) {
            this.fulfillOrder(lineNumber);
        }
    }

    private void fulfillOrder (int lineNumber) {
        try {
            long fulfillmentTime = ThreadLocalRandom.current().nextInt(this.minServeTime, this.maxServeTime);
            Thread.sleep(fulfillmentTime);
            this.getLockInstance(lineNumber).writeLock().lock();
            if(this.getCheckoutLine(lineNumber).size() > 0){
                Customer customer = this.getCheckoutLine(lineNumber).get(0);
                this.getLockInstance(this.avalibleLines).writeLock().lock();
                this.customersWithService.add(customer.setServiceTime(fulfillmentTime));
                this.getLockInstance(this.avalibleLines).writeLock().unlock();
                this.getCheckoutLine(lineNumber).remove(0);
                String greeting = customer.isSecondTime() ? " Apologize for waiting!!!" : "";
                System.out.println("Customer on line "+lineNumber+" has been served "+greeting);
                new Thread(()->updateBestLine()).start();
            }
            this.getLockInstance(lineNumber).writeLock().unlock();
        }
        catch (InterruptedException ex) {

        }
    }

    private void assignCustomer(){
        while (System.currentTimeMillis() < this.simulationSeconds) {
            Customer customer = new Customer("c"+ customerCount++);
            this.currentBestLineLock.readLock().lock();
            if (this.currentBestLine >= 0)
                this.tryJoinLine(this.currentBestLine, customer);
            else
                new Thread(()->waitAround(customer, true)).start();
            this.currentBestLineLock.readLock().unlock();
            try {Thread.sleep(ThreadLocalRandom.current().nextInt(50, 101));}
            catch (InterruptedException ex) {}
        }
    }

    private void tryJoinLine(int lineNumber, Customer customer) {
        this.getLockInstance(lineNumber).writeLock().lock();
        if (this.getCheckoutLine(lineNumber).size()<this.lineCapacity) {
            this.getCheckoutLine(lineNumber).add(customer);
            System.out.println("Customer joined line "+lineNumber);
            new Thread(()->updateBestLine()).start();
        }
        this.getLockInstance(lineNumber).writeLock().unlock();
    }

    private void updateBestLine() {
        int bestLine = -1;
        int best = lineCapacity;
        this.currentBestLineLock.writeLock().lock();

        for (int i=0; i<this.avalibleLines; i++){
            this.getLockInstance(i).readLock().lock();
            if(this.getCheckoutLine(i).size() < best){
                best=this.getCheckoutLine(i).size();
                bestLine = i;
            }
            this.getLockInstance(i).readLock().unlock();
        }
        this.currentBestLine = bestLine;
        this.currentBestLineLock.writeLock().unlock();
    }

    private void waitAround(Customer customer, boolean firstTime) {
        boolean success = false;
        Thread waiting = firstTime ? new Thread(()->customer.waitAround(customer.getWaitTime()))
                : new Thread(()->customer.waitAround(customer.getSecondWaitTime()));
        waiting.start();
        while (waiting.isAlive()){
            this.currentBestLineLock.readLock().lock();
            if (this.currentBestLine >= 0){
                this.currentBestLineLock.readLock().unlock();
                success = true;
                this.tryJoinLine(this.currentBestLine, customer);
                break;
            }
            this.currentBestLineLock.readLock().unlock();
        }
        if (!success && firstTime){
            try {
                System.out.println("Customer is coming back in 600 seconds");
                Thread.sleep(600);
                new Thread(()-> waitAround(customer, false)).start();
            }
            catch (InterruptedException ex) {
                System.out.println("Thread interrupted: during moving around 600 seconds");
            }
        }
    }

    private List<Customer> getCheckoutLine(int lineNumber) {
        return this.checkoutLines.get(lineNumber);
    }

    private ReadWriteLock getLockInstance(int lineNumber) {
        return this.locks.get(lineNumber);
    }

    public static class Builder {
        private String name;
        private int lineCapacity;
        private int avalibleLines;
        private int minServeTime;
        private int maxServeTime;
        private long simulationSecond;

        public Builder(String name) {
            this.name = name;
        }

        public Builder lineCapacity(int lineCapacity) {
            this.lineCapacity = lineCapacity;
            return this;
        }

        public Builder avalibleLines(int avalibleLines) {
            this.avalibleLines = avalibleLines;
            return this;
        }

        public Builder minServeTime(int minServeTime) {
            this.minServeTime = minServeTime;
            return this;
        }

        public Builder maxServeTime(int maxServeTime) {
            this.maxServeTime = maxServeTime;
            return this;
        }

        public Builder simulationSeconds(long simulationSeconds) {
            this.simulationSecond = simulationSeconds;
            return this;
        }

        public GroceryStore build() {
            return new GroceryStore(this);
        }
    }

}
