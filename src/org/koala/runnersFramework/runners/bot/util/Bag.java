package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Random;

import org.koala.runnersFramework.runners.bot.Cluster;
import org.koala.runnersFramework.runners.bot.Item;
//import org.koala.runnersFramework.runners.bot.Knapsack;


public abstract class Bag {

	long seed;
	int size;
	ArrayList<Double> x;
	double speedFactor;
	int costFactor;
	int cost;
	int maxItems;

	HashMap<String, SimulatedCluster> clusters;
	
	boolean printXLS = true;
	boolean printNodeStats = true;
	boolean printDebug = false;
	
	/*expressed in seconds*/
	double meanX, varXsq, stDevX;
	double estimateXmax;
	double meanXExecution, varXsqExecution, stDevXExecution;
	double estimateXmaxExecution;
	public double realExpectation;
	public double realVariance;
	public double realStDev;
	
	public boolean printSampleSummary;
	public int overMnodes;
	public int sampleSize;
	public boolean emergencyOnly;
	public boolean printTailMap = false; 
	
	ArrayList<Double> sampledTasks;
	
	TreeMap<Integer,Occurrence> histogram;
	public boolean useHisto;
	
	public int knowledge;
	static public int ESTIMATES = 1;
	static public int PERFECT = 2;
	static public int RANDOM = 3;
	
	public Random candidateSelector;
	private boolean allowMultipleReplicas = true;
	private boolean allowReMigration = true;
	
	public String budgetType;
	private String minCostName;
	private String maxCostName;
	
	public Bag copyMC() {
		Bag tmp = copy();
		tmp.clusters = (HashMap<String, SimulatedCluster>) clusters.clone();
		return tmp;
	}
	
	public abstract Bag copy() ;

	public void setClusters(HashMap<String, SimulatedCluster> clusters, long sampleSeed, int sampleSize) {
		this.clusters = clusters;
		for(SimulatedCluster simCluster : clusters.values()) {
			simCluster.cost = simCluster.costFactor*cost;
		}
	}
	
	public double getExpectation() {
		
		double e = 0.0;
		for(Double j : x) {
			e += j.doubleValue();
		}
		
		realExpectation = e/x.size();
		
		if(printSampleSummary) {
			System.out.print(realExpectation/60 + "\t");
		}
		
		return realExpectation;
	}
	
	public double getVariance() {
		double var = 0.0;
		for(Double j : x) {
			var += Math.pow(j.doubleValue()-realExpectation, 2);
		}
		realVariance = var/x.size();
		
		if(printSampleSummary) {
			System.out.print(Math.sqrt(realVariance)/60 + "\t");
		}
		
		realStDev = Math.sqrt(realVariance);	
		
		return realVariance; 
	}
	
	public void sample(long sampleSeed, int sampleSize) {
		this.sampleSize = sampleSize;
		sampledTasks =  new ArrayList<Double> ();		
		Random randomSample = new Random(sampleSeed);
		for(int i = 0; i < sampleSize; i++) {
			Double rt = x.remove(randomSample.nextInt(x.size()));
			sampledTasks.add(rt);			
		}

		double sumX = 0.0;
		double sumvarsq = 0.0;
		estimateXmax = Double.NEGATIVE_INFINITY;
		
		for (Double j : sampledTasks) {
			sumX += j.doubleValue();
			if(j.doubleValue() > estimateXmax) {
				estimateXmax = j.doubleValue();
			}
		}
		
		meanX = sumX / sampledTasks.size();

		for (Double j : sampledTasks) {
			sumvarsq += Math.pow(j.doubleValue()-meanX, 2);
		}	
		
		varXsq = sumvarsq / sampledTasks.size();

		stDevX = Math.sqrt(varXsq);
		
		meanXExecution = meanX;
		estimateXmaxExecution = estimateXmax;
		varXsqExecution = varXsq;
		stDevXExecution = stDevX;
		
		if(printXLS || printSampleSummary)
			System.out.print(meanX/60 + "\t"+Math.sqrt(varXsq)/60 + "\t");
		
		size = x.size();
	}

	public void sampleMultipleClusters(long sampleSeed, int sampleSize) {
		sample(sampleSeed,sampleSize);
		for(SimulatedCluster simCluster : clusters.values()) {
			simCluster.Ti = meanX/60/simCluster.speedFactor;
			simCluster.stdev = Math.sqrt(varXsq)/60/simCluster.speedFactor;		
		}
	}

	public ConfidenceInterval computeCIMean(double p_mu) {
		ConfidenceInterval ci = new ConfidenceInterval();
		ci.estimate = meanXExecution;
		ci.confidenceLevel = p_mu;
		
		double tnminus1 = umontreal.iro.lecuyer.probdist.StudentDist.inverseF(sampleSize-1, (1-p_mu)/2);
		double student = Math.abs(tnminus1/Math.sqrt(sampleSize));		
		ci.lowerBound = ci.estimate - student*stDevXExecution;
		ci.upperBound = ci.estimate + student*stDevXExecution;
		return ci;
	}

	
	public ConfidenceInterval computeCIStdev(double p_sigma) {
		ConfidenceInterval ci = new ConfidenceInterval();
		ci.estimate = stDevXExecution;
		ci.confidenceLevel = p_sigma;
		
		double chisLow = flanagan.analysis.Stat.chiSquareInverseCDF(sampleSize-1, (1+p_sigma)/2);
		double chisHigh = flanagan.analysis.Stat.chiSquareInverseCDF(sampleSize-1, (1-p_sigma)/2);
		double nchiLow = Math.sqrt((sampleSize-1)/chisLow);
		double nchiHigh = Math.sqrt((sampleSize-1)/chisHigh);
						
		ci.lowerBound = nchiLow*ci.estimate;
		ci.upperBound = nchiHigh*ci.estimate;
		return ci;
	}
	
	
	private SimulatedCluster selectMostProfitableRealParameters() {
		// TODO Auto-generated method stub
		SimulatedCluster mostProfitable = null;
		SimulatedCluster cheapest = findCheapestRealParameters();
		double profitMax = Double.MIN_VALUE;
		for(SimulatedCluster simCluster : clusters.values()) {
			simCluster.computeProfitabilityRealParameters(cheapest, realExpectation);
			if(simCluster.profitabilityRealParameters > profitMax) {
				profitMax = simCluster.profitabilityRealParameters;
				mostProfitable = simCluster;
			}
		}
		System.out.println("Most profitable cluster (based on real parameters) is: " + mostProfitable.name);
		return mostProfitable;
	} 

	private SimulatedCluster findCheapestRealParameters() {
		SimulatedCluster cheapestCluster = null;
		double cheapest = Double.MAX_VALUE;
		for (SimulatedCluster simCluster : clusters.values()) {
			if(simCluster.cost < cheapest) {
				cheapest = simCluster.cost;
				cheapestCluster = simCluster;
			} else if (simCluster.cost == cheapest) {
				if((realExpectation/simCluster.speedFactor) < (realExpectation/cheapestCluster.speedFactor)) {
					cheapestCluster = simCluster;
				}
			}
		}
		
		System.out.println("Cheapest cluster (based on real parameters) is: " + cheapestCluster.name);
		return cheapestCluster;
	}

	
	public long getMinimumMakespanBudgetRealParametersMultipleClusters() {		
		
		double maxSpeed = 0.0;
		long costMaxSpeed = 0;

		for(SimulatedCluster simCluster : clusters.values()) {		
			costMaxSpeed += simCluster.maxNodes * simCluster.cost;
			maxSpeed += (double) (simCluster.maxNodes / (realExpectation/60/simCluster.speedFactor));
		}
		
		double makespanMin = Math.ceil(((size-sampleSize)/maxSpeed)/60);
		long BmakespanMin = (long) makespanMin * costMaxSpeed;
				
		return BmakespanMin;
	}
	
	public long getMinimumBudgetRealParametersMultipleClusters() {
		SimulatedCluster mostProfitable = selectMostProfitableRealParameters();
		double makespanBmin = Math.ceil((size-sampleSize)*realExpectation/mostProfitable.speedFactor/3600);
		double Bmin = makespanBmin*mostProfitable.cost;

		return (long)Math.ceil(Bmin);
		
	}
	
	public SimulatedSchedule computeMakespanEstimateRealParametersMultipleClusters(long budget) {

		ArrayList<Item> items = new ArrayList<Item>();
		int minCost = Integer.MAX_VALUE, maxCost = 0;
		items.add(new Item(0,0,""));
		for(SimulatedCluster simCluster : clusters.values()) {
			if(minCost > simCluster.cost) {
				minCost = simCluster.cost;
				minCostName = simCluster.name;
			}
			for(int i=0; i<simCluster.maxNodes; i++) {				
				items.add(new Item(1/(realExpectation/60/simCluster.speedFactor),
						simCluster.cost,
						simCluster.name));
			}
			maxCost += simCluster.maxNodes*simCluster.cost;
		}
				
		Item[] machines = items.toArray(new Item[0]);
		GeneticAlgorithm offlineMoo = new GeneticAlgorithm(machines,budget,size-sampleSize,minCost,maxCost,60,maxItems);
		offlineMoo.printConfig = false;
		HashMap<String, Integer> offlineSol = offlineMoo.findSol();
		SimulatedSchedule sched = new SimulatedSchedule(budget, offlineMoo.costPlan, offlineMoo.noATUPlan, offlineSol);		
		return sched;
	}
	
	public SimulatedSchedule computeExtraBudgetMakespanEstimateRealParametersMultipleClusters(
			long budgetRealParameters, String budgetType) {
			
		ArrayList<Item> items = new ArrayList<Item>();
		int minCost = Integer.MAX_VALUE, maxCost = 0;
		items.add(new Item(0,0,""));
		for(SimulatedCluster simCluster : clusters.values()) {
			if(minCost > simCluster.cost) {
				minCost = simCluster.cost;
				minCostName = simCluster.name;
			}
			for(int i=0; i<simCluster.maxNodes; i++) {				
				items.add(new Item(1/(realExpectation/60/simCluster.speedFactor),
						simCluster.cost,
						simCluster.name));
			}
			maxCost += simCluster.maxNodes*simCluster.cost;
		}				
		Item[] machines = items.toArray(new Item[0]);
		
		GeneticAlgorithm offlineMoo = new GeneticAlgorithm(machines,budgetRealParameters,size-sampleSize,minCost,maxCost,60, maxItems);
		offlineMoo.printConfig = false;
		HashMap<String, Integer> offlineSol = offlineMoo.findSol();
		SimulatedSchedule initSched = new SimulatedSchedule(budgetRealParameters, offlineMoo.costPlan, offlineMoo.noATUPlan, offlineSol);	
		long deltaB = 0;
		int aini = 0;
		for (String cluster : initSched.machinesPerCluster.keySet()) {			 
				aini += initSched.machinesPerCluster.get(cluster).intValue()* 
							Math.floor(initSched.atus*60/(realExpectation/60/clusters.get(cluster).speedFactor));
		}
		int deltaN = size - sampleSize - aini;
		SimulatedSchedule modifiedSched = null;
		
		/*for the minimal makespan budget we learn how much money needed to
		 * execute the remaining \Delta_N tasks and add it to schedule.budget
		 * for the other types of budget we compute a new configuration*/
		if(budgetType.equals("BmakespanMin")) {
			int leftDeltaN = deltaN;
			if(deltaN > 0) {
				System.out.println("deltaN=" + deltaN);
				ArrayList<SimulatedCluster> orderedByJobsPerATU = new ArrayList<SimulatedCluster>(clusters.values());
				Collections.sort(orderedByJobsPerATU, new Comparator<SimulatedCluster>(){
					public int compare(SimulatedCluster a, SimulatedCluster b) {
						return (int)Math.floor(60/(realExpectation/60/a.speedFactor)) 
									- (int)Math.floor(60/(realExpectation/60/b.speedFactor));
					}
				});							
				for(int i=orderedByJobsPerATU.size()-1; i>=0; i--) {
					if(leftDeltaN > orderedByJobsPerATU.get(i).maxNodes) {
						deltaB += (long) orderedByJobsPerATU.get(i).maxNodes*orderedByJobsPerATU.get(i).cost;
						leftDeltaN -= orderedByJobsPerATU.get(i).maxNodes;
					} else {
						deltaB += (long) leftDeltaN*orderedByJobsPerATU.get(i).cost;
						leftDeltaN=0;
						break;
					}
				}			
			}		
			modifiedSched = new SimulatedSchedule(budgetRealParameters+deltaB, offlineMoo.costPlan, offlineMoo.noATUPlan, offlineSol);
			modifiedSched.deltaN = deltaN;
			modifiedSched.deltaNExtraB = leftDeltaN;
		} else {
			long Bthreshold = (long)Math.ceil(1.1*budgetRealParameters);
			int modifiedDeltaN = deltaN;			
			long BPlusDeltaB = 0;
			int x = 0;
			while (modifiedDeltaN > 0) {
				System.out.println("deltaN=" + modifiedDeltaN);
				x ++;
				BPlusDeltaB = (long)Math.ceil((1+(double)x/100)*budgetRealParameters);
				
				if(BPlusDeltaB > Bthreshold) {
					break;
				}
				
				offlineMoo = new GeneticAlgorithm(machines,BPlusDeltaB,size-sampleSize,minCost,maxCost,60,maxItems);
				offlineMoo.printConfig = false;
				offlineSol = offlineMoo.findSol();
				modifiedSched = new SimulatedSchedule(BPlusDeltaB, offlineMoo.costPlan, offlineMoo.noATUPlan, offlineSol);				
				aini = 0;				
				for (String cluster : modifiedSched.machinesPerCluster.keySet()) {			 
						aini += modifiedSched.machinesPerCluster.get(cluster).intValue()* 
									Math.floor(modifiedSched.atus*60/(realExpectation/60/clusters.get(cluster).speedFactor));
				}
				modifiedDeltaN = size - sampleSize - aini;
				deltaB = BPlusDeltaB - budgetRealParameters;
			}
			modifiedSched = new SimulatedSchedule(budgetRealParameters+deltaB, offlineMoo.costPlan, offlineMoo.noATUPlan, offlineSol);
			modifiedSched.deltaN = deltaN;
			modifiedSched.deltaNExtraB = modifiedDeltaN;
		}
			
		return modifiedSched;
	}

	
	public SimulatedSchedule computeMakespanEstimateMultipleClusters(long budget) {
		ArrayList<Item> items = new ArrayList<Item>();
		int minCost = Integer.MAX_VALUE, maxCost = 0;
		items.add(new Item(0,0,""));
		for(SimulatedCluster simCluster : clusters.values()) {
			if(minCost > simCluster.getCost()) {
				minCost = simCluster.getCost();
				minCostName = simCluster.name;
			}
			for(int i=0; i<simCluster.maxNodes; i++) {
				items.add(new Item(1/simCluster.Ti,
						simCluster.getCost(),
						simCluster.name));
			}
			maxCost += simCluster.maxNodes*simCluster.getCost();
		}
				
		Item[] machines = items.toArray(new Item[0]);
		GeneticAlgorithm offlineMoo = new GeneticAlgorithm(machines, budget, size, minCost, maxCost, 60, maxItems);
		offlineMoo.printConfig = false;
		HashMap<String, Integer> offlineSol = offlineMoo.findSol();		
		SimulatedSchedule sched = new SimulatedSchedule(budget, offlineMoo.costPlan, offlineMoo.noATUPlan, offlineSol);		
		return sched;
	}
	
