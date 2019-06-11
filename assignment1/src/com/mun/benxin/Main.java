package com.mun.benxin;

public class Main {

    public static void main(String[] args) {
	// write your code here
        DriveThrough popEyeChicken = new DriveThrough.Builder("Pop eye")
                .staffCount(3).minFulfillmentTime(300)
                .maxFulfillmentTime(600).lineCapacity(3)
                .build();
        popEyeChicken.startBusiness();
    }
}
