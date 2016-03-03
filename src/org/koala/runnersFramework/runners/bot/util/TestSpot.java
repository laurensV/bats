package org.koala.runnersFramework.runners.bot.util;

import ibis.ipl.registry.statistics.Experiment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Random;

enum Strategy {
	Current, Max, Avg, OnDemand;
}

public class TestSpot {
	
	public static int noExperiments = 1;
	public static int noExecutions = 1;
	private static int noSampleExperiments;
	
	private static int spot, demand;

	static PriceFluctuationsSimulator priceSim = new PriceFluctuationsSimulator();
	
	static int rateOfChangeDelay = 10;
	
	public static void main (String args[]) throws FileNotFoundException {
		//System.setOut(new PrintStream(new File("output.txt")));
		System.out.println(java.lang.Runtime.getRuntime().maxMemory()); 
		noSampleExperiments = Integer.parseInt(args[1]);
		noExecutions = 1;
		
		int sampleSize = 30;
		
		double desiredStDev = Math.sqrt(5);
		double desiredVar = desiredStDev * desiredStDev;
		long experimentBagGeneratingSeed;
		Random randomizedSeed = new Random(99999999L);
		experimentBagGeneratingSeed = randomizedSeed.nextLong();
		
//		experimentBagGeneratingSeed = Long.parseLong(args[1]);
		if (args[0].equals("50")) {
			spot = 50;
			demand = 50;
		} else if (args[0].equals("40")) {
			spot = 60;
			demand = 40;
		} else if (args[0].equals("100")){
			spot = 100;
			demand = 100;
		} else {
			spot = 20;
			demand = 20;
		}
		
		System.out.println("Run type");
		System.out.println(spot);
		System.out.println(demand);
		
		//String bagType = args[0];
		String bagType = "NDB";
		String budgetType = "Bmin";
		//budgetType = "BminPlus20";
		//budgetType = "BminPlus10";
		budgetType = "BmakespanMin";
		//budgetType = "BmakespanMinMinus20";
	//	budgetType = "BmakespanMinMinus10";
	
		
		System.out.println("bag type: " + bagType);
		//changed from cost 3 to cost 200
		if (bagType.equals("NDB")) {
			SpotBag ndb = new SpotBag(1000, 
					experimentBagGeneratingSeed,
					60*60, desiredStDev*60,
					1, 1, 1, priceSim, 100);
			ndb.sampleSize = sampleSize;
			testSpotSamplePhaseMultipleClustersNDB(budgetType, ndb);
		}
		
		if (bagType.equals("LDB")) {
			LevySpotBag ltb = new LevySpotBag(1000, 
					experimentBagGeneratingSeed, 15*60, desiredStDev*60,
					0.366, 2700,
					3, 4, 3, priceSim, 100);
			ltb.sampleSize = sampleSize;
			testSpotSamplePhaseMultipleClusters(budgetType, ltb);
		}
		if (bagType.equals("MDB")) {
			double mean1 = 48; double stdev1 = 29;
			double low2 = 106; double high2 = 607;
			double mean3 = 689; double stdev3 = 27;
			double low4 = 771; double high4 = 892;
			double low5 = 1649; double high5 = 2553;

			Random MDSeeder = new Random(experimentBagGeneratingSeed);
			long experimentBagGeneratingSeed0 = MDSeeder.nextLong();
			long experimentBagGeneratingSeed1 = MDSeeder.nextLong();
			long experimentBagGeneratingSeed2 = MDSeeder.nextLong();
			long experimentBagGeneratingSeed3 = MDSeeder.nextLong();
			long experimentBagGeneratingSeed4 = MDSeeder.nextLong();
			long experimentBagGeneratingSeed5 = MDSeeder.nextLong();

			DACHSpotBag mdb = new DACHSpotBag(1000,
					experimentBagGeneratingSeed1, mean1, stdev1,
					experimentBagGeneratingSeed2, low2, high2,
					experimentBagGeneratingSeed3, mean3, stdev3,
					experimentBagGeneratingSeed4, low4, high4,
					experimentBagGeneratingSeed5, low5, high5,
					0.274111675, 0.222335025, 0.452791878, 0.026395939,  
					experimentBagGeneratingSeed0,
					3,4,3, priceSim, 100);
			mdb.sampleSize = sampleSize;
			testSpotSamplePhaseMultipleClustersDACH(budgetType, mdb);
		}
	}

