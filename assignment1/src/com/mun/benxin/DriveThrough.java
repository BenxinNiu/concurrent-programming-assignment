package com.mun.benxin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class DriveThrough {

    private String businessName;
    private Semaphore dispatchLineSem = new Semaphore(1, true);
    private Semaphore fulfillmentLineMutex = new Semaphore(1, true);

    private List<Customer> dispatchLineOne;
    private List<Customer> dispatchLineTwo;
    private List<Customer> fulfillmentLine;
    private int currentFulfillmentTime;
    private boolean lineToggle;
    private int customerId = 0;
    private int servedCustomer = 0;
    private long totalServiceTime = 0;
    private int missedCustomer = 0;

    private int staffCount;
    private int minFulfillmentTime;
    private int maxFulfillmentTime;
    private int lineCapacity;

    private DriveThrough(Builder builder) {
        this.businessName = builder.businessName;
        this.staffCount = builder.staffCount;
        this.minFulfillmentTime = builder.minFulfillmentTime;
        this.maxFulfillmentTime = builder.maxFulfillmentTime;
        this.lineCapacity = builder.lineCapacity;

        this.lineToggle = false;
        this.currentFulfillmentTime = minFulfillmentTime;
        this.dispatchLineOne = new ArrayList<>();
        this.dispatchLineTwo = new ArrayList<>();
        this.fulfillmentLine = new ArrayList<>();
    }

    /**
     * method to "start the drive through business"
     * by instantiating # of staff (threads) to serve from fulfillment line
     *
     */
    public void startBusiness() {
        new Thread(() -> mergeLine()).start();
        for (int i = 0; i < this.staffCount; i++) {
            new Thread(() -> serve()).start();
        }
        new Thread(() -> assignCustomers()).start();
    }

    /**
     * method to fulfill customer order
     * each staff fights to serve, and winning staff check if there is any order
     * if there is, then fulfill, otherwise relinquish the position for other "staff"(thread)
     */
    private void serve() {
        while (true) {
            try {
                this.fulfillmentLineMutex.acquire();
                if (this.fulfillmentLine.size() > 0) this.fulfillOrder();
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
            Customer customer = this.fulfillmentLine.get(0);
            String greeting = customer.isSecondTime() ? " Apologize for waiting!!!" : "";
            System.out.println("Customer at pickup point, fulfillment begin... " + greeting);
            Thread.sleep(this.currentFulfillmentTime++);
            this.fulfillmentLine.remove(0);
            this.currentFulfillmentTime = currentFulfillmentTime >= maxFulfillmentTime ? minFulfillmentTime : currentFulfillmentTime;
            System.out.println("Customer order fulfilled; Total fulfillment time: " + this.currentFulfillmentTime);
            this.servedCustomer++;
            this.totalServiceTime += this.currentFulfillmentTime;
        } catch (InterruptedException ex) {
            System.out.println("Thread Interrupted during order fulfillment");
        }
    }

    /**
     * method to move customer(s) from dispatch lines to fulfillment line
     *
     */
    private void mergeLine() {
        while (true) {
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
        while (true) {
            try{
                Customer customer = new Customer("customer-"+ this.customerId++);
                synchronized (customer){
                    this.dispatchLineSem.acquire();
                    boolean lineFull = this.dispatchLineOne.size() + this.dispatchLineTwo.size() >= this.lineCapacity*2;
                    if (lineFull){
                        new Thread(()-> waitAndComeback(customer, true)).start();
                        this.dispatchLineSem.release();
                    }
                    else {
                        this.toDispatcher(customer);
                        this.dispatchLineSem.release();
                    }
                    Thread.sleep(100);
                }
            }
            catch (InterruptedException ex) {
                System.out.print("Thread Interruption during assigning customer");
            }
        }
    }

    private void waitAndComeback(Customer customer, boolean firstTime){
        synchronized (customer){
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
            if(!success && !firstTime) this.missedCustomer ++;
            if (!success && firstTime) {
                try {
                    System.out.println("Customer is coming back in 600 seconds");
                    Thread.sleep(600);
                    new Thread(()-> waitAndComeback(customer, false)).start();
                }
                catch (InterruptedException ex) {
                    System.out.println("Thread interrupted: during moving around 600 seconds");
                }
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

    /**
     * Implement builder pattern
     */
    public static class Builder {
        private String businessName;
        private int staffCount;
        private int minFulfillmentTime;
        private int maxFulfillmentTime;
        private int lineCapacity;


        public Builder(String name){
            this.businessName = name;
        }

        public Builder staffCount(int staffCount) {
            this.staffCount = staffCount;
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
