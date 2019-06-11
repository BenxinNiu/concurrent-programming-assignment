package com.mun.benxin;

public class Customer {

    private String customerId;

    private boolean isServed;

    private boolean isSecondTime;

    private boolean finishedWaiting;

    private int waitTime;

    private int secondWaitTime;

    public Customer(String customerId) {
        this.isSecondTime = false;
        this.isServed = false;
        this.finishedWaiting = false;
        this.customerId = customerId;
        this.secondWaitTime = 40;
        this.waitTime = 600;
    }

    public void waitAround(int seconds){
        try{
            if (this.isSecondTime)
                System.out.println("Customer "+customerId+" is coming back (second time) in "+ seconds +" seconds");
            else
                System.out.println("Customer "+customerId+" is coming back (first time) in "+ seconds +" seconds");
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

    public void setServed(boolean served) {
        isServed = served;
    }

    public boolean isSecondTime() {
        return isSecondTime;
    }

    public void setSecondTime(boolean secondTime) {
        isSecondTime = secondTime;
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

    public void setFinishedWaiting(boolean finishedWaiting) {
        this.finishedWaiting = finishedWaiting;
    }
}
