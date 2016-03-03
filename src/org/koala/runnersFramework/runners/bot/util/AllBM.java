package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.Random;

import org.koala.runnersFramework.runners.bot.HPDCJob;

public class AllBM {

	Bag myBag;	

	//ArrayList<Configuration> allPlans;
	public AllBM(Bag bag) {
		myBag = bag;
	/*	allPlans = generateAllBM(bag.size-1);
		for(Configuration plan : allPlans) {
			plan.printBM();
		} */
		
	
	}

	/*private ArrayList<Configuration> generateAllBM(int i) {
		ArrayList<Configuration> myconfigs = new ArrayList<Configuration>();
		if(i==0) {						
			for(Machine m : myBag.machines.values()) {
				Configuration initConfig = new Configuration(myBag.jobs.get(0),m);
				myconfigs.add(initConfig);
			}
		} else {
			ArrayList<Configuration> configs = generateAllBM(i-1);
			for(Configuration config : configs) {
				for(Machine m : myBag.machines.values()) {
					Configuration myConfig = new Configuration(myBag.jobs.get(i),m);
					myConfig.add(config);
					myconfigs.add(myConfig);
				}
			}			
		}
		return myconfigs;
	}*/
	
	public static void main (String args[]) {
		
		int size = args.length > 0 ? Integer.parseInt(args[0]) : 100;
		/*args related to distribution time interval are expressed in minutes by the user*/
		long budget = args.length > 1 ? Long.parseLong(args[1]) : 350;
		double mean = args.length > 2 ? Double.parseDouble(args[2])*60 : 15*60;
		double variance = args.length > 3 ? Double.parseDouble(args[3])*60 : Math.sqrt(5)*60;
/*
		double zeta = args.length > 1 ? Double.parseDouble(args[1]) : 1.96;
		double delta = args.length > 2 ? Double.parseDouble(args[2]) : 0.25;
		double zeta_sq = zeta * zeta;
		int noSampleJobs = (int) Math.ceil(size * zeta_sq
				/ (zeta_sq + 2 * (size - 1) * delta * delta));
		
		System.out.println("Sample number is " + noSampleJobs + " totalNumberTasks: " + size);
	*/	
		
		DifferentDistributionBag bag = new DifferentDistributionBag(size, budget, mean, variance);
		//AllBM allBM = new AllBM(bag);		
	}
}