	private static void testSpotSamplePhaseMultipleClustersNDB(String budgetType, SpotBag bag) {
		boolean printXLS = false;
		boolean printSampleSummary = false;
		boolean printNodeStats = true;
		
		
		SimulatedCluster clusterA, clusterB, clusterC, clusterAD, clusterBD, clusterCD;
		HashMap<String,SimulatedCluster> clusters;
				
		Random sampleSeeder = new Random(111111111L);
		Random executionSeeder = new Random(888888888L);
		
		long sampleSeed, executionSeed;
		
		double p_mu = 0.9, p_sigma = 0.9, p_M = 0.9;

		int sampleSize = bag.sampleSize;
								
		boolean printInfo = false;
				
		
				
		bag.printXLS = printXLS;
		bag.printSampleSummary = printSampleSummary;
		bag.printNodeStats = printNodeStats;
		
		//speedFactor, costFactor, name, maxNodes, startIndex, file, strategy
		//currently costFactor is not really used in the computation
		//diff from 50 to 60 for normal BKP
		clusterA = new SimulatedSpotCluster(1, 2, "t1.micro", spot, 1, "us-east-1a-t1.micro.out", Strategy.Current, 20);
		clusterB = new SimulatedSpotCluster(2, 4, "m1.small", spot, 1, "us-east-1a-m1.small.out", Strategy.Current, 65);
		clusterC = new SimulatedSpotCluster(3, 5, "m1.medium", spot, 1, "us-east-1a-m1.medium.out", Strategy.Current, 130);
		
		/*
		clusterA = new SimulatedSpotCluster(1, 2, "t1.micro", 24, 1, "us-east-1a-t1.micro.out", Strategy.Avg, 20);
		clusterB = new SimulatedSpotCluster(2, 4, "m1.small", 24, 1, "us-east-1a-m1.small.out", Strategy.Avg, 65);
		clusterC = new SimulatedSpotCluster(3, 5, "m1.medium", 24, 1, "us-east-1a-m1.medium.out", Strategy.Avg, 130);
		*/
		/*
		clusterA = new SimulatedSpotCluster(1, 2, "t1.micro", 24, 1, "us-east-1a-t1.micro.out", Strategy.Max, 20);
		clusterB = new SimulatedSpotCluster(2, 4, "m1.small", 24, 1, "us-east-1a-m1.small.out", Strategy.Max, 65);
		clusterC = new SimulatedSpotCluster(3, 5, "m1.medium", 24, 1, "us-east-1a-m1.medium.out", Strategy.Max, 130);
		*/
		clusterAD = new SimulatedSpotCluster(1, 2, "t1.microD", demand, 1, "us-east-1a-t1.micro.out", Strategy.OnDemand, 20);
		clusterBD = new SimulatedSpotCluster(2, 4, "m1.smallD", demand, 1, "us-east-1a-m1.small.out", Strategy.OnDemand, 65);
		clusterCD = new SimulatedSpotCluster(3, 5, "m1.mediumD", demand, 1, "us-east-1a-m1.medium.out", Strategy.OnDemand, 130);
		
	/*	clusterA = new SimulatedSpotCluster(1, 1, "t1.micro", 24, "us-east-1a-t1.micro.out");
		clusterB = new SimulatedSpotCluster(2, 2, "m1.small", 24, "us-east-1a-m1.small.out");
		clusterC = new SimulatedSpotCluster(3, 3, "m1.medium", 24, "us-east-1a-m1.medium.out");
	*/	
		clusters = new HashMap<String,SimulatedCluster>();
		clusters.put("t1.micro", clusterA);
		clusters.put("m1.small", clusterB);
		clusters.put("m1.medium", clusterC);
		clusters.put("t1.microD", clusterAD);
		clusters.put("m1.smallD", clusterBD);
		clusters.put("m1.mediumD", clusterCD);
		priceSim.addCluster((SimulatedSpotCluster)clusterA);
		priceSim.addCluster((SimulatedSpotCluster)clusterB);
		priceSim.addCluster((SimulatedSpotCluster)clusterC);

		priceSim.addCluster((SimulatedSpotCluster)clusterAD);
		priceSim.addCluster((SimulatedSpotCluster)clusterBD);
		priceSim.addCluster((SimulatedSpotCluster)clusterCD);
		sampleSeed = sampleSeeder.nextLong();
		bag.setClusters(clusters, sampleSeed, sampleSize);
		
		
		SampleStatsMultipleClusters bagSS = new SampleStatsMultipleClusters(bag, sampleSize, p_mu, p_sigma, p_M);
		bagSS.budgetType = budgetType;
		bagSS.selectBudget(budgetType, false);
		
//		System.out.println("Schedule based on real parameters: \t" + bagSS.scheduleRealParameters.machinesPerCluster);
//		System.out.println("Schedule based on real parameters + deltaB: \t" + 
				//	bagSS.scheduleRealParametersExtraBudget.machinesPerCluster + 
				//	"\t initialDeltaN=" + bagSS.scheduleRealParametersExtraBudget.deltaN +
//				  "\t deltaNExtraB=" + bagSS.scheduleRealParametersExtraBudget.deltaNExtraB);
		
		
		for(int j=0; j < noSampleExperiments; j++) {		
			
			//sampleSeed = sampleSeeder.nextLong();	
			
			System.out.println("----------->Sample Experiment " + j + "<-----------");
			System.out.println("Sample seed: " + sampleSeed);
					

			SpotBag tmpBag = bag.copyMC();								
			tmpBag.sampleMultipleClusters(sampleSeed, sampleSize);
			bagSS.addSampleStats(tmpBag);
			//bag.setClusters(clusters);
			
			
			long budgetSampleParameters = tmpBag.selectedBudget(budgetType);
			tmpBag.budgetType = budgetType;
					
			SimulatedSchedule simSched = tmpBag.computeMakespanEstimateMultipleClusters(budgetSampleParameters);
			System.out.println(simSched);
			
			bagSS.addScheduleStats(simSched, tmpBag);
		/*	
			SimulatedSchedule simSchedDeltaB = tmpBag.computeExtraBudgetMakespanEstimateMultipleClusters(budgetSampleParameters, 
					budgetType);
			System.out.println(simSchedDeltaB);
			/*
			//bagSS.addExtraBudgetScheduleStats(simSchedDeltaB, tmpBag);
			
			if(printSampleSummary) {
				System.out.println();
			}
			
			// set makespan in price simulator
			double makespan = tmpBag.computePIMakespan(simSched, p_mu, p_sigma, p_M).estimate;
			System.out.println("=========Makespan" + makespan);
			
			priceSim.setCoef(makespan);
			*/
		/*	
			for (int i=0; i < noExecutions; i++) {	
				
				System.out.println("---------->>Execution " + j + "." + i + "<<--------");
				
				executionSeed = executionSeeder.nextLong();
				
				
				SimulatedExecution simExec = tmpBag.executeMultipleClustersMoreDetails(executionSeed, simSched);
				bagSS.addExecutionStats(j, simExec);

				//SimulatedExecution simExecExtraB = tmpBag.executeMultipleClustersMoreDetails(executionSeed, simSchedDeltaB);
				//bagSS.addExtraBudgetExecutionStats(j, simExecExtraB);
						
			}*/
						
		}	
		
		//bagSS.printFormatted("", false);
		
	}
	