	public SimulatedSchedule computeExtraBudgetMakespanEstimateMultipleClusters(			
			long budgetSampleParameters, String budgetType) {
		
		ArrayList<Item> items = new ArrayList<Item>();
		int minCost = Integer.MAX_VALUE, maxCost = 0;
		items.add(new Item(0,0,""));
		for(SimulatedCluster simCluster : clusters.values()) {
			if(minCost > simCluster.cost) {
				minCost = simCluster.cost;
				minCostName = simCluster.name;
			}
			for(int i=0; i<simCluster.maxNodes; i++) {				
				items.add(new Item(1/simCluster.Ti,
						simCluster.cost,
						simCluster.name));
			}
			maxCost += simCluster.maxNodes*simCluster.cost;
		}				
		Item[] machines = items.toArray(new Item[0]);
		
		GeneticAlgorithm offlineMoo = new GeneticAlgorithm(machines,budgetSampleParameters,size,minCost,maxCost,60, maxItems);
		offlineMoo.printConfig = false;
		HashMap<String, Integer> offlineSol = offlineMoo.findSol();
		SimulatedSchedule initSched = new SimulatedSchedule(budgetSampleParameters, offlineMoo.costPlan, offlineMoo.noATUPlan, offlineSol);	
		long deltaB = 0;
		int aini = 0;
		for (String cluster : initSched.machinesPerCluster.keySet()) {			 
				aini += initSched.machinesPerCluster.get(cluster).intValue()* 
							Math.floor(initSched.atus*60/clusters.get(cluster).Ti);
		}
		int deltaN = size - aini;
		SimulatedSchedule modifiedSched = null;
		
		/*for the minimal makespan budget we learn how much money needed to
		 * execute the remaining \Delta_N tasks and add it to schedule.budget
		 * for the other types of budget we compute a new configuration*/
		if(budgetType.equals("BmakespanMin")) {
			int leftDeltaN = deltaN;
			System.out.println("initialDeltaN=" + deltaN);
			if(deltaN > 0) {				
				ArrayList<SimulatedCluster> orderedByJobsPerATU = new ArrayList<SimulatedCluster>(clusters.values());
				Collections.sort(orderedByJobsPerATU, new Comparator<SimulatedCluster>(){
					public int compare(SimulatedCluster a, SimulatedCluster b) {
						return (int)Math.floor(60/a.Ti) - (int)Math.floor(60/b.Ti);
					}
				});							
				for(int i=orderedByJobsPerATU.size()-1; i>=0; i--) {
					if(leftDeltaN > orderedByJobsPerATU.get(i).maxNodes) {
						deltaB += (long) orderedByJobsPerATU.get(i).maxNodes*orderedByJobsPerATU.get(i).cost;
						leftDeltaN -= orderedByJobsPerATU.get(i).maxNodes;
					} else {
						deltaB += (long) leftDeltaN*orderedByJobsPerATU.get(i).cost;
						leftDeltaN=0;
						break;
					}
				}			
			}		
			System.out.println("extraBDeltaN=" + leftDeltaN);
			modifiedSched = new SimulatedSchedule(budgetSampleParameters+deltaB, offlineMoo.costPlan, offlineMoo.noATUPlan, offlineSol);
			modifiedSched.deltaN = deltaN;
			modifiedSched.deltaNExtraB = leftDeltaN;
		} else {
			long Bthreshold = (long)Math.ceil(1.1*budgetSampleParameters);
			int modifiedDeltaN = deltaN;			
			long BPlusDeltaB = 0;
			int x = 0;
			System.out.println("initialDeltaN=" + modifiedDeltaN);
			
			while (modifiedDeltaN > 0) {				
				x ++;
				BPlusDeltaB = (long)Math.ceil((1+(double)x/100)*budgetSampleParameters);
				
				if(BPlusDeltaB > Bthreshold) {
					break;
				}
				
				offlineMoo = new GeneticAlgorithm(machines,BPlusDeltaB,size,minCost,maxCost,60,maxItems);
				offlineMoo.printConfig = false;
				offlineSol = offlineMoo.findSol();
				modifiedSched = new SimulatedSchedule(BPlusDeltaB, offlineMoo.costPlan, offlineMoo.noATUPlan, offlineSol);
				System.out.println("intermediate extra budget schedule: " + modifiedSched);
				aini = 0;				
				for (String cluster : modifiedSched.machinesPerCluster.keySet()) {			 
						aini += modifiedSched.machinesPerCluster.get(cluster).intValue()* 
									Math.floor(modifiedSched.atus*60/clusters.get(cluster).Ti);
				}
				modifiedDeltaN = size - aini;
				deltaB = BPlusDeltaB - budgetSampleParameters;
			}
			System.out.println("extraBDeltaN=" + modifiedDeltaN);
			modifiedSched = new SimulatedSchedule(budgetSampleParameters+deltaB, offlineMoo.costPlan, offlineMoo.noATUPlan, offlineSol);
			System.out.println("extra budget schedule: " + modifiedSched);
			modifiedSched.deltaN = deltaN;
			modifiedSched.deltaNExtraB = modifiedDeltaN;
		}
			
		return modifiedSched;
	}
	
	public PredictionInterval computePIMakespan(SimulatedSchedule sched, double p_mu, double p_sigma, double p_M) {
		
		PredictionInterval pi = new PredictionInterval();
		pi.confidenceLevel = p_M + p_sigma + p_mu - 2;
		
		double gamma = 0.0, inv_gamma_est_max = 0.0, inv_gamma_est_min = 0.0;
		double S1 = 0, S2 = 0, S3 = 0;
				
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			SimulatedCluster simCluster = clusters.get(clusterName);
			int noMachines = sched.machinesPerCluster.get(clusterName).intValue();
			ConfidenceInterval ciMu = simCluster.computeCIMean(p_mu, sampleSize); 
			ConfidenceInterval ciStdev = simCluster.computeCIStdev(p_sigma, sampleSize);
			
			gamma += noMachines/ciMu.estimate;	
			inv_gamma_est_max += noMachines/ciMu.upperBound;
			inv_gamma_est_min += noMachines/ciMu.lowerBound;		
						
			S1 += noMachines*ciMu.upperBound;
			S2 += noMachines*ciStdev.upperBound/ciMu.lowerBound;
			S3 += noMachines;
		}
			
		double ti_agg_est_max = 1/inv_gamma_est_max;
		double ti_agg_est = 1/gamma;
		
		double stDev_agg_est_max = Math.sqrt( S1/inv_gamma_est_max 
										+ S2/inv_gamma_est_max 
										- S3*S3/inv_gamma_est_min/inv_gamma_est_min)
										/S3;

		if(printXLS)
			System.out.print(ti_agg_est_max + "\t" + stDev_agg_est_max + "\t");
		
		double z_alpha = StatUtil.getInvCDF(p_M, true);
		
		pi.estimate = size*ti_agg_est;
		
		pi.upperBound = size*ti_agg_est_max + Math.sqrt(size)*z_alpha*stDev_agg_est_max;
		
