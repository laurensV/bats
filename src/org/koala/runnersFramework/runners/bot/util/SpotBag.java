package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class SpotBag extends NormalDistributedBag {

	PriceFluctuationsSimulator priceSimulator;
	int counter = 0;
	
	// the budget with which the execution started
	// needed for rescheduling
	long originalBudget;

	public SpotBag(int size, double mean, double stdev, double speedFactor,
			int costFactor, int cost, PriceFluctuationsSimulator priceSimulator) {
		super(size, mean, stdev, speedFactor, costFactor, cost);
		this.priceSimulator = priceSimulator;
	}

	public SpotBag(int i, long experimentBagGeneratingSeed, int j, double d,
			int k, int l, int m, PriceFluctuationsSimulator priceSimulator, int maxItems) {
		super(i, experimentBagGeneratingSeed, j, d, k, l, m);
		this.priceSimulator = priceSimulator;
		this.maxItems = maxItems;
	}

	@Override
	public SpotBag copy() {
		SpotBag tmp = new SpotBag(size, mean, stdev, speedFactor, costFactor,
				cost, priceSimulator);
		for (Double j : this.x) {
			tmp.x.add(j.doubleValue());
		}
		tmp.realExpectation = this.realExpectation;
		tmp.realVariance = this.realVariance;
		tmp.printXLS = this.printXLS;
		tmp.printSampleSummary = this.printSampleSummary;
		tmp.printNodeStats = this.printNodeStats;
		tmp.maxItems = this.maxItems;
		return tmp;
	}

	@Override
	public void setClusters(HashMap<String, SimulatedCluster> clusters, long sampleSeed, int sampleSize) {
		this.clusters = clusters;
		sampleMultipleClusters(sampleSeed, sampleSize);
		SimulatedCluster mostProfitable = selectMostProfitable();
		for(SimulatedCluster simCluster : clusters.values()) {
			//simCluster.cost = simCluster.costFactor*cost;
			((SimulatedSpotCluster)simCluster).setBiddingPrice(mostProfitable);
		}
		
	}
	
	@Override
	public SpotBag copyMC() {
		SpotBag tmp = copy();
		tmp.clusters = (HashMap<String, SimulatedCluster>) clusters.clone();
		return tmp;
	}

	/*@Override
	public void updateDistributionParameterEstimates(double rt) {

	}*/

	@Override
	public SimulatedExecution executeMultipleClustersMoreDetails(
			long executorSeed, SimulatedSchedule sched) {

		if (sched.atus == Integer.MAX_VALUE)
			return new SimulatedExecution(Double.NaN, 0, 0);

		double violation = 0.0;

		double instanceHour = 0;

		HashMap<String, ArrayList<Node>> allNodes = new HashMap<String, ArrayList<Node>>();

		originalBudget = sched.budget;
		ArrayList<Task> tasks = new ArrayList<Task>();

		for (int i = 0; i < x.size(); i++) {
			tasks.add(new Task(i, x.get(i)));
		}

		/*
		 * in case we repeat executions with different tail strategies on the
		 * same bag (object)
		 */
		resetDistributionParameterEstimates();

		Random randomExecutor = new Random(executorSeed);

		for (String clusterName : sched.machinesPerCluster.keySet()) {

			ArrayList<Node> nodes = new ArrayList<Node>();
			allNodes.put(clusterName, nodes);
			SimulatedCluster cluster = clusters.get(clusterName);

			for (int i = 0; i < sched.machinesPerCluster.get(clusterName); i++) {
				Node node = new Node();
				Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size()));
				node.task = tmp;
				node.initialRt = tmp.rt / cluster.speedFactor;
				node.crtJob = node.initialRt;
				node.nATU = sched.atus;
				node.clusterName = clusterName;
				nodes.add(node);
			}
		}

		int doneTasks = 0;
		long cost = 0;
		

		while (x.size() > doneTasks) {
			// if(!emergencyOnly) System.out.println("doneTasks before cycle: "
			// + doneTasks);
			double minRt = Double.MAX_VALUE;
			Node minNode = null;
			boolean priceChange = false;

			for (String clusterName : sched.machinesPerCluster.keySet()) {
				if (!priceSimulator.hasTerminated(clusterName))
					for (Node node : allNodes.get(clusterName)) {
						if ((!node.nodeFinished) && (node.crtJob < minRt)) {
							minNode = node;
							minRt = node.crtJob;
						}
					}
			}
			
			if (minNode == null) {
				System.out.println("There are no more machines left to execute the remaining tasks.");
				break;
			}

			double minNextPriceChange = priceSimulator.getTimeUntilNextChange();
			
			if (minRt > minNextPriceChange) {
				minRt = minNextPriceChange;
				priceChange = true;
			}

			if (!priceChange) {
				minNode.doneJob();
				doneTasks++;
				
				if (tasks.size() > 0) {
					Task tmp = tasks.remove(randomExecutor.nextInt(tasks.size()));
					minNode.task = tmp;
					minNode.initialRt = tmp.rt
							/ clusters.get(minNode.clusterName).speedFactor;
					minNode.crtJob = minNode.initialRt;
				} else {
					minNode.nodeFinished = true;
				}
			}
			if (priceChange) {
				boolean reschedule = false;
				for (String clusterName : sched.machinesPerCluster.keySet()) {
					if (!priceSimulator.hasTerminated(clusterName) && ((SimulatedSpotCluster)clusters.get(clusterName)).strategy != Strategy.OnDemand) {
						int currentBidPrice = clusters.get(clusterName).cost;
						int currentSpotPrice = priceSimulator.getPrice(clusterName);
						
						//System.out.println("Current bid price " + currentBidPrice + " current spot price " + currentSpotPrice);
						//set machines on on demand if the spot price exceeds on demand price
						if (currentSpotPrice <= ((SimulatedSpotCluster)clusters.get(clusterName)).costOnDemand && currentBidPrice < currentSpotPrice) {
							// terminate machines from this type
							System.out.println("Terminating cluster " + clusterName);
							
							// running jobs return to bag
							for (Node node : allNodes.get(clusterName)){
								tasks.add(node.task);
							}
							
							//SimulatedSpotCluster cluster = (SimulatedSpotCluster) clusters.get(clusterName);
							//cluster.terminated = true;
							
							// removing cluster from price simulator
							//priceSimulator.clusters.remove(clusterName);
							
							reschedule = true;
						}
					}
				}
				
				//what happens with the clusters that were still running?
				//now the running clusters are ignored
				if (reschedule) {
					counter++;
					reschedule(sched, cost, randomExecutor, allNodes, tasks);
					/*for (String clusterName : sched.machinesPerCluster.keySet()) {
						if (priceSimulator.clusters.get(clusterName)==null) {
							//((SimulatedSpotCluster)clusters.get(clusterName)).terminated = false;
							//priceSimulator.clusters.put(clusterName, (SimulatedSpotCluster)clusters.get(clusterName));
						}
					}*/
				}
			}

			for (String clusterName : sched.machinesPerCluster.keySet()) {
				if (!priceSimulator.hasTerminated(clusterName)) 
					for (Node node : allNodes.get(clusterName)) {
						if (minNode == node)
							continue;
						if (node.nodeFinished)
							continue;
						node.crtJob -= minRt;
						if (node.crtJob == 0) {
							if (tasks.size() > 0) {
								node.doneJob();
								doneTasks++;

								Task tmp = tasks.remove(randomExecutor
										.nextInt(tasks.size()));
								node.task = tmp;
								node.initialRt = tmp.rt
										/ clusters.get(node.clusterName).speedFactor;
								node.crtJob = node.initialRt;
							} else {
								node.doneJob();
								doneTasks++;
								node.nodeFinished = true;
							}
						}
					}
			}

			
			instanceHour += minRt;

			//if either an hour passed or all the tasks are done
			if (instanceHour >= 3600 || x.size() <= doneTasks) {
				instanceHour -= 3600;
				// adding the cost for last instanceHour
				for (String clusterName : sched.machinesPerCluster.keySet()) {
					if (!priceSimulator.hasTerminated(clusterName)) {
						cost += priceSimulator.getPrice(clusterName)
						* sched.machinesPerCluster.get(clusterName);
					}
					//System.out.println(cost);
				}
			}
			
		//	System.out.println("cost" + cost);
			// update elapsed time after the cost for this instance hour has been computed
			// because otherwise the price index might increase 
			priceSimulator.updateElapsedTime(minRt);

		}
		
		

		
		violation = Double.NEGATIVE_INFINITY;

		overMnodes = 0;

		for(String clusterName : sched.machinesPerCluster.keySet()) {
			for(Node node : allNodes.get(clusterName)) {
				double nodeRt = node.totalRt; //cost +=
			//Math.ceil(nodeRt/3600)*clusters.get(clusterName).cost;
			if(printNodeStats) System.out.print(Math.ceil(nodeRt/3600)+"\t");
			if((nodeRt/60 - node.nATU*60) > violation) violation = nodeRt/60 -
					node.nATU*60; if(nodeRt/60 > node.nATU*60) { overMnodes ++; } }
			System.out.print("||"); }

		
		if (printNodeStats) {
			System.out.println("\nmakespan overdone: " + violation
					+ ", machines overdone: " + overMnodes + ", cost: " + cost + 
			 "\n tasks to be done: " + x.size() + "\n tasks done: " +
			 doneTasks);
		}

		double makespanMinutes = sched.atus * 60 + violation;
		System.out.println("Makespan: " + makespanMinutes + " min");
		return new SimulatedExecution(makespanMinutes, cost, overMnodes);
	}

	private void reschedule(SimulatedSchedule sched, long cost, Random r, HashMap<String, ArrayList<Node>> allNodes, ArrayList<Task> tasks) {
		System.out.println("Rescheduling");
		SimulatedSchedule sim = computeMakespanEstimateMultipleClusters(originalBudget-cost);
		sched = sim;
		System.out.println(sim);
		
		HashMap<String, ArrayList<Node>> newAllNodes = new HashMap<String, ArrayList<Node>>();

		ArrayList<Task> newTasks = new ArrayList<Task>();

		for (int i = 0; i < tasks.size(); i++) {
			newTasks.add(tasks.get(i));
		}
		/*
		 * in case we repeat executions with different tail strategies on the
		 * same bag (object)
		 */
		//resetDistributionParameterEstimates();
		
		
		for (String clusterName : sched.machinesPerCluster.keySet()) {

			ArrayList<Node> nodes = new ArrayList<Node>();
			newAllNodes.put(clusterName, nodes);
			SimulatedCluster cluster = clusters.get(clusterName);
			
			((SimulatedSpotCluster)cluster).setBiddingPrice(selectMostProfitable());
			for (int i = 0; i < sched.machinesPerCluster.get(cluster.name); i++) {
				Node node = new Node();
				if (newTasks.size() > 0) {
					Task tmp = newTasks.remove(r.nextInt(newTasks.size()));
					node.task = tmp;
					node.initialRt = tmp.rt / cluster.speedFactor;
					node.crtJob = node.initialRt;
					node.nATU = sched.atus;
					node.clusterName = clusterName;
					nodes.add(node);
				}
			}
		}
		tasks = newTasks;
		allNodes = newAllNodes;
	}
	
	/*@Override
	SimulatedCluster findCheapest() {
		SimulatedCluster cheapestCluster = null;
		double cheapest = Double.MAX_VALUE;
		for (SimulatedCluster simCluster : clusters.values()) {
			if(((SimulatedSpotCluster)simCluster).costOnDemand < cheapest) {
				cheapest = ((SimulatedSpotCluster)simCluster).costOnDemand;
				cheapestCluster = simCluster;
			} else if (((SimulatedSpotCluster)simCluster).costOnDemand == cheapest) {
				if(simCluster.Ti < cheapestCluster.Ti) {
					cheapestCluster = simCluster;
				}
			}
		}
		return cheapestCluster;
	}
	*/
}
