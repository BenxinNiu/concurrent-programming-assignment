package com.mun.benxin;

import java.util.Objects;

public class Customer {

    private String customerId;

    private boolean isServed;

    private boolean isSecondTime;

    private boolean finishedWaiting;

    private int waitTime;

    private int secondWaitTime;

    private long arrivalTime;

    private long serviceTime;

    public Customer(String customerId) {
        this.isSecondTime = false;
        this.isServed = false;
        this.finishedWaiting = false;
        this.customerId = customerId;
        this.secondWaitTime = 40;
        this.waitTime = 20;
        this.serviceTime=0;
        this.arrivalTime = System.currentTimeMillis() % 1000;
    }

    public void waitAround(int seconds){
        try{
            if (this.isSecondTime)
                System.out.println("Customer is waiting (second time) for "+ seconds +" seconds");
            else
                System.out.println("Customer is waiting (first time) for "+ seconds +" seconds");
            this.isSecondTime = true;
            this.finishedWaiting = false;
            Thread.sleep(seconds);
            this.finishedWaiting = true;
        }
        catch (InterruptedException ex){

        }
    }

    public String getCustomerId() {
        return customerId;
    }

    public boolean isServed() {
        return isServed;
    }

    public boolean isSecondTime() {
        return isSecondTime;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public int getSecondWaitTime() {
        return secondWaitTime;
    }

    public boolean isFinishedWaiting() {
        return finishedWaiting;
    }

    public Customer setServed(boolean served) {
        isServed = served;
        return this;
    }

    public Customer setServiceTime(long fulfillmentTime){
        this.serviceTime = (System.currentTimeMillis() % 1000) - this.arrivalTime + fulfillmentTime;
        return this;
    }

    public long getServiceTime() {
        return serviceTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return isServed == customer.isServed &&
                isSecondTime == customer.isSecondTime &&
                finishedWaiting == customer.finishedWaiting &&
                waitTime == customer.waitTime &&
                secondWaitTime == customer.secondWaitTime &&
                customerId.equals(customer.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, isServed, isSecondTime, finishedWaiting, waitTime, secondWaitTime);
    }
}