		long budgetMax = 0;
		int nATUsMax = (int) Math.ceil(pi.upperBound/60);
		
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			SimulatedCluster simCluster = clusters.get(clusterName);
			int noMachines = sched.machinesPerCluster.get(clusterName).intValue();
			budgetMax += noMachines*simCluster.cost*nATUsMax;
		}
		
		sched.budgetMax = budgetMax;
		return pi;
		
	}
	
	public long selectedBudget(String budgetType) {
		long budget = 0;		
		if(budgetType.equals("Bmin")) {
			budget = getMinimumBudgetMultipleClusters();
		} else if(budgetType.equals("BminPlus10")) {
			budget = (long)Math.ceil(1.1*getMinimumBudgetMultipleClusters());
		} else if(budgetType.equals("BminPlus20")) {
			budget = (long)Math.ceil(1.2*getMinimumBudgetMultipleClusters());
		} else if(budgetType.equals("BmakespanMin")) {
			//method called for Spot Instance version
			budget = getMinimumMakespanBudgetMultipleClusters();
		} else if(budgetType.equals("BmakespanMinMinus20")) {
			budget = (long)Math.ceil(0.8*getMinimumMakespanBudgetMultipleClusters());
		} else if(budgetType.equals("BmakespanMinMinus10")) {
			budget = (long)Math.ceil(0.9*getMinimumMakespanBudgetMultipleClusters());
		} 
		return budget;
	}
	
	public long selectedBudgetRealParameters(String budgetType) {
		long budget = 0;		
		if(budgetType.equals("Bmin")) {
			budget = getMinimumBudgetRealParametersMultipleClusters();
		} else if(budgetType.equals("BminPlus10")) {
			budget = (long)Math.ceil(1.1*getMinimumBudgetRealParametersMultipleClusters());
		} else if(budgetType.equals("BminPlus20")) {
			budget = (long)Math.ceil(1.2*getMinimumBudgetRealParametersMultipleClusters());
		} else if(budgetType.equals("BmakespanMin")) {
			budget = getMinimumMakespanBudgetRealParametersMultipleClusters();
		} else if(budgetType.equals("BmakespanMinMinus20")) {
			budget = (long)Math.ceil(0.8*getMinimumMakespanBudgetRealParametersMultipleClusters());
		} else if(budgetType.equals("BmakespanMinMinus10")) {
			budget = (long)Math.ceil(0.9*getMinimumMakespanBudgetRealParametersMultipleClusters());
		} 
		return budget;
	}
	
	/*to be implemented for Chapter 4 tests
	 * it refers to the estimated makespan
	 * based on "perfect" information about expectation and variance
	 * though using the configuration computed using the 
	 * expectation and variance values obtained through sampling*/
	private void computeMakespanRealMultipleClusters() {
		/*double realTi1 = realExpectation/60; 
		double realStDev1 = Math.sqrt(realVariance)/60;
		
		double realTi2 = realTi1/beta;
		double realStDev2 = realStDev1/beta;
		double gamma_real = (a/realTi1 + b/realTi2);
		double ti_agg_real = 1/gamma_real;		
		double stDev_agg_real = Math.sqrt((a*realTi1+b*realTi2)/gamma_real 
				+ (a*realStDev1*realStDev1/realTi1+b*realStDev2*realStDev2/realTi2)/gamma_real -
				(a+b)*(a+b)/gamma_real/gamma_real)/(a+b); 
					
		if(printXLS)
			System.out.print(ti_agg_real + "\t" + stDev_agg_real + "\t");
		
		
		double realMakespanWithCi = size*ti_agg_real + Math.sqrt(size)*z_alpha*stDev_agg_real;
		*/
		}
	
	private double[] computeAggEst(SimulatedSchedule sched, double p_mu, double p_sigma, double p_m) {
		
		double gamma = 0, inv_gamma_est_max = 0, inv_gamma_est_min = 0; 
				
		double p_mu_1sided = 0.5+p_mu/2;
		
		double tnminus1 = umontreal.iro.lecuyer.probdist.StudentDist.inverseF(sampleSize-1, p_mu_1sided);
		double student = tnminus1/Math.sqrt(sampleSize);
		double chis = flanagan.analysis.Stat.chiSquareInverseCDF(sampleSize-1, p_sigma);
		double nchi = Math.sqrt((sampleSize-1)/chis);
		double z_alpha = StatUtil.getInvCDF(p_m, true);
		
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			SimulatedCluster simCluster = clusters.get(clusterName);
			int noMachines = sched.machinesPerCluster.get(clusterName).intValue();
			gamma += noMachines/simCluster.Ti;	
			double ti_est_min = simCluster.Ti - student*simCluster.stdev;
			double ti_est_max = simCluster.Ti + student*simCluster.stdev;
			
			inv_gamma_est_max += noMachines/ti_est_max;
			inv_gamma_est_min += noMachines/ti_est_min;
			
		}
		
		double ti_agg_est = 1/inv_gamma_est_max;
		
		double S1 = 0, S2 = 0, S3 = 0;
		
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			SimulatedCluster simCluster = clusters.get(clusterName);
			int noMachines = sched.machinesPerCluster.get(clusterName).intValue();
			double ti_est_min = simCluster.Ti - student*simCluster.stdev;
			double ti_est_max = simCluster.Ti + student*simCluster.stdev;
			double stDev_est_max = nchi*simCluster.stdev;
			
			S1 += noMachines*ti_est_max;
			S2 += noMachines*stDev_est_max/ti_est_min;
			S3 += noMachines;
		}		
		
		double stDev_agg_est = Math.sqrt( S1/inv_gamma_est_max 
										+ S2/inv_gamma_est_max 
										- S3*S3/inv_gamma_est_min/inv_gamma_est_min)
									/S3;

		if(printXLS)
			System.out.print(ti_agg_est + "\t" + stDev_agg_est + "\t");
		
		double makespanWithCI = size*ti_agg_est + Math.sqrt(size)*z_alpha*stDev_agg_est; 
		
		return new double[] {ti_agg_est, stDev_agg_est, makespanWithCI};
	}
		
	
	public ArrayList<Integer> computeMakespanWithCI(long budget, 
			double p_mu, double p_sigma, double p_m) {

		Item[] machines = new Item[65];
		machines[0]=null;

		int alpha = costFactor;
		
		int c1 = cost;
		double beta = speedFactor;
		
		double ti1 = meanX/60; 
		double stDev1 = Math.sqrt(varXsq)/60;
		
		int c2 = alpha*c1;
		
		double ti2 = ti1/beta;
		double stDev2 = stDev1/beta;

		for(int i=1;i<33;i++) {
			machines[i] = new Item(1/ti1,c1,"1");						
		} 
		for(int i=33;i<65;i++) {
			machines[i] = new Item(1/ti2,c2,"2");			
		}

		GeneticAlgorithm offlineMoo = new GeneticAlgorithm(machines,budget,size,3,32*c1+32*c2,60,maxItems);
		offlineMoo.printConfig = false;
		HashMap<String, Integer> offlineSol = offlineMoo.findSol();
		
		int a = offlineSol.get("1")!=null ? offlineSol.get("1").intValue() : 0;
		int b = offlineSol.get("2")!=null ? offlineSol.get("2").intValue() : 0;
				
		ArrayList<Integer> config = new ArrayList<Integer>();
		config.add(offlineMoo.noATUPlan); config.add(a); config.add(b);
		
		if(printXLS)
			System.out.print(budget+"\t" + offlineMoo.noATUPlan+"\t" + offlineMoo.costPlan + "\t" +
				+ a + "\t" + b + "\t");
				
		double gamma = (a/ti1 + b/ti2);
		
		double p_mu_1sided = 0.5+p_mu/2;
		
		double tnminus1 = umontreal.iro.lecuyer.probdist.StudentDist.inverseF(sampleSize-1, p_mu_1sided);
		double student = tnminus1/Math.sqrt(sampleSize);
		
		double chis = flanagan.analysis.Stat.chiSquareInverseCDF(sampleSize-1, p_sigma);
		double nchi = Math.sqrt((sampleSize-1)/chis);
		double z_alpha = StatUtil.getInvCDF(p_m, true);
		
		double ti1_est_min = ti1 - student*stDev1;
		double ti2_est_min = ti2 - student*stDev2;
		
		double ti1_est_max = ti1 + student*stDev1;
		double ti2_est_max = ti2 + student*stDev2;
		
		double stDev1_est_max = nchi*stDev1;
		double stDev2_est_max = nchi*stDev2;
		
		double gamma_est_max = (a/ti1_est_max+b/ti2_est_max);
		double gamma_est_min = (a/ti1_est_min+b/ti2_est_min);
		double ti_agg_est = 1/gamma_est_max;
		double stDev_agg_est = Math.sqrt((a*ti1_est_max+b*ti2_est_max)/gamma_est_max 
		+ (a*stDev1_est_max*stDev1_est_max/ti1_est_min+b*stDev2_est_max*stDev2_est_max/ti2_est_min)/gamma_est_max 
		- (a+b)*(a+b)/gamma_est_min/gamma_est_min)/(a+b);
		
		/*double ti_agg_est = (a*ti1 + b*ti2)/(a+b)/(a+b);
		double stDev_agg_est = (a*stDev1+b*stDev2)/(a+b)/(a+b);
		*/
		if(printXLS)
			System.out.print(ti_agg_est + "\t" + stDev_agg_est + "\t");
		
		
		double makespanWithCI = size*ti_agg_est + Math.sqrt(size)*z_alpha*stDev_agg_est; 
								 // + size*student*stDev_agg_est 
								  
		
		double realTi1 = realExpectation/60; 
		double realStDev1 = Math.sqrt(realVariance)/60;
		
		double realTi2 = realTi1/beta;
		double realStDev2 = realStDev1/beta;
		double gamma_real = (a/realTi1 + b/realTi2);
		double ti_agg_real = 1/gamma_real;		
		double stDev_agg_real = Math.sqrt((a*realTi1+b*realTi2)/gamma_real 
				+ (a*realStDev1*realStDev1/realTi1+b*realStDev2*realStDev2/realTi2)/gamma_real -
				(a+b)*(a+b)/gamma_real/gamma_real)/(a+b); 
					
		if(printXLS)
			System.out.print(ti_agg_real + "\t" + stDev_agg_real + "\t");
		
		
		double realMakespanWithCi = size*ti_agg_real + Math.sqrt(size)*z_alpha*stDev_agg_real;
		
		if(printXLS)
			System.out.print(p_mu_1sided +
					  "\t" + tnminus1 + 
					  "\t" + chis + 
					  "\t" + z_alpha + 
					  "\t" + (size/gamma) +
					  "\t" + makespanWithCI + 
					  "\t" + realMakespanWithCi);
						
		if(makespanWithCI >  offlineMoo.noATUPlan * 60)
			config.add(1);
		else
			config.add(0);
	
		return config;
	}

	public ArrayList<Integer> computeRealMakespan(long budget) {

		Item[] machines = new Item[65];
		machines[0]=null;

		int alpha = costFactor;
		
		int c1 = cost;
		double beta = speedFactor;
		
		double ti1 = realExpectation/60; 
				
		int c2 = alpha*c1;
		double ti2 = ti1/beta;		

		for(int i=1;i<33;i++) {
			machines[i] = new Item(1/ti1,c1,"1");						
		} 
		for(int i=33;i<65;i++) {
			machines[i] = new Item(1/ti2,c2,"2");			
		}

		GeneticAlgorithm offlineLTMoo = new GeneticAlgorithm(machines,budget,size,3,32*c1+32*c2,60,maxItems);
		offlineLTMoo.printConfig = false;
		HashMap<String, Integer> offlineLTSol = offlineLTMoo.findSol();
		
		int a = offlineLTSol.get("1")!=null ? offlineLTSol.get("1").intValue() : 0;
		int b = offlineLTSol.get("2")!=null ? offlineLTSol.get("2").intValue() : 0;
				
		ArrayList<Integer> config = new ArrayList<Integer>();
		config.add(offlineLTMoo.noATUPlan); config.add((int)offlineLTMoo.costPlan);
		config.add(a); config.add(b);
		
		if(printXLS)
			System.out.print(budget+"\t" + offlineLTMoo.noATUPlan+"\t" + offlineLTMoo.costPlan + "\t" +
				+ a + "\t" + b + "\t");
		
		return config;
	}
	
	
	public ArrayList<Integer> computeMakespan(long budget) {

		Item[] machines = new Item[65];
		machines[0]=null;

		int alpha = costFactor;
		
		int c1 = cost;
		double beta = speedFactor;
		
		double ti1 = meanX/60; 
				
		int c2 = alpha*c1;
		double ti2 = ti1/beta;		

		for(int i=1;i<33;i++) {
			machines[i] = new Item(1/ti1,c1,"1");						
		} 
		for(int i=33;i<65;i++) {
			machines[i] = new Item(1/ti2,c2,"2");			
		}

		GeneticAlgorithm offlineLTMoo = new GeneticAlgorithm(machines,budget,size,3,32*c1+32*c2,60,maxItems);
		offlineLTMoo.printConfig = false;
		HashMap<String, Integer> offlineLTSol = offlineLTMoo.findSol();
		
		int a = offlineLTSol.get("1")!=null ? offlineLTSol.get("1").intValue() : 0;
		int b = offlineLTSol.get("2")!=null ? offlineLTSol.get("2").intValue() : 0;
				
		ArrayList<Integer> config = new ArrayList<Integer>();
		config.add(offlineLTMoo.noATUPlan); config.add((int)offlineLTMoo.costPlan);
		config.add(a); config.add(b);
		
		if(printXLS)
			System.out.print(budget+"\t" + offlineLTMoo.noATUPlan+"\t" + offlineLTMoo.costPlan + "\t" +
				+ a + "\t" + b + "\t");
		
		return config;
	}
	
	
	public long getMinimumMakespanBudget() {
		double maxSpeed = 0.0;
		long costMaxSpeed = 0;
		double ti1 = meanX/60;		
		double ti2 = ti1/speedFactor;
		int c1 = cost;
		int c2 = costFactor*c1;
		maxSpeed += (double) (32 / ti1);
		maxSpeed += (double) (32 / ti2);
		costMaxSpeed += 32 * c1;
		costMaxSpeed += 32 * c2 ;

		double makespanMin = Math.ceil((size/maxSpeed)/60);
		long BmakespanMin = (long) makespanMin * costMaxSpeed;
		
		return BmakespanMin;
	}
	
	public long getMinimumBudget() {
		double ti1 = meanX/60;		
		double ti2 = ti1/speedFactor;
		int c1 = cost;
		int c2 = costFactor*c1;
		if(speedFactor > costFactor) {
			double makespanBmin = Math.ceil((size*ti2)/60);
			long Bmin = (long)makespanBmin*c2;
			return Bmin;
		} else {
			double makespanBmin = Math.ceil((size*ti1)/60);
			long Bmin = (long)makespanBmin*c1;
			return Bmin;
		}
	}
	
	public long getRealMinimumMakespanBudget() {		
		double maxSpeed = 0.0;
		long costMaxSpeed = 0;
		double ti1 = realExpectation/60;		
		double ti2 = ti1/speedFactor;
		int c1 = cost;
		int c2 = costFactor*c1;
		maxSpeed += (double) (32 / ti1);
		maxSpeed += (double) (32 / ti2);
		costMaxSpeed += 32 * c1;
		costMaxSpeed += 32 * c2 ;

		double makespanMin = Math.ceil((size/maxSpeed)/60);
		long BmakespanMin = (long) makespanMin * costMaxSpeed;
		
		return BmakespanMin;
	}
	
	public long getRealMinimumBudget() {
		double ti1 = realExpectation/60;		
		double ti2 = ti1/speedFactor;
		int c1 = cost;
		int c2 = costFactor*c1;
		double makespanBmin = Math.ceil((size*ti2)/60);
		long Bmin = (long)makespanBmin*c2;
		
		return Bmin;
		
	}

	public double execute(long executorSeed, int nATU, int a, int b) {
	
		if((a+b) == 0) return Double.NaN;
	
		ArrayList<Node> slow = new ArrayList<Node>();
		ArrayList<Node> fast = new ArrayList<Node>();
		
		Random randomExecutor = new Random(executorSeed);
		
		if(printNodeStats)
			System.out.println("\ta=" + a + "; b=" + b);
		
		int totalTasks = x.size();
		
		for(int i = 0; i < a; i ++) {
			Node sl = new Node();
			sl.initialRt = x.remove(randomExecutor.nextInt(x.size())).doubleValue();
			sl.crtJob = sl.initialRt;
			slow.add(sl);
		}
		
		for(int i = 0; i < b; i ++) {
			Node fs = new Node();
			fs.initialRt = x.remove(randomExecutor.nextInt(x.size())).doubleValue()/speedFactor;
			fs.crtJob = fs.initialRt;
			fast.add(fs);
		}
		
		
		int doneTasks = 0;
		
		while(totalTasks > doneTasks) {
			double minRt = Double.MAX_VALUE;
			Node minNode = null, minSlowNode;
			for(Node sl : slow) {
				if((!sl.nodeFinished) && (sl.crtJob < minRt)) {
					minNode = sl;
					minRt = sl.crtJob;
				}								
			}
			minSlowNode = minNode;
			
			for(Node fs : fast) {
				if((!fs.nodeFinished) && (fs.crtJob < minRt)) {
					minNode = fs;
					minRt = fs.crtJob;
				}								
			}
			
			minNode.doneJob();
			doneTasks ++;
			
			if(minNode == minSlowNode) {
				if(x.size() > 0) {
					minNode.initialRt = x.remove(randomExecutor.nextInt(x.size())).doubleValue();
					minNode.crtJob = minNode.initialRt;
				} else {
					minNode.nodeFinished = true;
				}					
			} else {
				if(x.size() > 0) {
					minNode.initialRt = x.remove(randomExecutor.nextInt(x.size())).doubleValue()/speedFactor;
					minNode.crtJob = minNode.initialRt;
				} else {
					minNode.nodeFinished = true;
				}
			}				
			
			for(Node sl : slow) {
				if(sl == minNode) {continue;}
				sl.crtJob -= minRt;
				if((!sl.nodeFinished) && (sl.crtJob == 0)) {
					sl.doneJob();
					doneTasks ++;
					if(x.size() > 0) {
						sl.initialRt = x.remove(randomExecutor.nextInt(x.size())).doubleValue();
						sl.crtJob = sl.initialRt;
					} else {
						sl.nodeFinished = true;						
					}
				}
			}
			
			for(Node fs : fast) {
				if(fs == minNode) {continue;}
				fs.crtJob -= minRt;
				if((!fs.nodeFinished) && (fs.crtJob == 0)) {
					fs.doneJob();
					doneTasks ++;
					if(x.size() > 0) {						
						fs.initialRt = x.remove(randomExecutor.nextInt(x.size())).doubleValue()/speedFactor;
						fs.crtJob = fs.initialRt;
					} else {						
						fs.nodeFinished = true;
					}
				}
			}			
		}
		
		double violation = Double.NEGATIVE_INFINITY;
		
		overMnodes = 0;
		
		for(Node sl : slow) {
			/*if crtJob isn't zero, we should add 
			 * the runtime of the last job executed by that machine
			 * which is NOT crtJob, since that is decreased
			 * by minRt at each cycle
			 */
			double nodeRt = sl.totalRt + (sl.crtJob == 0 ? 0 : sl.initialRt);  
			if(printNodeStats)
				System.out.print(nodeRt+"\t");
			if((nodeRt/60 - nATU*60)  > violation) 
				violation = (nodeRt/60 - nATU*60) ;
			if(nodeRt/60 > nATU*60) {
				overMnodes ++;
			}
		}
		for(Node fs : fast) {
			double nodeRt = fs.totalRt + (fs.crtJob == 0 ? 0 : fs.initialRt);
			if(printNodeStats)
				System.out.print(nodeRt+"\t");
			//if(nodeRt/60 > nATU*60) violation = true;
			if((nodeRt/60 - nATU*60) > violation) 
				violation = (nodeRt/60 - nATU*60) ;
			if(nodeRt/60 > nATU*60) {
				overMnodes ++;
			}
		}
		if(printNodeStats)
			System.out.println("\nmakespan overdone: " + violation + ", machines overdone " + overMnodes);
		
		
		return violation;
	}

