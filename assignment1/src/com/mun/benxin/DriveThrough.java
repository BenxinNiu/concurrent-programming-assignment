package com.mun.benxin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class DriveThrough {

    private String businessName;
    private Semaphore dispatchLineSem = new Semaphore(1, true);
    private Semaphore fulfillmentLineMutex = new Semaphore(1, true);

    private List<Customer> dispatchLineOne;
    private List<Customer> dispatchLineTwo;
    private List<Customer> fulfillmentLine;
    private List<Customer> customersWithService;
    private boolean lineToggle;
    private int customerCount = 0;

    private int staffCount;
    private int minFulfillmentTime;
    private int maxFulfillmentTime;
    private int lineCapacity;
    private long simulationSeconds;

    private DriveThrough(Builder builder) {
        this.businessName = builder.businessName;
        this.staffCount = builder.staffCount;
        this.minFulfillmentTime = builder.minFulfillmentTime;
        this.maxFulfillmentTime = builder.maxFulfillmentTime;
        this.lineCapacity = builder.lineCapacity;
        this.simulationSeconds = builder.simulationSecond + System.currentTimeMillis();

        this.lineToggle = false;
        this.dispatchLineOne = new ArrayList<>();
        this.dispatchLineTwo = new ArrayList<>();
        this.fulfillmentLine = new ArrayList<>();
        this.customersWithService = new ArrayList<>();
    }

    /**
     * method to "start the drive through business"
     * by instantiating # of staff (threads) to serve from fulfillment line
     *
     */
    public void startBusiness() {
        boolean simulationRunning = true;
        List<Thread> pool = new ArrayList<>();
        pool.add(new Thread(this::mergeLine));
        pool.add(new Thread(this::assignCustomers));
        for (int i = 0; i < this.staffCount; i++) {
            pool.add(new Thread(this::serve));
        }
        pool.forEach(Thread::start);

        while (simulationRunning) {
            int aliveCount = 0;
            for(Thread t: pool){
                if (t.isAlive()) aliveCount++;
            }
            simulationRunning = aliveCount != 0;
        }

        new SimulationReport.Builder("Drive Through")
                .customersWithService(this.customersWithService)
                .customerCount(this.customerCount)
                .maxFulfillmentTime(this.maxFulfillmentTime)
                .minFulfillmentTime(this.minFulfillmentTime)
                .build().printSimResult();
    }

    /**
     * method to fulfill customer order
     * each staff fights to serve, and winning staff check if there is any order
     * if there is, then fulfill, otherwise relinquish the position for other "staff"(thread)
     */
    private void serve() {
        while (System.currentTimeMillis() < this.simulationSeconds) {
            try {
                this.fulfillmentLineMutex.acquire();
                if (this.fulfillmentLine.size() > 0)
                    this.fulfillOrder();
                this.fulfillmentLineMutex.release();
            } catch (InterruptedException ex) {
                System.out.println("Thread interrupted during acquiring lock");
            }
        }
    }

    /**
     * private method for fulfilling process
     * fulfillment time are uniformly distributed by increment time for each order and
     * reset it when reached to max amount
     */
    private void fulfillOrder() {
        try {
            int currentFulfillmentTime = ThreadLocalRandom.current().nextInt(this.minFulfillmentTime, this.maxFulfillmentTime);
            Customer customer = this.fulfillmentLine.get(0);
            String greeting = customer.isSecondTime() ? " Apologize for waiting!!!" : "";
            System.out.println("Customer at pickup point, fulfillment begin... " + greeting);
            Thread.sleep(currentFulfillmentTime);
            this.fulfillmentLine.remove(0);
            System.out.println("Customer order fulfilled; Total fulfillment time: " + currentFulfillmentTime);
            this.customersWithService.add(customer.setServed(true).setServiceTime(currentFulfillmentTime));
        } catch (InterruptedException ex) {
            System.out.println("Thread Interrupted during order fulfillment");
        }
    }

    /**
     * method to move customer(s) from dispatch lines to fulfillment line
     *
     */
    private void mergeLine() {
        while (System.currentTimeMillis() < this.simulationSeconds) {
            try {
                this.fulfillmentLineMutex.acquire();
                this.dispatchLineSem.acquire();
                for (int i=0; i<this.lineCapacity-this.fulfillmentLine.size(); i++) {
                    this.toFulfillmentLine();
                }
                this.dispatchLineSem.release();
                this.fulfillmentLineMutex.release();
            } catch (InterruptedException ex) {
                System.out.println("Thread interrupted during acquiring lock");
            }
        }
    }

    /**
     * method for moving a customer to fulfillment line
     */
    private void toFulfillmentLine() {
        if (this.lineToggle && this.dispatchLineOne.size() > 0) {
            this.fulfillmentLine.add(this.dispatchLineOne.get(0));
            System.out.println("Customer is moving to fulfillment line from dispatch one");
            this.dispatchLineOne.remove(0);
        }
        else if(this.dispatchLineTwo.size() > 0) {
            this.fulfillmentLine.add(this.dispatchLineTwo.get(0));
            System.out.println("Customer is moving to fulfillment line from dispatch two");
            this.dispatchLineTwo.remove(0);
        }
        else if (this.dispatchLineOne.size() > 0) {
            this.fulfillmentLine.add(this.dispatchLineOne.get(0));
            System.out.println("Customer  is moving to fulfillment line from dispatch one");
            this.dispatchLineOne.remove(0);
        }
        this.lineToggle = !this.lineToggle;
    }

    private void assignCustomers() {
        while (System.currentTimeMillis() < this.simulationSeconds) {
            try{
                Customer customer = new Customer("customer-"+ this.customerCount++);
                synchronized (customer){
                    this.dispatchLineSem.acquire();
                    boolean lineFull = this.dispatchLineOne.size() + this.dispatchLineTwo.size() >= this.lineCapacity*2;
                    if (lineFull){
                        new Thread(()-> waitOrComeback(customer, true)).start();
                        this.dispatchLineSem.release();
                    }
                    else {
                        this.toDispatcher(customer);
                        this.dispatchLineSem.release();
                    }

                    Thread.sleep(ThreadLocalRandom.current().nextInt(50, 101));
                }
            }
            catch (InterruptedException ex) {
                System.out.print("Thread Interruption during assigning customer");
            }
        }
    }

    private void toDispatcher(Customer customer) {
        if (this.dispatchLineOne.size() > this.dispatchLineTwo.size()){
            if(this.dispatchLineTwo.size() < this.lineCapacity) this.dispatchLineTwo.add(customer);
            else if (this.dispatchLineOne.size() < this.lineCapacity) this.dispatchLineOne.add(customer);
            System.out.println("Customer entered dispatch line");
        }
        else {
            if(this.dispatchLineOne.size() < this.lineCapacity) this.dispatchLineOne.add(customer);
            else if (this.dispatchLineTwo.size() < this.lineCapacity) this.dispatchLineTwo.add(customer);
            System.out.println("Customer entered dispatch line");
        }
    }

    private void waitOrComeback(Customer customer, boolean firstTime){
        Thread waitingSeat = firstTime ? new Thread(() -> customer.waitAround(customer.getWaitTime())) : new Thread(() -> customer.waitAround(customer.getSecondWaitTime()));
        waitingSeat.start();
        boolean success = false;
        while(waitingSeat.isAlive()) {
            if (this.dispatchLineSem.tryAcquire()){
                boolean lineFull = this.dispatchLineOne.size() + this.dispatchLineTwo.size() >= this.lineCapacity*2;
                if(!lineFull){
                    this.toDispatcher(customer);
                    this.dispatchLineSem.release();
                    success = true;
                    break;
                }
                this.dispatchLineSem.release();
            }
        }
        if (!success && firstTime) {
            try {
                System.out.println("Customer is coming back in 600 seconds");
                Thread.sleep(600);
                new Thread(()-> waitOrComeback(customer, false)).start();
            }
            catch (InterruptedException ex) {
                System.out.println("Thread interrupted: during moving around 600 seconds");
            }
        }
    }

    /**
     * Implement builder pattern
     */
    public static class Builder {
        private String businessName;
        private int staffCount;
        private int minFulfillmentTime;
        private int maxFulfillmentTime;
        private int lineCapacity;
        private long simulationSecond;


        public Builder(String name){
            this.businessName = name;
        }

        public Builder staffCount(int staffCount) {
            this.staffCount = staffCount;
            return this;
        }

        public Builder simulationSeconds(long sec) {
            this.simulationSecond = sec;
            return this;
        }

        public Builder minFulfillmentTime(int minFulfillmentTime) {
            this.minFulfillmentTime = minFulfillmentTime;
            return this;
        }

        public Builder maxFulfillmentTime(int maxFulfillmentTime) {
            this.maxFulfillmentTime = maxFulfillmentTime;
            return this;
        }

        public Builder lineCapacity(int lineCapacity) {
            this.lineCapacity = lineCapacity;
            return this;
        }

        public DriveThrough build() {
            return new DriveThrough(this);
        }
    }
}
