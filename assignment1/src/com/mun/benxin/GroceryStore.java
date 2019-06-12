package com.mun.benxin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GroceryStore {
    private List<Customer> checkoutLineOne;
    private List<Customer> checkoutLineTwo;
    private List<Customer> checkoutLineThree;
    private List<Customer> customersWithService;
    private List<Customer> customersWithoutService;
    private int currentBestLine;

    private int lineCapacity;
    private int avalibleLines;
    private int minServeTime;
    private int maxServeTime;
    private int simulationSeconds;
    private int id = 0;

    private ReadWriteLock checkoutLineOneLock;
    private ReadWriteLock checkoutLineTwoLock;
    private ReadWriteLock checkoutLineThreeLock;
    private ReadWriteLock currentBestLineLock;

    public GroceryStore() {
        this.checkoutLineOne = new ArrayList<>();
        this.checkoutLineTwo = new ArrayList<>();
        this.checkoutLineThree = new ArrayList<>();
        this.customersWithoutService = new ArrayList<>();
        this.customersWithService = new ArrayList<>();
        this.currentBestLine = 0;
        this.avalibleLines = 3;
        this.lineCapacity = 2;
        this.minServeTime = 300;
        this.maxServeTime = 600;
        this.simulationSeconds = 14400;

        this.checkoutLineOneLock = new ReentrantReadWriteLock();
        this.checkoutLineTwoLock = new ReentrantReadWriteLock();
        this.checkoutLineThreeLock = new ReentrantReadWriteLock();
        this.currentBestLineLock = new ReentrantReadWriteLock();
    }

    public void openBusiness() {
        new Thread(()-> assignCustomer()).start();
        new Thread(()->serve(0)).start();
        new Thread(()->serve(1)).start();
        new Thread(()->serve(2)).start();
    }

    private void serve (int lineNumber) {
        while (true) {
            this.fulfillOrder(lineNumber);
        }
    }

    private void fulfillOrder (int lineNumber) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(this.minServeTime, this.maxServeTime));
            this.getLockInstance(lineNumber).writeLock().lock();
            if(this.getCheckoutLine(lineNumber).size() > 0){
                Customer customer = this.getCheckoutLine(lineNumber).get(0);
                this.customersWithService.add(customer);
                this.getCheckoutLine(lineNumber).remove(0);
                System.out.println("Customer "+ customer.getCustomerId()+ " on line "+lineNumber+" has been served");
                new Thread(()->updateBestLine()).start();
            }
            this.getLockInstance(lineNumber).writeLock().unlock();
        }
        catch (InterruptedException ex) {

        }
    }

    private void assignCustomer(){
        while (true) {
            Customer customer = new Customer("c"+id++);
            this.currentBestLineLock.readLock().lock();
            if (this.currentBestLine >= 0)
                this.tryJoinLine(this.currentBestLine, customer);
            else
                new Thread(()->waitAround(customer, true)).start();
            this.currentBestLineLock.readLock().unlock();
            try {Thread.sleep(100);} catch (InterruptedException ex) {}
        }
    }

    private void tryJoinLine(int lineNumber, Customer customer) {
        this.getLockInstance(lineNumber).writeLock().lock();
        if (this.getCheckoutLine(lineNumber).size()<this.lineCapacity) {
            this.getCheckoutLine(lineNumber).add(customer);
            System.out.println("Customer "+ customer.getCustomerId() +" joined line "+lineNumber);
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
                System.out.println("Customer "+customer.getCustomerId()+" is coming back in 600 seconds");
                Thread.sleep(600);
                new Thread(()-> waitAround(customer, false)).start();
            }
            catch (InterruptedException ex) {
                System.out.println("Thread interrupted: during moving around 600 seconds");
            }
        }
    }

    private List<Customer> getCheckoutLine(int lineNumber) {
        switch (lineNumber){
            case 0: return this.checkoutLineOne;
            case 1: return this.checkoutLineTwo;
            case 2: return this.checkoutLineThree;
            default: return this.checkoutLineOne;
        }
    }

    private ReadWriteLock getLockInstance(int lineNumber) {
        switch (lineNumber){
            case 0: return this.checkoutLineOneLock;
            case 1: return this.checkoutLineTwoLock;
            case 2: return this.checkoutLineThreeLock;
            default: return this.checkoutLineOneLock;
        }
    }

}