public long getMinimumMakespanBudgetMultipleClusters() {
	double maxSpeed = 0.0;
	long costMaxSpeed = 0;
	
	for(SimulatedCluster simCluster : clusters.values()) {		
		costMaxSpeed += simCluster.maxNodes * simCluster.cost;
		maxSpeed += (double) (simCluster.maxNodes / simCluster.Ti);
	}
	
	double makespanMin = Math.ceil((size/maxSpeed)/60);
	long BmakespanMin = (long) makespanMin * costMaxSpeed;

	return BmakespanMin;
}

public long getMinimumBudgetMultipleClusters() {
	SimulatedCluster mostProfitable = selectMostProfitable();
	double makespanBmin = Math.ceil(size*mostProfitable.Ti/60);
	double Bmin = makespanBmin*mostProfitable.cost;

	return (long)Math.ceil(Bmin);
}

protected SimulatedCluster selectMostProfitable() {
	SimulatedCluster mostProfitable = null;
	SimulatedCluster cheapest = findCheapest();
	
	double profitMax = Double.MIN_VALUE;
	for(SimulatedCluster simCluster : clusters.values()) {
		simCluster.computeProfitability(cheapest);
		if(simCluster.profitability > profitMax) {
			profitMax = simCluster.profitability;
			mostProfitable = simCluster;
		}
	}
	//System.out.println("Most profitable cluster is: " + mostProfitable.name);
	return mostProfitable;
} 

SimulatedCluster findCheapest() {
	SimulatedCluster cheapestCluster = null;
	double cheapest = Double.MAX_VALUE;
	for (SimulatedCluster simCluster : clusters.values()) {
		if(simCluster.cost < cheapest) {
			cheapest = simCluster.cost;
			cheapestCluster = simCluster;
		} else if (simCluster.cost == cheapest) {
			if(simCluster.Ti < cheapestCluster.Ti) {
				cheapestCluster = simCluster;
			}
		}
	}
	return cheapestCluster;
}

public SimulatedExecution executeMultipleClustersMoreDetails(long executorSeed, SimulatedSchedule sched) {
	
	if(sched.atus == Integer.MAX_VALUE) return new SimulatedExecution(Double.NaN, 0, 0);
	
	double violation = 0.0;
	
	HashMap<String,ArrayList<Node>> allNodes = new HashMap<String, ArrayList<Node>> ();
	
	ArrayList<Task> tasks = new ArrayList<Task>();
		
	for(int i=0; i<x.size(); i++){
		tasks.add(new Task(i, x.get(i)));
	}
	
	/*in case we repeat executions with different tail strategies
	 * on the same bag (object)*/
	resetDistributionParameterEstimates();
	
	Random randomExecutor = new Random(executorSeed);
	
	for(String clusterName : sched.machinesPerCluster.keySet()) {
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		allNodes.put(clusterName, nodes);
		SimulatedCluster cluster = clusters.get(clusterName);
		
		for(int i = 0; i < sched.machinesPerCluster.get(clusterName); i++) {
			Node node = new Node();
			Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
			node.task = tmp;
			node.initialRt = tmp.rt/cluster.speedFactor;
			node.crtJob = node.initialRt;
			node.nATU = sched.atus;
			node.clusterName = clusterName;
			nodes.add(node);
		}
	}
	
	int doneTasks = 0;
	
	while(x.size() > doneTasks) {
		//if(!emergencyOnly) System.out.println("doneTasks before cycle: " + doneTasks);
		double minRt = Double.MAX_VALUE;
		Node minNode = null;
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			for(Node node : allNodes.get(clusterName)) {			
				if((!node.nodeFinished) && (node.crtJob < minRt)) {
					minNode = node; 
					minRt = node.crtJob;
				}								
			}		
		}
		minNode.doneJob();
		doneTasks ++;
				
		//if(!emergencyOnly) System.out.println("minNode is a fast node");
		if(tasks.size() > 0) {
			Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size()));
			minNode.task = tmp;
			minNode.initialRt = tmp.rt/clusters.get(minNode.clusterName).speedFactor;
			minNode.crtJob = minNode.initialRt;			
		} else {
			minNode.nodeFinished = true;
		}
				
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			for(Node node : allNodes.get(clusterName)) {
				if(minNode == node) continue;
				if(node.nodeFinished) continue;
				node.crtJob -= minRt;
				if(node.crtJob == 0) {
					if(tasks.size() > 0) {
						node.doneJob(); 
						doneTasks ++;				
						
						Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
						node.task = tmp;
						node.initialRt = tmp.rt/clusters.get(node.clusterName).speedFactor;
						node.crtJob = node.initialRt;
					} else {
						node.doneJob(); 
						doneTasks ++;				
						node.nodeFinished = true;
					}
				}
			}
		}		
	}
	
    violation = Double.NEGATIVE_INFINITY;
	
	overMnodes = 0;
	long cost = 0;
	
	for(String clusterName : sched.machinesPerCluster.keySet()) {
		for(Node node : allNodes.get(clusterName)) {
			double nodeRt = node.totalRt;
			cost += Math.ceil(nodeRt/3600)*clusters.get(clusterName).cost;
			if(printNodeStats)
				System.out.print(Math.ceil(nodeRt/3600)+"\t");
			if((nodeRt/60 - node.nATU*60) > violation) 
				violation = nodeRt/60 - node.nATU*60;
			if(nodeRt/60 > node.nATU*60) {				
				overMnodes ++;
			} 
		} System.out.print("||");
	}
	
	if(printNodeStats) {
		System.out.println("\nmakespan overdone: " + violation + ", machines overdone: " + overMnodes + ", cost: " + cost); 
						   //"\n tasks to be done: " + x.size() + "\n tasks done: " + doneTasks +
	}

	double makespanMinutes = sched.atus*60 + violation;
	return new SimulatedExecution(makespanMinutes, cost, overMnodes);
}


public double executeMultipleClusters(long executorSeed, SimulatedSchedule sched) {
	
	if(sched.atus == Integer.MAX_VALUE) return Double.NaN;
	
	double violation = 0.0;
	
	HashMap<String,ArrayList<Node>> allNodes = new HashMap<String, ArrayList<Node>> ();
		
	ArrayList<Task> tasks = new ArrayList<Task>();
		
	for(int i=0; i<x.size(); i++){
		tasks.add(new Task(i, x.get(i)));
	}
	
	/*in case we repeat executions with different tail strategies
	 * on the same bag (object)*/
	resetDistributionParameterEstimates();
	
	Random randomExecutor = new Random(executorSeed);
	
	for(String clusterName : sched.machinesPerCluster.keySet()) {
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		allNodes.put(clusterName, nodes);
		SimulatedCluster cluster = clusters.get(clusterName);
		
		for(int i = 0; i < sched.machinesPerCluster.get(clusterName); i++) {
			Node node = new Node();
			Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
			node.task = tmp;
			node.initialRt = tmp.rt/cluster.speedFactor;
			node.crtJob = node.initialRt;
			node.nATU = sched.atus;
			node.clusterName = clusterName;
			nodes.add(node);
		}
	}
	
	int doneTasks = 0;
	
	while(x.size() > doneTasks) {
		//if(!emergencyOnly) System.out.println("doneTasks before cycle: " + doneTasks);
		double minRt = Double.MAX_VALUE;
		Node minNode = null;
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			for(Node node : allNodes.get(clusterName)) {			
				if((!node.nodeFinished) && (node.crtJob < minRt)) {
					minNode = node; 
					minRt = node.crtJob;
				}								
			}		
		}
		minNode.doneJob();
		doneTasks ++;
				
		//if(!emergencyOnly) System.out.println("minNode is a fast node");
		if(tasks.size() > 0) {
			Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size()));
			minNode.task = tmp;
			minNode.initialRt = tmp.rt/clusters.get(minNode.clusterName).speedFactor;
			minNode.crtJob = minNode.initialRt;			
		} else {
			minNode.nodeFinished = true;
		}
				
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			for(Node node : allNodes.get(clusterName)) {
				if(minNode == node) continue;
				if(node.nodeFinished) continue;
				node.crtJob -= minRt;
				if(node.crtJob == 0) {
					if(tasks.size() > 0) {
						node.doneJob(); 
						doneTasks ++;				
						
						Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
						node.task = tmp;
						node.initialRt = tmp.rt/clusters.get(node.clusterName).speedFactor;
						node.crtJob = node.initialRt;
					} else {
						node.doneJob(); 
						doneTasks ++;				
						node.nodeFinished = true;
					}
				}
			}
		}		
	}
	
    violation = Double.NEGATIVE_INFINITY;
	
	overMnodes = 0;
		
	for(String clusterName : sched.machinesPerCluster.keySet()) {
		for(Node node : allNodes.get(clusterName)) {
		/*since we loop until all tasks are done at least once, 
		 * we don't care about the last task running on 
		 * a machine: if it didn't make it into the totalRt it means
		 * it was already executed by another machine*/
			double nodeRt = node.totalRt;
			if(printNodeStats)
				System.out.print(nodeRt+"\t");
			if((nodeRt/60 - node.nATU*60) > violation) 
				violation = (nodeRt/60 - node.nATU*60) ;
			if(nodeRt/60 > node.nATU*60) {
				overMnodes ++;
			}
		}
	}
	
	if(printNodeStats) {
		System.out.println("\nmakespan overdone: " + violation + ", machines overdone: " + overMnodes); 
						   //"\n tasks to be done: " + x.size() + "\n tasks done: " + doneTasks +
	}


	return violation;
}