	private static void testSpotSamplePhaseMultipleClusters(String budgetType, LevySpotBag bag) {
		
		System.out.println("test");
		
		boolean printXLS = false;
		boolean printSampleSummary = false;
		boolean printNodeStats = true;
		
		
		SimulatedCluster clusterA, clusterB, clusterC, clusterAD, clusterBD, clusterCD;
		HashMap<String,SimulatedCluster> clusters;
				
		Random sampleSeeder = new Random(111111111L);
		Random executionSeeder = new Random(888888888L);
		
		long sampleSeed, executionSeed;
		
		double p_mu = 0.9, p_sigma = 0.9, p_M = 0.9;

		int sampleSize = bag.sampleSize;
								
		boolean printInfo = false;
		bag.printXLS = printXLS;
		bag.printSampleSummary = printSampleSummary;
		bag.printNodeStats = printNodeStats;
		
		//speedFactor, costFactor, name, maxNodes, startIndex, file, strategy
		//currently costFactor is not really used in the computation
		//diff from 50 to 60 for normal BKP
		clusterA = new SimulatedSpotCluster(1, 2, "t1.micro", 100, 1, "us-east-1a-t1.micro.out", Strategy.Current, 20);
		clusterB = new SimulatedSpotCluster(2, 4, "m1.small", 100, 1, "us-east-1a-m1.small.out", Strategy.Current, 65);
		clusterC = new SimulatedSpotCluster(3, 5, "m1.medium", 100, 1, "us-east-1a-m1.medium.out", Strategy.Current, 130);
		
		
		clusterAD = new SimulatedSpotCluster(1, 2, "t1.microD", 100, 1, "us-east-1a-t1.micro.out", Strategy.OnDemand, 20);
		clusterBD = new SimulatedSpotCluster(2, 4, "m1.smallD", 100, 1, "us-east-1a-m1.small.out", Strategy.OnDemand, 65);
		clusterCD = new SimulatedSpotCluster(3, 5, "m1.mediumD", 100, 1, "us-east-1a-m1.medium.out", Strategy.OnDemand, 130);
		
	
		clusters = new HashMap<String,SimulatedCluster>();
		clusters.put("t1.micro", clusterA);
		clusters.put("m1.small", clusterB);
		clusters.put("m1.medium", clusterC);
		clusters.put("t1.microD", clusterAD);
		clusters.put("m1.smallD", clusterBD);
		clusters.put("m1.mediumD", clusterCD);
		priceSim.addCluster((SimulatedSpotCluster)clusterA);
		priceSim.addCluster((SimulatedSpotCluster)clusterB);
		priceSim.addCluster((SimulatedSpotCluster)clusterC);

		priceSim.addCluster((SimulatedSpotCluster)clusterAD);
		priceSim.addCluster((SimulatedSpotCluster)clusterBD);
		priceSim.addCluster((SimulatedSpotCluster)clusterCD);
		sampleSeed = sampleSeeder.nextLong();
		bag.setClusters(clusters, sampleSeed, sampleSize);
		System.out.println("cluster set");
		
		SampleStatsMultipleClusters bagSS = new SampleStatsMultipleClusters(bag, sampleSize, p_mu, p_sigma, p_M);
		bagSS.budgetType = budgetType;
		bagSS.selectBudget(budgetType, false);
		
		System.out.println("sample stats done");
	
		
		for(int j=0; j < noSampleExperiments; j++) {		
			
			//sampleSeed = sampleSeeder.nextLong();	
			
			System.out.println("----------->Sample Experiment " + j + "<-----------");
			System.out.println("Sample seed: " + sampleSeed);
					

			LevySpotBag tmpBag = bag.copyMC();								
			tmpBag.sampleMultipleClusters(sampleSeed, sampleSize);
			bagSS.addSampleStats(tmpBag);
			//bag.setClusters(clusters);
			
			
			long budgetSampleParameters = tmpBag.selectedBudget(budgetType);
			tmpBag.budgetType = budgetType;
					
			SimulatedSchedule simSched = tmpBag.computeMakespanEstimateMultipleClusters(budgetSampleParameters);
			System.out.println(simSched);
			
			bagSS.addScheduleStats(simSched, tmpBag);
	
	
		}
		
	}
private static void testSpotSamplePhaseMultipleClustersDACH(String budgetType, DACHSpotBag bag) {
		
		System.out.println("test");
		
		boolean printXLS = false;
		boolean printSampleSummary = false;
		boolean printNodeStats = true;
		
		
		SimulatedCluster clusterA, clusterB, clusterC, clusterAD, clusterBD, clusterCD;
		HashMap<String,SimulatedCluster> clusters;
				
		Random sampleSeeder = new Random(111111111L);
		Random executionSeeder = new Random(888888888L);
		
		//Random sampleSeeder = new Random(Experiment);
		//Random executionSeeder = new Random(888888888L);
		
		long sampleSeed, executionSeed;
		
		double p_mu = 0.9, p_sigma = 0.9, p_M = 0.9;

		int sampleSize = bag.sampleSize;
								
		boolean printInfo = false;
		bag.printXLS = printXLS;
		bag.printSampleSummary = printSampleSummary;
		bag.printNodeStats = printNodeStats;
		
		//speedFactor, costFactor, name, maxNodes, startIndex, file, strategy
		//currently costFactor is not really used in the computation
		//diff from 50 to 60 for normal BKP
		clusterA = new SimulatedSpotCluster(1, 2, "t1.micro", 20, 1, "us-east-1a-t1.micro.out", Strategy.Current, 20);
		clusterB = new SimulatedSpotCluster(2, 4, "m1.small", 20, 1, "us-east-1a-m1.small.out", Strategy.Current, 65);
		clusterC = new SimulatedSpotCluster(3, 5, "m1.medium", 20, 1, "us-east-1a-m1.medium.out", Strategy.Current, 130);
		
		clusterAD = new SimulatedSpotCluster(1, 2, "t1.microD", 20, 1, "us-east-1a-t1.micro.out", Strategy.OnDemand, 20);
		clusterBD = new SimulatedSpotCluster(2, 4, "m1.smallD", 20, 1, "us-east-1a-m1.small.out", Strategy.OnDemand, 65);
		clusterCD = new SimulatedSpotCluster(3, 5, "m1.mediumD", 20, 1, "us-east-1a-m1.medium.out", Strategy.OnDemand, 130);
		
	
		clusters = new HashMap<String,SimulatedCluster>();
		clusters.put("t1.micro", clusterA);
		clusters.put("m1.small", clusterB);
		clusters.put("m1.medium", clusterC);
		clusters.put("t1.microD", clusterAD);
		clusters.put("m1.smallD", clusterBD);
		clusters.put("m1.mediumD", clusterCD);
		priceSim.addCluster((SimulatedSpotCluster)clusterA);
		priceSim.addCluster((SimulatedSpotCluster)clusterB);
		priceSim.addCluster((SimulatedSpotCluster)clusterC);

		priceSim.addCluster((SimulatedSpotCluster)clusterAD);
		priceSim.addCluster((SimulatedSpotCluster)clusterBD);
		priceSim.addCluster((SimulatedSpotCluster)clusterCD);
		sampleSeed = sampleSeeder.nextLong();
		bag.setClusters(clusters, sampleSeed, sampleSize);
		System.out.println("cluster set");
		
		SampleStatsMultipleClusters bagSS = new SampleStatsMultipleClusters(bag, sampleSize, p_mu, p_sigma, p_M);
		bagSS.budgetType = budgetType;
		bagSS.selectBudget(budgetType, false);
		
		System.out.println("sample stats done");
	
		
		for(int j=0; j < noSampleExperiments; j++) {		
			
			//sampleSeed = sampleSeeder.nextLong();	
			
			System.out.println("----------->Sample Experiment " + j + "<-----------");
			System.out.println("Sample seed: " + sampleSeed);
					

			DACHSpotBag tmpBag = bag.copyMC();								
			tmpBag.sampleMultipleClusters(sampleSeed, sampleSize);
			bagSS.addSampleStats(tmpBag);
			//bag.setClusters(clusters);
			
			
			long budgetSampleParameters = tmpBag.selectedBudget(budgetType);
			tmpBag.budgetType = budgetType;
					
			SimulatedSchedule simSched = tmpBag.computeMakespanEstimateMultipleClusters(budgetSampleParameters);
			System.out.println(simSched);
			
			bagSS.addScheduleStats(simSched, tmpBag);
	
	
		}
		
	}
}
