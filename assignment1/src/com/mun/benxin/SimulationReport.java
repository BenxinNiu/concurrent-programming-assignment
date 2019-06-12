package com.mun.benxin;

import java.util.List;

public class SimulationReport {

    private long totalServiceTime = 0;

    private List<Customer> customersWithService;
    private int minFulfillmentTime;
    private int maxFulfillmentTime;
    private int customerCount;
    private String simulationName;

    public SimulationReport(Builder builder) {
        this.simulationName = builder.simulationName;
        this.customersWithService = builder.customersWithService;
        this.minFulfillmentTime = builder.minFulfillmentTime;
        this.maxFulfillmentTime = builder.maxFulfillmentTime;
        this.customerCount = builder.customerCount;
        for (Customer customer: this.customersWithService){
            this.totalServiceTime += customer.getServiceTime();
        }
    }

    public void printSimResult() {
        System.out.println("\n" + this.simulationName + " Simulation Result: \n");
        System.out.println("Customer Arrival interval is between 50 and 100 seconds");
        System.out.println("Order fulfillment time (Without customer waiting time) is between "+ this.minFulfillmentTime + " and " + this.maxFulfillmentTime);
        System.out.println("There were a total of " + this.customerCount);
        System.out.println(this.customersWithService.size() + " of them got served");
        System.out.println(this.customerCount - this.customersWithService.size() + " of them did not get served");
        System.out.println("Average service time is (including customer waiting time) "+ this.totalServiceTime / this.customersWithService.size() + " seconds \n \n");
        System.out.println("Simulation finished, but some customer may still be waiting in other threads \n" +
                "this is because customer waiting is handled by different threads which may still be alive when simulation time reached: \n");
    }

    public static class Builder {
        private List<Customer> customersWithService;
        private int minFulfillmentTime;
        private int maxFulfillmentTime;
        private int customerCount;
        private String simulationName;

        public Builder(String simulationName) {
            this.simulationName = simulationName;
        }

        public Builder customersWithService(List<Customer> customersWithService) {
            this.customersWithService = customersWithService;
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

        public Builder customerCount(int customerCount) {
            this.customerCount = customerCount;
            return this;
        }

        public SimulationReport build() {
            return new SimulationReport(this);
        }
    }
}