public double executeTailMultipleClusters(long executorSeed, SimulatedSchedule sched, int nTail) {
	
	if(sched.atus == Integer.MAX_VALUE) return Double.NaN;
	
	double violation = 0.0;
	
	HashMap<String,ArrayList<Node>> allNodes = new HashMap<String, ArrayList<Node>> ();
		
	ArrayList<Task> tasks = new ArrayList<Task>();
	ArrayList<Integer> check = new ArrayList<Integer>();
	
	for(int i=0; i<x.size(); i++){
		tasks.add(new Task(i, x.get(i)));
		check.add(i);
	}
	
	histogram = new TreeMap<Integer,Occurrence>();	
	for(int i = 0; i < sampleSize; i++) {
		Double rt = sampledTasks.get(i);
		if(!histogram.containsKey(rt.intValue())) {
			histogram.put(rt.intValue(), new Occurrence());			
		} 
		histogram.get(rt.intValue()).occurrence ++;
	}
	/*in case we repeat executions with different tail strategies
	 * on the same bag (object)*/
	resetDistributionParameterEstimates();
	
	Random randomExecutor = new Random(executorSeed);
	candidateSelector = new Random(executorSeed+100000000000000L);
	
	for(String clusterName : sched.machinesPerCluster.keySet()) {
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		allNodes.put(clusterName, nodes);
		SimulatedCluster cluster = clusters.get(clusterName);
		
		for(int i = 0; i < sched.machinesPerCluster.get(clusterName); i++) {
			Node node = new Node();
			Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
			tmp.primary = node;
			node.task = tmp;
			node.initialRt = tmp.rt/cluster.speedFactor;
			node.crtJob = node.initialRt;
			node.nATU = sched.atus;
			node.clusterName = clusterName;
			nodes.add(node);
		}
	}
	
	int doneTasks = 0;
	
	while(x.size() > doneTasks) {
		//if(emergencyOnly) System.out.println("doneTasks before cycle: " + doneTasks);
		double minRt = Double.MAX_VALUE;
		Node minNode = null;
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			for(Node node : allNodes.get(clusterName)) {			
				if((!node.nodeFinished) && (node.crtJob < minRt)) {
					minNode = node; 
					minRt = node.crtJob;
				}								
			}		
		}
		/*
		if(minNode == null) {
			System.out.println(x.size() + " versus " + doneTasks);
			break;
		}
		*/
		int taskDoneFirstTime = minNode.doneJob(check);
		doneTasks += taskDoneFirstTime;
		if(taskDoneFirstTime == 1) {
			Integer rt = new Integer((int)minNode.task.rt);
			if(!histogram.containsKey(rt)) {
				histogram.put(rt, new Occurrence());				
			} 
			histogram.get(rt).occurrence ++;
			updateDistributionParameterEstimates(minNode.task.rt);
		}
		
		/*since we do not know from which node another node would steal.*/
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			for(Node node : allNodes.get(clusterName)) {
				if(!node.nodeFinished) 	
					node.crtJob -= minRt;
			}
		}
		
		if(tasks.size() > nTail) {
			Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size()));
			tmp.primary = minNode;
			minNode.task = tmp;			
			minNode.initialRt = tmp.rt/clusters.get(minNode.clusterName).speedFactor;
			minNode.crtJob = minNode.initialRt;			
		} else {			
			replicateMigrateMultipleClusters(sched, minNode, allNodes, tasks, randomExecutor);			
		}	
		
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			for(Node node : allNodes.get(clusterName)) {
				if(node.nodeFinished) continue;
				if(minNode == node) continue;		
				if(node.crtJob == 0) {
					if(tasks.size() > nTail) {
						taskDoneFirstTime = node.doneJob(check); 
						doneTasks += taskDoneFirstTime;				
						if(taskDoneFirstTime == 1) {
							Integer rt = new Integer((int)node.task.rt);
							if(!histogram.containsKey(rt)) {
								histogram.put(rt, new Occurrence());								
							} 
							histogram.get(rt).occurrence ++;
							updateDistributionParameterEstimates(node.task.rt);
						}
						Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
						tmp.primary = node;
						node.task = tmp;
						node.initialRt = tmp.rt/clusters.get(node.clusterName).speedFactor;
						node.crtJob = node.initialRt;
					} else {
						taskDoneFirstTime = node.doneJob(check); 
						doneTasks += taskDoneFirstTime;				
						if(taskDoneFirstTime == 1) {
							Integer rt = new Integer((int)node.task.rt);
							if(!histogram.containsKey(rt)) {
								histogram.put(rt, new Occurrence());								
							} 
							histogram.get(rt).occurrence ++;
							updateDistributionParameterEstimates(node.task.rt);	
						}		
						//if(!emergencyOnly) System.out.println("Entering migrate method");
						replicateMigrateMultipleClusters(sched, node, allNodes, tasks, randomExecutor);
						//if(!emergencyOnly) System.out.println("Replicate/Migrate");
					}
				}
			}
		}		
	}
	
    violation = Double.NEGATIVE_INFINITY;
	
	overMnodes = 0;
	double totalWastedRt = 0;
	
	int totalMigrated=0, totalReplicated=0;
	
	
	for(String clusterName : sched.machinesPerCluster.keySet()) {
		for(Node node : allNodes.get(clusterName)) {
		/*since we loop until all tasks are done at least once, 
		 * we don't care about the last task running on 
		 * a machine: if it didn't make it into the totalRt it means
		 * it was already executed by another machine*/
			double nodeRt = node.totalRt;
			if((node.task!=null) && (node.task.migratedUnterminated)) {
				node.wastedRt += node.initialRt - node.crtJob;
			}

			totalWastedRt += node.wastedRt;

			totalMigrated += node.noTasksMigrated;
			totalReplicated += node.noTasksReplicated;
			
			if(printNodeStats)
				System.out.print(nodeRt+"\t");
			if((nodeRt/60 - node.nATU*60) > violation) 
				violation = (nodeRt/60 - node.nATU*60) ;
			if(nodeRt/60 > node.nATU*60) {
				overMnodes ++;
			}
		}
	}
	
	if(printNodeStats) {
		System.out.println("\nmakespan overdone: " + violation + ", machines overdone: " + overMnodes + 
						   //"\n tasks to be done: " + x.size() + "\n tasks done: " + doneTasks +
						   "\n total wasted runtime: " + totalWastedRt + 
						   ", total number migrated: " + totalMigrated + 
						   ", total number replicated: " + totalReplicated);
		System.out.println("check:" + check.size());
		for(Integer i : check) {
			System.out.print(" " + i );
		}
	}

	
	return violation;
}

	private void replicateMigrateMultipleClusters(SimulatedSchedule sched, Node minNode,
		HashMap<String, ArrayList<Node>> allNodes, ArrayList<Task> tasks,
		Random randomExecutor) {

		if(tasks.size() == 0) {
			/*only replicate*/
			//if(!emergencyOnly) System.out.println("replicate in migrate");
			replicateMultipleClusters(sched, minNode, allNodes, tasks);
		} else {
			
			//if(!emergencyOnly) System.out.println("migrate in migrate");
			migrateMultipleClusters(sched, minNode, allNodes, tasks, randomExecutor);
		}
}
	
	private void replicateMultipleClusters(SimulatedSchedule sched, Node minNode,
			HashMap<String, ArrayList<Node>> allNodes, ArrayList<Task> tasks) {
		ArrayList<Task> candidates = new ArrayList<Task>();
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			/*skip faster/same-speed cluster*/
			if(clusters.get(clusterName).speedFactor >= clusters.get(minNode.clusterName).speedFactor) 
				continue;
			for(Node node : allNodes.get(clusterName)) {				
				if(!node.nodeFinished) {
					if(allowMultipleReplicas) {
						/*node is source and minNode is target*/
						/*allow multiple replicas, but running on different machine types*/
						if((!node.task.replicas.containsKey(minNode.clusterName)) 
								&& (isReplicationCandidateMultipleClusters(node.task,node,minNode))) {							
							candidates.add(node.task);
						}
					} else if(!(node.task.replicated || node.task.migrated)) {
						/*node is source and minNode is target*/
						if(isReplicationCandidateMultipleClusters(node.task,node,minNode)) {
							candidates.add(node.task);
						}
					}
				}
			}
		}

		if(candidates.size() == 0) {
			minNode.nodeFinished = true;
			minNode.task = null;
			//System.out.println("No candidate found, node finished");
			return;
		}	
		
		if(printDebug) {
			System.out.println("Replication candidates");
			for(Task t : candidates) {
				System.out.print(" " + t.id + " ");
			}
			System.out.println();
		}
		
		Task nextTaskToReplicate = selectTaskToReplicate(candidates);
	
		if(printDebug) {	
			System.out.println("Replicating task: " + nextTaskToReplicate.id + 
							   " with primary:  " + nextTaskToReplicate.primary.clusterName + 
							   " on : " + minNode.clusterName);
		}
		nextTaskToReplicate.replicated = true;
		nextTaskToReplicate.replicas.put(minNode.clusterName,minNode);
		nextTaskToReplicate.timesReplicated ++;
		minNode.task = nextTaskToReplicate;
		minNode.initialRt = nextTaskToReplicate.rt/clusters.get(minNode.clusterName).speedFactor;
		minNode.crtJob = minNode.initialRt;
		minNode.noTasksReplicated ++;

}

	private void migrateMultipleClusters(SimulatedSchedule sched, Node minNode,
			HashMap<String, ArrayList<Node>> allNodes, ArrayList<Task> tasks,
			Random randomExecutor) {
		ArrayList<Task> candidates = new ArrayList<Task>();
		for(String clusterName : sched.machinesPerCluster.keySet()) {
			/*skip faster cluster*/
			if(clusters.get(clusterName).speedFactor >= clusters.get(minNode.clusterName).speedFactor) 
				continue;
			for(Node node : allNodes.get(clusterName)) {
				if(!node.nodeFinished) {
					if(allowReMigration) {
						/*node is source and minNode is target*/
						if(isMigrationCandidateMultipleClusters(node.task, node, minNode)) {
							//if(!emergencyOnly) System.out.println("true for: " + sl.task.id);
							candidates.add(node.task);
						}
					} else if(!node.task.migrated) {
						//if(!emergencyOnly) System.out.println("Enter check candidate: " );
						/*node is source and minNode is target*/
						if(isMigrationCandidateMultipleClusters(node.task, node, minNode)) {
							//if(!emergencyOnly) System.out.println("true for: " + sl.task.id);
							candidates.add(node.task);
						}
					}
				}
			}
		}
		//if(!emergencyOnly) System.out.println("found candidates size: " + candidates.size());

		if(candidates.size() ==0) {
			//if(!emergencyOnly) System.out.println("Found no candidate; going to execute normal task");
			Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
			tmp.primary = minNode;
			minNode.task = tmp;
			minNode.initialRt = tmp.rt/clusters.get(minNode.clusterName).speedFactor;
			minNode.crtJob = minNode.initialRt;
			return;
		}
		if(printDebug) {
			System.out.println("Migration candidates");
			for(Task t : candidates) {
				System.out.print(" " + t.id + " ");
			}
			System.out.println();
		}
		
		//if(!emergencyOnly) System.out.println("Enter select task: " );
		Task nextTaskToMigrate = selectTaskToMigrate(candidates);

		if(printDebug) {	
			System.out.println("Migrating task: " + nextTaskToMigrate.id + 
							   " with primary:  " + nextTaskToMigrate.primary.clusterName + 
							   " to another primary: " + minNode.clusterName);
		}
		
		nextTaskToMigrate.primary = minNode;
		minNode.task = nextTaskToMigrate;
		nextTaskToMigrate.migrated = true;
		minNode.initialRt = nextTaskToMigrate.rt/clusters.get(minNode.clusterName).speedFactor;
		minNode.crtJob = minNode.initialRt;
		minNode.noTasksMigrated ++;

		for(String clusterName : sched.machinesPerCluster.keySet()) {
			for(Node node : allNodes.get(clusterName)) {
				if(node.task == nextTaskToMigrate) {	
					if(node == minNode) continue;
					if(tasks.size() > 0) {
						node.totalRt += node.initialRt - node.crtJob;
						node.wastedRt += node.initialRt - node.crtJob;
						Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
						tmp.primary = node;
						if(printDebug) 
							System.out.println("Previous host of migrated task: " +  nextTaskToMigrate.id +
											   " will start execution of task: " + tmp.id);
						node.task = tmp;
						node.initialRt = tmp.rt/clusters.get(node.clusterName).speedFactor;
						node.crtJob = node.initialRt;
					} else {
						if(printDebug) {
							System.out.println("Since bag is empty, " +
											   "previous host of migrated task will continue execution of task: " + nextTaskToMigrate.id);
						}
						node.task.migratedUnterminated = true;
					}
					break;	
				}
			}
		}

	}

public double executeTail(long executorSeed, int nATU, int a, int b, int nTail) {
	
	if((a+b) == 0) return Double.NaN;
	
	ArrayList<Node> slow = new ArrayList<Node>();
	ArrayList<Node> fast = new ArrayList<Node>();
	
	ArrayList<Task> tasks = new ArrayList<Task>();
	ArrayList<Integer> check = new ArrayList<Integer>();
	
	for(int i=0; i<x.size(); i++){
		tasks.add(new Task(i, x.get(i)));
		check.add(i);
	}
	
	histogram = new TreeMap<Integer,Occurrence>();	
	for(int i = 0; i < sampleSize; i++) {
		Double rt = sampledTasks.get(i);
		if(!histogram.containsKey(rt.intValue())) {
			histogram.put(rt.intValue(), new Occurrence());			
		} 
		histogram.get(rt.intValue()).occurrence ++;
	}
	
	/*in case we repeat executions with different tail strategies
	 * on the same bag (object)*/
	resetDistributionParameterEstimates();
	
	Random randomExecutor = new Random(executorSeed);
	
	if(printNodeStats)
		System.out.println("\ta=" + a + "; b=" + b);
	
	/*
	if((nTail > 0) && (nTail < (a+b))) {
		nTail = a+b;
	}*/
	
	for(int i = 0; i < a; i ++) {
		Node sl = new Node();
		Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
		sl.task = tmp;
		sl.initialRt = tmp.rt;
		sl.crtJob = sl.initialRt;
		sl.nATU = nATU;
		slow.add(sl);
	}
	
	for(int i = 0; i < b; i ++) {
		Node fs = new Node();
		Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
		fs.task = tmp;
		fs.initialRt = tmp.rt/speedFactor;
		fs.crtJob = fs.initialRt;
		fs.nATU = nATU;
		fast.add(fs);
	}
	
	int doneTasks = 0;
	
	while(x.size() > doneTasks) {
		//if(!emergencyOnly) System.out.println("doneTasks before cycle: " + doneTasks);
		double minRt = Double.MAX_VALUE;
		Node minNode = null, minSlowNode;
		for(Node sl : slow) {			
			if((!sl.nodeFinished) && (sl.crtJob < minRt)) {
				minNode = sl; 
				minRt = sl.crtJob;
			}								
		}
		minSlowNode = minNode;
		
		//if(!emergencyOnly) System.out.println("slow nodes: " + slow.size() + "; minNode: " + minNode);
		
		for(Node fs : fast) {
			if((!fs.nodeFinished) && (fs.crtJob < minRt)) {
				minNode = fs;
				minRt = fs.crtJob;
			}								
		}
		/*
		if(!emergencyOnly) System.out.println("slow nodes: " + slow.size());
		if(!emergencyOnly) System.out.println("Node with minRt:" + minNode.crtJob + "; task id " + minNode.task.id);
		*/
		int taskDoneFirstTime = minNode.doneJob(check);
		doneTasks += taskDoneFirstTime;
		if(taskDoneFirstTime == 1) {
			/*meanX = (meanX*(doneTasks-1+sampleSize)+minNode.task.rt)/(doneTasks+sampleSize);
			if(minNode.task.rt > estimateXmax) {
				estimateXmax = minNode.task.rt;
			}*/			
			//if(useHisto) {
				Integer rt = new Integer((int)minNode.task.rt);
				if(!histogram.containsKey(rt)) {
					histogram.put(rt, new Occurrence());					
				} 
				histogram.get(rt).occurrence ++;
			//}
				updateDistributionParameterEstimates(minNode.task.rt);
		}
		
		if(minNode == minSlowNode) {
			//if(!emergencyOnly) System.out.println("minNode is a slow node");
			if(tasks.size() > 0) {
				Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size()));
				minNode.task = tmp;
				minNode.initialRt = tmp.rt;
				minNode.crtJob = minNode.initialRt;
				//if(!emergencyOnly) System.out.println("minNode is a slow node; tasks size is now: " + tasks.size());
			} else {
				minNode.task = null;
				minNode.nodeFinished = true;
				//if(!emergencyOnly) System.out.println("minNode is a slow node; node will finish; tasks size is now: " + tasks.size());
			}
		} else {
			//if(!emergencyOnly) System.out.println("minNode is a fast node");
			if(tasks.size() > nTail) {
				Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size()));
				minNode.task = tmp;
				minNode.initialRt = tmp.rt/speedFactor;
				minNode.crtJob = minNode.initialRt;
				//if(!emergencyOnly) System.out.println("minNode is a fast node; tasks size is now: " + tasks.size());
			} else {
				//if(!emergencyOnly) System.out.println("Entering migrate method in minNode");
				replicateMigrate(minNode, slow, tasks, randomExecutor);
				//if(!emergencyOnly) System.out.println("Replicate/Migrate minNode");
			}
		}		
		
		//if(!emergencyOnly) System.out.println("tasks size is now: " + tasks.size());
	
		
		for(Node sl : slow) {
			if(minNode == sl) continue;
			sl.crtJob -= minRt;
			if(sl.crtJob == 0) {
				if(tasks.size() > 0) {	
					taskDoneFirstTime = sl.doneJob(check); 
					doneTasks += taskDoneFirstTime;				
					if(taskDoneFirstTime == 1) {/*
						meanX = (meanX*(doneTasks-1+sampleSize)+sl.task.rt)/(doneTasks+sampleSize);
						if(sl.task.rt > estimateXmax) {
							estimateXmax = sl.task.rt;
						}*/						
						//if(useHisto) {
							Integer rt = new Integer((int)sl.task.rt);
							if(!histogram.containsKey(rt)) {
								histogram.put(rt, new Occurrence());								
							} 
							histogram.get(rt).occurrence ++;
						//}
							updateDistributionParameterEstimates(sl.task.rt);
					}
					Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size()));
					sl.task = tmp;
					sl.initialRt = tmp.rt;
					sl.crtJob = sl.initialRt;
				} else {
					taskDoneFirstTime = sl.doneJob(check); 
					doneTasks += taskDoneFirstTime;				
					if(taskDoneFirstTime == 1) {/*
						meanX = (meanX*(doneTasks-1+sampleSize)+sl.task.rt)/(doneTasks+sampleSize);
						if(sl.task.rt > estimateXmax) {
							estimateXmax = sl.task.rt;
						}*/						
						//if(useHisto) {
							Integer rt = new Integer((int)sl.task.rt);
							if(!histogram.containsKey(rt)) {
								histogram.put(rt, new Occurrence());								
							} 
							histogram.get(rt).occurrence ++;
						//}
							updateDistributionParameterEstimates(sl.task.rt);
					}
					sl.nodeFinished = true;
					sl.task = null;
				}
			}
		}
		
		for(Node fs : fast) {
			if(minNode == fs) continue;
			fs.crtJob -= minRt;
			if(fs.crtJob == 0) {
				if(tasks.size() > nTail) {
					taskDoneFirstTime = fs.doneJob(check); 
					doneTasks += taskDoneFirstTime;				
					if(taskDoneFirstTime == 1) {/*
						meanX = (meanX*(doneTasks-1+sampleSize)+fs.task.rt)/(doneTasks+sampleSize);
						if(fs.task.rt > estimateXmax) {
							estimateXmax = fs.task.rt;
						}*/						
						//if(useHisto) {
							Integer rt = new Integer((int)fs.task.rt);
							if(!histogram.containsKey(rt)) {
								histogram.put(rt, new Occurrence());								
							} 
							histogram.get(rt).occurrence ++;
						//}
							updateDistributionParameterEstimates(fs.task.rt);
					}
					Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
					fs.task = tmp;
					fs.initialRt = tmp.rt/speedFactor;
					fs.crtJob = fs.initialRt;
				} else {
					taskDoneFirstTime = fs.doneJob(check); 
					doneTasks += taskDoneFirstTime;				
					if(taskDoneFirstTime == 1) {/*
						meanX = (meanX*(doneTasks-1+sampleSize)+fs.task.rt)/(doneTasks+sampleSize);
						if(fs.task.rt > estimateXmax) {
							estimateXmax = fs.task.rt;
						}*/						
						//if(useHisto) {
							Integer rt = new Integer((int)fs.task.rt);
							if(!histogram.containsKey(rt)) {
								histogram.put(rt, new Occurrence());
							} 
							histogram.get(rt).occurrence ++;
						//}
							updateDistributionParameterEstimates(fs.task.rt);	
					}		
					//if(!emergencyOnly) System.out.println("Entering migrate method");
					replicateMigrate(fs, slow, tasks, randomExecutor);
					//if(!emergencyOnly) System.out.println("Replicate/Migrate");
				}
			}
		}
		//if(!emergencyOnly) System.out.println("doneTasks after cycle: " + doneTasks);
	}
	
	
	
	double violation = Double.NEGATIVE_INFINITY;
	
	overMnodes = 0;
	double totalWastedRt = 0;
	
	int totalMigrated=0, totalReplicated=0;
	
	for(Node sl : slow) {
		/*since we loop until all tasks are done at least once, 
		 * we don't care about the last task running on 
		 * a machine: if it didn't make it into the totalRt it means
		 * it was already executed by another machine*/
		double nodeRt = sl.totalRt;
		if((sl.task!=null) && (sl.task.migratedUnterminated)) {
			sl.wastedRt += sl.initialRt - sl.crtJob;
		}
		
		totalWastedRt += sl.wastedRt;
		
		if(printNodeStats)
			System.out.print(nodeRt+"\t");
		if((nodeRt/60 - nATU*60) > violation) 
			violation = (nodeRt/60 - nATU*60) ;
		if(nodeRt/60 > nATU*60) {
			overMnodes ++;
		}
	}
	for(Node fs : fast) {
		double nodeRt = fs.totalRt;
		totalMigrated += fs.noTasksMigrated;
		totalReplicated += fs.noTasksReplicated;
		
		if(printNodeStats)
			System.out.print(nodeRt+"\t");
		//if(nodeRt/60 > nATU*60) violation = true;
		if((nodeRt/60 - nATU*60) > violation) 
			violation = (nodeRt/60 - nATU*60);
		if(nodeRt/60 > nATU*60) {
			overMnodes ++;
		}
	}
	if(printNodeStats) {
		System.out.println("\nmakespan overdone: " + violation + ", machines overdone: " + overMnodes + 
						   //"\n tasks to be done: " + x.size() + "\n tasks done: " + doneTasks +
						   "\n total wasted runtime: " + totalWastedRt + 
						   ", total number migrated: " + totalMigrated + 
						   ", total number replicated: " + totalReplicated);
		//System.out.println("check:" + check.size());
		for(Integer i : check) {
			System.out.print(" " + i );
		}
	}
	
	return violation;
}

public void resetDistributionParameterEstimates() {
	meanXExecution = meanX;
	estimateXmaxExecution = estimateXmax;
	varXsqExecution = varXsq;
	stDevXExecution = stDevX;
	System.out.println(meanXExecution + " " + estimateXmaxExecution + " " + varXsqExecution + " " + stDevXExecution);
}

public abstract void updateDistributionParameterEstimates(double rt);

private void replicateMigrate(Node fs, ArrayList<Node> slow,
		ArrayList<Task> tasks, Random randomExecutor) {
		
	if(tasks.size() == 0) {
		/*only replicate*/
		//if(!emergencyOnly) System.out.println("replicate in migrate");
		
		replicate(fs, slow, tasks);
	} else {
		
		//if(!emergencyOnly) System.out.println("migrate in migrate");
		migrate(fs, slow, tasks, randomExecutor);
	}
	
}


private void migrate(Node fs, ArrayList<Node> slow, ArrayList<Task> tasks, Random randomExecutor) {
	ArrayList<Task> candidates = new ArrayList<Task>();
	for(Node sl : slow) {
		if((!sl.nodeFinished) && (!sl.task.migrated)) {
			//if(!emergencyOnly) System.out.println("Enter check candidate: " );
			if(isMigrationCandidate(sl.task,fs,sl)) {
				//if(!emergencyOnly) System.out.println("true for: " + sl.task.id);
				candidates.add(sl.task);
			}
		}
	}
	//if(!emergencyOnly) System.out.println("found candidates size: " + candidates.size());
	
	if(candidates.size() ==0) {
		//if(!emergencyOnly) System.out.println("Found no candidate; going to execute normal task");
		Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
		fs.task = tmp;
		fs.initialRt = tmp.rt/speedFactor;
		fs.crtJob = fs.initialRt;
		return;
	}
	
	//if(!emergencyOnly) System.out.println("Enter select task: " );
	Task nextTaskToMigrate = selectTaskToMigrate(candidates);
	System.out.println("Migrating task: " + nextTaskToMigrate.id);
	fs.task = nextTaskToMigrate;
	nextTaskToMigrate.migrated = true;
	fs.initialRt = nextTaskToMigrate.rt/speedFactor;
	fs.crtJob = fs.initialRt;
	fs.noTasksMigrated ++;
	
	for(Node sl : slow) {
		if(sl.task == nextTaskToMigrate) {			
			if(tasks.size() > 0) {
				sl.totalRt += sl.initialRt - sl.crtJob;
				sl.wastedRt += sl.initialRt - sl.crtJob;
				Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size())); 
				//if(!emergencyOnly) System.out.println("Previous host of migrated task will start execution of task: " + tmp.id);
				sl.task = tmp;
				sl.initialRt = tmp.rt;
				sl.crtJob = sl.initialRt;
			} else {
				//if(!emergencyOnly) System.out.println("Previous host of migrated task will continue execution of task: " + sl.task.id);
				sl.task.migratedUnterminated = true;
			}
		break;	
		}
	}
	//if(!emergencyOnly) System.out.println("Tasks left in bag: " + tasks.size());
}

private Task selectTaskToMigrate(ArrayList<Task> candidates) {
	
	return selectMigrationTaskLargestExpectedRemainingRt(candidates);
	//return selectMigrationTaskLargestExpectedRtOnTargetNode(candidates);
	//if(emergencyOnly) return selectTaskEmergencyOnly(candidates);
	//return selectMigrationTaskLargestSuspectedOverMRtTaskLargestMoreTasksLargestTauSmallestElapsedETFirst(candidates);
	//return selectTaskLargestSuspectedOverMRtLargestTauSmallestElapsedETFirst(candidates);
}


private Task selectMigrationTaskLargestExpectedRemainingRt(
		ArrayList<Task> candidates) {
	if(knowledge == RANDOM) {
		Task nextTaskToMigrate = candidates.get(candidateSelector.nextInt(candidates.size()));
		return nextTaskToMigrate;
	}
	Collections.sort(candidates, new Comparator<Task>(){
		public int compare(Task a, Task b) {			
			double tmp = (a.estimatedRt - a.elapsed) - (b.estimatedRt - b.elapsed);
			if(tmp < 0) return 1;
			if(tmp > 0) return -1;
			return 0;					
		}
	});
	
	Task nextTaskToMigrate = candidates.get(0);	
	return nextTaskToMigrate;

}

private Task selectMigrationTaskLargestExpectedRtOnTargetNode(ArrayList<Task> candidates) {
	if(knowledge == RANDOM) {
		Task nextTaskToMigrate = candidates.get(candidateSelector.nextInt(candidates.size()));
		return nextTaskToMigrate;
	}
	Collections.sort(candidates, new Comparator<Task>(){
		public int compare(Task a, Task b) {			
			double tmp = a.tau - b.tau;
			if(tmp < 0) return 1;
			if(tmp > 0) return -1;
			return 0;					
		}
	});
	
	Task nextTaskToMigrate = candidates.get(0);	
	return nextTaskToMigrate;

}

private Task selectTaskLargestSuspectedOverMRtLargestTauSmallestElapsedETFirst(
		ArrayList<Task> candidates) {
	Collections.sort(candidates, new Comparator<Task>(){
		public int compare(Task a, Task b) {
			double tmpOverM = a.estimatedOverMRt - b.estimatedOverMRt;
			if(tmpOverM < 0) return 1;
			if(tmpOverM > 0) return -1;
			double tmp = a.tau - b.tau;
			if(tmp < 0) return 1;
			if(tmp > 0) return -1;
			double tmpElapsed = a.elapsed - b.elapsed;
			if(tmpElapsed < 0) return -1;
			if(tmpElapsed > 0) return 1;
			return 0;					
		}
	});
	
	Task nextTaskToReplicate = candidates.get(0);
	
	return nextTaskToReplicate;

}

private Task selectTaskEmergencyOnly(ArrayList<Task> candidates) {
	Collections.sort(candidates, new Comparator<Task>(){
		public int compare(Task a, Task b) {
			double tmpOverM = a.estimatedOverMRt - b.estimatedOverMRt;
			if(tmpOverM < 0) return 1;
			if(tmpOverM > 0) return -1;			
			return 0;					
		}
	});
	
	Task nextTaskToMigrate = candidates.get(0);
	
	return nextTaskToMigrate;
}

private Task selectMigrationTaskLargestSuspectedOverMRtTaskLargestMoreTasksLargestTauSmallestElapsedETFirst(ArrayList<Task> candidates) {
	if(knowledge == RANDOM) {
		Task nextTaskToMigrate = candidates.get(candidateSelector.nextInt(candidates.size()));
		
		return nextTaskToMigrate;
	}
	Collections.sort(candidates, new Comparator<Task>(){
		public int compare(Task a, Task b) {
			double tmpOverM = a.estimatedOverMRt - b.estimatedOverMRt;
			if(tmpOverM < 0) return 1;
			if(tmpOverM > 0) return -1;
			double tmpMoreTasks = a.moreTasks - b.moreTasks;
			if(tmpMoreTasks < 0) return 1;
			if(tmpMoreTasks > 0) return -1;
			double tmpTau = a.tau - b.tau;
			if(tmpTau < 0) return 1;
			if(tmpTau > 0) return -1;
			double tmpElapsed = a.elapsed - b.elapsed;
			if(tmpElapsed < 0) return -1;
			if(tmpElapsed > 0) return 1;
			return 0;					
		}
	});
	
	Task nextTaskToMigrate = candidates.get(0);
	//System.out.println("Found task to migrate: " + nextTaskToMigrate.id);
	
	return nextTaskToMigrate;
}

private void replicate(Node fs, ArrayList<Node> slow, ArrayList<Task> tasks) {
	ArrayList<Task> candidates = new ArrayList<Task>();
	for(Node sl : slow) {
		if((!sl.nodeFinished) && (!(sl.task.replicated || sl.task.migrated))) {
			if(isReplicationCandidate(sl.task,fs,sl)) {
				candidates.add(sl.task);
			}
		}
	}
	if(candidates.size() ==0) {
		fs.nodeFinished = true;
		fs.task = null;
		return;
	}	
	
	Task nextTaskToReplicate = selectTaskToReplicate(candidates);
	System.out.println("Replicating task: " + nextTaskToReplicate.id);
	nextTaskToReplicate.replicated = true;
	fs.task = nextTaskToReplicate;
	fs.initialRt = nextTaskToReplicate.rt/speedFactor;
	fs.crtJob = fs.initialRt;
	fs.noTasksReplicated ++;
	
}

private Task selectTaskToReplicate(ArrayList<Task> candidates) {
	
	return selectReplicationTaskLargestExpectedRemainingRt(candidates);
	//return selectReplicationTaskLargestExpectedRtOnTargetNode(candidates);
	//return selectTaskLargestTauSmallestElapsedETFirst(candidates);
	//return selectReplicationTaskLargestSuspectedOverMRtLargestTauSmallestElapsedETFirst(candidates);
}

private Task selectReplicationTaskLargestExpectedRemainingRt(ArrayList<Task> candidates) {

	if(knowledge == RANDOM) {
		Task nextTaskToReplicate = candidates.get(candidateSelector.nextInt(candidates.size()));
		return nextTaskToReplicate;
	}
	Collections.sort(candidates, new Comparator<Task>(){
		public int compare(Task a, Task b) {			
			double tmp = (a.estimatedRt - a.elapsed) - (b.estimatedRt - b.elapsed);
			if(tmp < 0) return 1;
			if(tmp > 0) return -1;
			return 0;					
		}
	});

	Task nextTaskToReplicate = candidates.get(0);	
	return nextTaskToReplicate;

}

/*LargestExpectedRtOnTargetNode = task.tau*/

private Task selectReplicationTaskLargestExpectedRtOnTargetNode(ArrayList<Task> candidates) {
	if(knowledge == RANDOM) {
		Task nextTaskToReplicate = candidates.get(candidateSelector.nextInt(candidates.size()));
		
		return nextTaskToReplicate;
	}
	Collections.sort(candidates, new Comparator<Task>(){
		public int compare(Task a, Task b) {			
			double tmp = a.tau - b.tau;
			if(tmp < 0) return 1;
			if(tmp > 0) return -1;
			return 0;					
		}
	});
	
	Task nextTaskToReplicate = candidates.get(0);
	
	return nextTaskToReplicate;

}

private Task selectTaskLargestTauSmallestElapsedETFirst(
		ArrayList<Task> candidates) {
	if(knowledge == RANDOM) {
		Task nextTaskToReplicate = candidates.get(candidateSelector.nextInt(candidates.size()));
		
		return nextTaskToReplicate;
	}
	Collections.sort(candidates, new Comparator<Task>(){
		public int compare(Task a, Task b) {
			double tmp = a.tau - b.tau;
			if(tmp < 0) return 1;
			if(tmp > 0) return -1;
			double tmpElapsed = a.elapsed - b.elapsed;
			if(tmpElapsed < 0) return -1;
			if(tmpElapsed > 0) return 1;
			return 0;					
		}
	});
	
	Task nextTaskToReplicate = candidates.get(0);
	
	return nextTaskToReplicate;

}

private Task selectReplicationTaskLargestSuspectedOverMRtLargestTauSmallestElapsedETFirst(
		ArrayList<Task> candidates) {
	if(knowledge == RANDOM) {
		Task nextTaskToReplicate = candidates.get(candidateSelector.nextInt(candidates.size()));
		
		return nextTaskToReplicate;
	}
	Collections.sort(candidates, new Comparator<Task>(){
		public int compare(Task a, Task b) {
			double tmpOverM = a.estimatedOverMRt - b.estimatedOverMRt;
			if(tmpOverM < 0) return 1;
			if(tmpOverM > 0) return -1;
			double tmp = a.tau - b.tau;
			if(tmp < 0) return 1;
			if(tmp > 0) return -1;
			double tmpElapsed = a.elapsed - b.elapsed;
			if(tmpElapsed < 0) return -1;
			if(tmpElapsed > 0) return 1;
			return 0;					
		}
	});
	
	Task nextTaskToReplicate = candidates.get(0);
	
	return nextTaskToReplicate;

}

private boolean isReplicationCandidateMultipleClusters(Task t, Node source, Node target) {
	/*do not allow replication on machines of the same type*/
	if((!t.notYetFinished) || (source.crtJob == 0)) {
		if(printDebug) {
			System.out.println("task " + t.id + " finished -> not a replication candidate");
		}
		return false;
	}
	if(target.clusterName.equals(source.clusterName)) return false;
	if(knowledge == RANDOM) {
		if(t.replicated) {
			for(Node replica : t.replicas.values()) {		
				if((replica.crtJob == 0) || (replica.nodeFinished)) {
					System.out.println("KRandom: task " + t.id + " finished -> not a replication candidate");
					return false;
				}
			}
		}
		return true;
	}
	
	double elapsedET;
	double estimatedTETSource;
	double estimatedTETTarget;
	SimulatedCluster targetC, sourceC;	
	
	if(t.replicated) {
		double minEstimatedRemainingRt, minElapsedET, minEstimatedTETSource;				
		SimulatedCluster minSourceC;		
		sourceC = clusters.get(t.primary.clusterName);
		targetC = clusters.get(target.clusterName);
		
		elapsedET = t.primary.initialRt - t.primary.crtJob;	
		estimatedTETSource = estimateExecutionTimeMultipleClusters(t, elapsedET, sourceC);
	
		if(printDebug) {		
			System.out.println("(already replicated) job " + t.id + " primary type " + sourceC.name 
					+ " has elapsed time: " + (double)(elapsedET) + 
					  " , ETotalETime (sec):" + estimatedTETSource);
		}
		
		minEstimatedRemainingRt = estimatedTETSource - elapsedET;
		minElapsedET = elapsedET;
		minEstimatedTETSource = estimatedTETSource;
		minSourceC = sourceC;
		
		for(Node replica : t.replicas.values()) {			
			sourceC = clusters.get(replica.clusterName);
			elapsedET = replica.initialRt - replica.crtJob;	
			estimatedTETSource = estimateExecutionTimeMultipleClusters(t, elapsedET, sourceC);
			if(printDebug) {		
				System.out.println("job " + t.id + " on replica type " + sourceC.name 
						+ " has crtJob: " + replica.crtJob + " elapsed time: " + (double)(elapsedET) + 
						  " , ETotalETime (sec):" + estimatedTETSource);
			}
			if((replica.crtJob == 0) || (replica.nodeFinished)) {
				return false;
			}
			if(estimatedTETSource - elapsedET < minEstimatedRemainingRt) {
				minEstimatedRemainingRt = estimatedTETSource - elapsedET;
				minElapsedET = elapsedET;
				minEstimatedTETSource = estimatedTETSource;			
				minSourceC = sourceC;
			}		
		}
		
		t.estimatedRt = minEstimatedTETSource;
		t.elapsed = minElapsedET;
		estimatedTETTarget = convertExecutionTimeMultipleClusters(estimatedTETSource, minSourceC, targetC);
		if(estimatedTETTarget < minEstimatedRemainingRt) {		
			t.tau = estimatedTETTarget;						
			return true;
		}			
		return false;
	}
	
			
	targetC = clusters.get(target.clusterName);
	sourceC = clusters.get(source.clusterName);
	/*do not allow replication on machines of the same type*/
	
	elapsedET = source.initialRt - source.crtJob;	
	estimatedTETSource = estimateExecutionTimeMultipleClusters(t, elapsedET, sourceC);
	
	if(printDebug) {		
		System.out.println("job " + t.id + " on current type " + source.clusterName 
				+ " has elapsed time: " + (double)(elapsedET) + 
				  " , ETotalETime (sec):" + estimatedTETSource);
	}
	
	estimatedTETTarget = convertExecutionTimeMultipleClusters(estimatedTETSource, sourceC, targetC);
	
	/*if the estimatedTarget threatens to go over the nATU of 
	 * target machine, not a replication candidate
	if(fs.totalRt + estimatedTarget > fs.nATU*60) 
		return false;
	*/
	/*replication should also give priority to tasks
	 * threatening the deadline*/	
	/*
	if(source.totalRt + (estimatedTETSource - elapsedET) > source.nATU*60) {
		source.task.estimatedOverMRt = source.totalRt + (estimatedTETSource - elapsedET) - source.nATU*60;
		t.estimatedRt = estimatedTETSource;
		t.tau = estimatedTETTarget;
		t.elapsed = elapsedET;		
		return true;
	}*/
	if(estimatedTETTarget < (estimatedTETSource-elapsedET)) {	
		t.estimatedRt = estimatedTETSource;
		t.tau = estimatedTETTarget;			
		t.elapsed = elapsedET;
		return true;
	}			
	return false;
}

private boolean isMigrationCandidateMultipleClusters(Task t, Node source, Node target) {
	if((!t.notYetFinished) || (source.crtJob == 0)) {
		if(printDebug) {
			System.out.println("task " + t.id + " finished -> not a migration candidate");
		}
		return false;
	}
	if(source.clusterName.equals(target.clusterName)) return false;
	
	if(knowledge == RANDOM) return true;
	
	SimulatedCluster sourceC = clusters.get(source.clusterName);
	SimulatedCluster targetC = clusters.get(target.clusterName);
	
	double elapsedET = source.initialRt-source.crtJob;
	double elapsedETBasic = convertExecutionTimeBasicCluster(elapsedET, sourceC);
	
	
	//if(!emergencyOnly) System.out.println("Enter estimateET(elapsedET)");
	
	double estimatedSource = estimateExecutionTimeMultipleClusters(t, elapsedET, sourceC);
	//if(!emergencyOnly) System.out.println("estimatedSource: " + estimatedSource);
	//System.out.println("real ET: " + sl.initialRt);
	double estimatedTarget = convertExecutionTimeMultipleClusters(estimatedSource, sourceC, targetC);
	
	/*if the estimatedTarget threatens to go over the nATU of 
	 * target machine, not a migration candidate
	if(fs.totalRt + estimatedTarget > fs.nATU*60) 
		return false;
	*/
	/*emergency clause*/
	if(emergencyOnly) {
		if(source.totalRt + (estimatedSource - elapsedET) > source.nATU*60) {
			source.task.estimatedOverMRt = source.totalRt + (estimatedSource - elapsedET) - source.nATU*60;
			return true;
		}
		else {
			return false;
		}
	} else {
		/*
		if(source.totalRt + (estimatedSource - elapsedET) > source.nATU*60) {
			t.tau = estimatedTarget;
			t.elapsed = elapsedET;		
			t.estimatedRt = estimatedSource;
			double rtHighFreqSourceC = getMostPopularRT(elapsedETBasic)/sourceC.speedFactor;
			double tLeftATUOld = source.nATU*60-source.totalRt-elapsedET;
			double tLeftATUNew = target.nATU*60-target.totalRt-estimatedTarget;
			source.task.estimatedOverMRt = source.totalRt + (estimatedSource - elapsedET) - source.nATU*60;
			t.moreTasks = Math.floor(tLeftATUOld / rtHighFreqSourceC) 
						+ Math.floor(tLeftATUNew/convertExecutionTimeMultipleClusters(rtHighFreqSourceC, sourceC, targetC)) 
						- (Math.floor((tLeftATUOld-(estimatedSource - elapsedET))/rtHighFreqSourceC) 
						   + Math.floor((tLeftATUNew+estimatedTarget)/convertExecutionTimeMultipleClusters(rtHighFreqSourceC, sourceC, targetC)));
			return true;
		} else {
		 	*/
			/*no violation signaled, 
			 * but migrating the task would produce more 
			 * possible tasks done in general*/
			/*
			t.tau = estimatedTarget;
			t.elapsed = elapsedET;
			t.estimatedRt = estimatedSource;		
			double rtHighFreqSourceC = getMostPopularRT(elapsedETBasic)/sourceC.speedFactor;
			//if(!emergencyOnly) System.out.println("Most popular RT: " + rtHighFreq); 
			double tLeftATUOld = source.nATU*60-source.totalRt-elapsedET;
			double tLeftATUNew = target.nATU*60-target.totalRt-estimatedTarget;
			if((Math.floor(tLeftATUOld / rtHighFreqSourceC) + Math.floor(tLeftATUNew/ convertExecutionTimeMultipleClusters(rtHighFreqSourceC, sourceC, targetC))) 
					> (Math.floor((tLeftATUOld-(estimatedSource - elapsedET))/rtHighFreqSourceC) 
							+ Math.floor((tLeftATUNew+estimatedTarget)/convertExecutionTimeMultipleClusters(rtHighFreqSourceC, sourceC, targetC)))) {
				//System.out.println("Found migration candidate task: " + t.id);
				t.moreTasks = Math.floor(tLeftATUOld / rtHighFreqSourceC) + Math.floor(tLeftATUNew/convertExecutionTimeMultipleClusters(rtHighFreqSourceC, sourceC, targetC)) -
							 (Math.floor((tLeftATUOld-(estimatedSource - elapsedET))/rtHighFreqSourceC) + 
							  Math.floor((tLeftATUNew+estimatedTarget)/convertExecutionTimeMultipleClusters(rtHighFreqSourceC, sourceC, targetC)));
				return true;
			} else { 
				return false;
			}
		}
		*/
		if(estimatedTarget < (estimatedSource-elapsedET)) {	
			t.estimatedRt = estimatedSource;
			t.tau = estimatedTarget;			
			t.elapsed = elapsedET;
			return true;
		}			
		return false;
	}
}

private boolean isReplicationCandidate(Task t, Node fs, Node sl) {
	
	double elapsedET = sl.initialRt-sl.crtJob;
	double estimatedSource = estimateExecutionTime(elapsedET);
	//System.out.println("real ET: " + sl.initialRt);
	double estimatedTarget = convertExecutionTime(estimatedSource);
	
	/*if the estimatedTarget threatens to go over the nATU of 
	 * target machine, not a replication candidate
	if(fs.totalRt + estimatedTarget > fs.nATU*60) 
		return false;
	*/
	/*replication should also give priority to tasks
	 * threatening the deadline*/	
	if(sl.totalRt + (estimatedSource - elapsedET) > sl.nATU*60) {
		sl.task.estimatedOverMRt = sl.totalRt + (estimatedSource - elapsedET) - sl.nATU*60;
		t.tau = estimatedTarget;
		t.elapsed = elapsedET;
		return true;
	}
	if(estimatedTarget < (estimatedSource - elapsedET) ) {
		t.tau = estimatedTarget;
		t.elapsed = elapsedET;
		return true;
	}
	return false;
}

private boolean isMigrationCandidate(Task t, Node fs, Node sl) {
	
	double elapsedET = sl.initialRt-sl.crtJob;
	//if(!emergencyOnly) System.out.println("Enter estimateET(elapsedET)");
	double estimatedSource = estimateExecutionTime(elapsedET);
	//if(!emergencyOnly) System.out.println("estimatedSource: " + estimatedSource);
	//System.out.println("real ET: " + sl.initialRt);
	double estimatedTarget = convertExecutionTime(estimatedSource);
	
	/*if the estimatedTarget threatens to go over the nATU of 
	 * target machine, not a migration candidate
	if(fs.totalRt + estimatedTarget > fs.nATU*60) 
		return false;
	*/
	/*emergency clause*/
	if(emergencyOnly) {
		if(sl.totalRt + (estimatedSource - elapsedET) > sl.nATU*60) {
			sl.task.estimatedOverMRt = sl.totalRt + (estimatedSource - elapsedET) - sl.nATU*60;
			return true;
		}
		else {
			return false;
		}
	} else {	
		if(sl.totalRt + (estimatedSource - elapsedET) > sl.nATU*60) {
			t.tau = estimatedTarget;
			t.elapsed = elapsedET;		
			double rtHighFreq = getMostPopularRT(elapsedET);
			double tLeftATUOld = sl.nATU*60-sl.totalRt-elapsedET;
			double tLeftATUNew = fs.nATU*60-fs.totalRt-estimatedTarget;
			sl.task.estimatedOverMRt = sl.totalRt + (estimatedSource - elapsedET) - sl.nATU*60;
			t.moreTasks = Math.floor(tLeftATUOld / rtHighFreq) + Math.floor(tLeftATUNew/convertExecutionTime(rtHighFreq)) -
			 (Math.floor((tLeftATUOld-(estimatedSource - elapsedET))/rtHighFreq) + 
			  Math.floor((tLeftATUNew+estimatedTarget)/convertExecutionTime(rtHighFreq)));
			return true;
		} else {
			/*no violation signaled, 
			 * but migrating the task would produce more 
			 * possible tasks done in general*/
			t.tau = estimatedTarget;
			t.elapsed = elapsedET;		
			double rtHighFreq = getMostPopularRT(elapsedET);
			//if(!emergencyOnly) System.out.println("Most popular RT: " + rtHighFreq); 
			double tLeftATUOld = sl.nATU*60-sl.totalRt-elapsedET;
			double tLeftATUNew = fs.nATU*60-fs.totalRt-estimatedTarget;
			if((Math.floor(tLeftATUOld / rtHighFreq) + Math.floor(tLeftATUNew/ convertExecutionTime(rtHighFreq))) 
					> (Math.floor((tLeftATUOld-(estimatedSource - elapsedET))/rtHighFreq) 
							+ Math.floor((tLeftATUNew+estimatedTarget)/convertExecutionTime(rtHighFreq)))) {
				//System.out.println("Found migration candidate task: " + t.id);
				t.moreTasks = Math.floor(tLeftATUOld / rtHighFreq) + Math.floor(tLeftATUNew/convertExecutionTime(rtHighFreq)) -
							 (Math.floor((tLeftATUOld-(estimatedSource - elapsedET))/rtHighFreq) + 
							  Math.floor((tLeftATUNew+estimatedTarget)/convertExecutionTime(rtHighFreq)));
				return true;
			} else { 
				return false;
			}		
		}
	}
}



public double getMostPopularRT(double elapsedET) {
	
	int popRTOccurrence = Integer.MIN_VALUE;
	double popRT = 0.0;
	Set<Map.Entry<Integer,Occurrence>> setHisto = histogram.entrySet();
	for(Map.Entry<Integer,Occurrence> r : setHisto) {
		if(r.getValue().occurrence > popRTOccurrence) {
			popRTOccurrence = r.getValue().occurrence;
			popRT = r.getKey().doubleValue();
		}
	}
	return popRT;
}

public double estimateExecutionTime(double elapsedET) {
	
	double estimate = 0.0;
	int noTailTasks = 0;
	
	NavigableMap<Integer,Occurrence> tailHistoNM = histogram.tailMap(new Integer((int)elapsedET),false);
	Set<Map.Entry<Integer,Occurrence>> setTailHisto = tailHistoNM.entrySet();
	for(Map.Entry<Integer,Occurrence> r : setTailHisto) {
		estimate +=  r.getKey().doubleValue()*r.getValue().occurrence;
		noTailTasks += r.getValue().occurrence;
	}
	if(noTailTasks != 0) { 
		estimate = estimate / noTailTasks;
		return estimate;
	}
	return elapsedET;
}


private double convertExecutionTime(double estimatedSource) {
	
	return estimatedSource/speedFactor;
}


private double estimateExecutionTimeMultipleClusters(Task t, double elapsedET, SimulatedCluster simCluster) {
	if(knowledge == PERFECT) return t.rt/simCluster.speedFactor;
	
	double estimate = 0.0;
	int noTailTasks = 0;
	
	/*the histogram maintains the runtimes as executed 
	 * on the base type cluster (sf=1,cf=1)
	 * and must be converted back and forth*/
	
	NavigableMap<Integer,Occurrence> tailHistoNM = histogram.tailMap(new Integer((int)(elapsedET*simCluster.speedFactor)),false);
	Set<Map.Entry<Integer,Occurrence>> setTailHisto = tailHistoNM.entrySet();
	for(Map.Entry<Integer,Occurrence> r : setTailHisto) {
		estimate +=  r.getKey().doubleValue()/simCluster.speedFactor*r.getValue().occurrence;
		noTailTasks += r.getValue().occurrence;
		if(printTailMap) System.out.print(" (" + r.getKey().doubleValue() + "," + r.getValue().occurrence + ") ");
	}
	if(printTailMap) System.out.println();
	if(noTailTasks != 0) { 
		estimate = estimate / noTailTasks;
		return estimate;
	}
	return elapsedET;
}

private double convertExecutionTimeMultipleClusters(double estimatedSource, 
		SimulatedCluster sourceCluster, SimulatedCluster targetCluster) {
	
	return estimatedSource*sourceCluster.speedFactor/targetCluster.speedFactor;
}

private double convertExecutionTimeBasicCluster(double estimatedSource, 
		SimulatedCluster sourceCluster) {
	return estimatedSource*sourceCluster.speedFactor;
}

public double erfc(double x) {
	return 2*Phi(-Math.sqrt(2)*x);
}

// return phi(x) = standard Gaussian pdf
	public double phi(double x) {
	return Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI);
}

// return Phi(z) = standard Gaussian cdf using Taylor approximation
	public double Phi(double z) {
	if (z < -8.0) return 0.0;
	if (z >  8.0) return 1.0;
	double sum = 0.0, term = z;
	for (int i = 3; sum + term != sum; i += 2) {
		sum  = sum + term;
		term = term * z * z / i;
	}
	return 0.5 + sum * phi(z);
}

	public void print() {
		for(int i=0; i<size; i++) {
			System.out.println(x.get(i));
		}
	}	
}


