package org.koala.runnersFramework.runners.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;


public class EnergyMaster extends Master {
	private ArrayList<Job> replicatedTasks;

	Thread energyStatsThread;

	private double sampleCost;
	private long sampleMakespan; /*expressed in seconds*/
	public ArrayList<Schedule> schedules;

	// maxNoJobsToNodes = the degree of parallelism (number of threads) on that node
	private HashMap<String, Integer> indexMaxNoJobsToNodes;
	// current number of jobs from a batch sent/received for a node
	private HashMap<String, Integer> noJobsToNodes;
	private HashMap<String, Boolean> waitingNodes;

	private HashMap<String, Boolean> terminatedNodes;
	private int noTerminatedNodes;

	private HashMap<Cluster, Integer> totalSamplingSubmittedCounter;


	private EnergyStats energyStats;

	Random r = new Random();

	protected EnergyMaster(BoTRunner bot) throws Exception {
		super(bot);

		totalSamplingSubmittedCounter = new HashMap<Cluster, Integer>();

		schedules = new ArrayList<Schedule>();
		noJobsToNodes = new HashMap<String, Integer>();
		indexMaxNoJobsToNodes = new HashMap<String, Integer>();
		waitingNodes = new HashMap<String, Boolean>();
		terminatedNodes = new HashMap<String, Boolean>();

		bot.finishedTasks = new ArrayList<Job>();

		replicatedTasks = new ArrayList<Job>();
		Random randomSample = new Random(1111111111L);

	//	for(int i = 0; i < bot.noReplicatedJobs; i++) {
	//		replicatedTasks.add(bot.tasks.remove(randomSample.nextInt(bot.tasks.size())));
	//	}

		//formula for sampling with replacement
		double zeta_sq = bot.zeta * bot.zeta;
		bot.noSampleJobs = (int) Math.ceil(bot.tasks.size() * zeta_sq
				/ (zeta_sq + 2 * (bot.tasks.size() - 1) * bot.delta * bot.delta));		

		System.out.println("Sample size is: " + bot.noSampleJobs);

		//if(bot.noSampleJobs < bot.noReplicatedJobs || (bot.noSampleJobs*bot.Clusters.size() > 0.5 * totalNumberTasks)) {
	//		System.out.println("Bag too small!");
	//		System.exit(0);
	//	}

		Collection<Cluster> clusters = bot.Clusters.values();

		bot.noInitialWorkers = 1;
		this.noTerminatedNodes = 0;

		Cluster cheapest = findCheapest();

		bot.minCostATU = Integer.MAX_VALUE;
		bot.maxCostATU = 0;

		for (Cluster cluster : clusters) {
			HashMap<String, WorkerStats> workersCluster = new HashMap<String, WorkerStats>();
			System.out.println(cluster.alias);
			System.out.println(cluster.hostname);
			workers.put(cluster.alias, workersCluster);

			cluster.setCrtNodes(0);
			cluster.setPendingNodes(bot.noInitialWorkers);
			cluster.setNecNodes(bot.noInitialWorkers);
		}
	}



	private Cluster findCheapest() {
		Cluster cheapestCluster = null;
		double cheapest = Double.MAX_VALUE;
		for (Cluster cluster : bot.Clusters.values()) {
			if(cluster.costUnit < cheapest) {
				cheapest = cluster.costUnit;
				cheapestCluster = cluster;
			} else if (cluster.costUnit == cheapest) {
				if(cluster.Ti < cheapestCluster.Ti) {
					cheapestCluster = cluster;
				}
			}
		}
		return cheapestCluster;
	}

	@Override
	protected boolean areWeDone() {
		Collection<Integer> values = noJobsToNodes.values();
		for (Integer value: values) {
			if (value != 0) {

				return false;
			}
		}

		/*values = indexMaxNoJobsToNodes.values();
		for (Integer value : values) {
			System.out.println("value " + value.intValue() );
			System.out.println("value " + value);
			if (value.intValue() != -1) {
				System.out.println("second false");
				return false;
			}
		}*/

		if (noTerminatedNodes < indexMaxNoJobsToNodes.size()) {
			return false;
		}



		return true;
	}

	@Override
	protected Job handleJobRequest(IbisIdentifier from) {
		String clusterName = from.location().getParent().toString();
		String node = from.location().getLevel(0);
		String clusterAlias = EnergyConst.allNodes.get(node);
		Cluster cluster = bot.Clusters.get(clusterAlias);

		/*DEBUG*/
		System.out.println("job request from node " + from.location().toString() + " in cluster " + clusterAlias);

		WorkerStats reacquiredMachine = workers.get(clusterAlias).get(node);
		if(reacquiredMachine == null) {
			workers.get(clusterAlias).put(node, new WorkerStats(node, System.currentTimeMillis(), from));
		} else {
			reacquiredMachine.reacquire(cluster.timeUnit, System.currentTimeMillis());
		}	

		cluster.setCrtNodes(cluster.getCrtNodes()+1);
		cluster.setPendingNodes(cluster.getPendingNodes()-1);

		//decideReferenceCluster();

		return findNextJob(cluster, from);
	}

	@Override
	protected Job handleJobResult(JobResult received, IbisIdentifier from) {

		String node = from.location().getLevel(0);
		String clusterName = from.location().getParent().toString();			
		String clusterAlias = EnergyConst.allNodes.get(node);
		Cluster cluster = bot.Clusters.get(clusterAlias);

		System.out.println(from.location().toString() + " returned result of job " + 
				received.getJobID() + " executed for (sec)" + received.getStats().getRuntime()/1000000000);

		// find the job 
		int maxJobs = EnergyConst.maxJobsPerNodeList[indexMaxNoJobsToNodes.get(node)];
		int noJobs = noJobsToNodes.get(node);
		//int samplingCounter = 0;

		if (cluster instanceof DAS4Cluster) {
			DAS4Cluster c = (DAS4Cluster) cluster;

			VirtualCluster vc;
			if (c.virtualCls.get(maxJobs) == null) {
				vc = new VirtualCluster(c, maxJobs);
				c.virtualCls.put(maxJobs, vc);
			} else {
				vc = c.virtualCls.get(maxJobs);
			}

			Job doneJob = vc.regressionPoints.get(received.getJobID()); 

			if (doneJob == null) {
				doneJob = vc.samplingPoints.get(received.getJobID());
				if (doneJob == null) {
					System.out.println("done job should not be null + maxNoJobs " + maxJobs);
				} else {
					vc.samplingPointsCounter++;
				}
			} else {
				vc.replicatedTasksCounter++;
			}

			doneJob.runtimes.put(vc.alias, new Double(received.getStats().getRuntime()/1000000000)) ;
			doneJob.done = true;

			bot.finishedTasks.add(doneJob);
		}

		noJobs--;
		noJobsToNodes.put(node, noJobs);

		if (noJobs == 0 && terminatedNodes.get(node) != null && terminatedNodes.get(node) == true) {
			DAS4DoubleNode dNode = EnergyConst.isPartOfDoubleNode(node);

			for (int i = 0; i < energyStats.nodes.size(); i++) {
				if (dNode == null && !(energyStats.nodes.get(i) instanceof DAS4DoubleNode) && energyStats.nodes.get(i).name.equals(node)) {
					energyStats.pduThreads.get(i).kill();
				} else {
					if (dNode != null && terminatedNodes.get(dNode.name1) != null && 
							terminatedNodes.get(dNode.name1) == true && 
							terminatedNodes.get(dNode.name2) != null &&
							terminatedNodes.get(dNode.name2) == true) {
						if (energyStats.nodes.get(i) instanceof DAS4DoubleNode) {
							DAS4DoubleNode dNodeFound = (DAS4DoubleNode) energyStats.nodes.get(i); 
							if (dNodeFound.name1.equals(dNode.name1) && dNodeFound.name2.equals(dNode.name2)) {
								System.out.println("killing double node " + dNodeFound.name1 + " " + dNodeFound.name2);
								energyStats.pduThreads.get(i).kill();
							}

						}
					}
				}

			}
		}

		workers.get(clusterAlias).get(node).addJobStats(received.getStats().getRuntime());

		return null;	
	}

	private Job findNextJob(Cluster cluster, IbisIdentifier from) {
		String node = from.location().getLevel(0);

		Job nextJob = null;
		if (bot.tasks.size() == 0) {
			nextJob = sayGB(from);
		}

		int noJobs = noJobsToNodes.get(node);
		int maxJobs = EnergyConst.maxJobsPerNodeList[indexMaxNoJobsToNodes.get(node)];


		System.out.println("node " + node + " noJobs " + noJobs + " maxJobs " + maxJobs);

		Boolean isWaiting = waitingNodes.get(node);
		if (isWaiting != null && isWaiting.booleanValue() == true) {
			return new NoJob(EnergyConst.wait);
		}

		if (noJobs < maxJobs) {
			if (cluster instanceof DAS4Cluster) {
				DAS4Cluster c = (DAS4Cluster) cluster; 

				VirtualCluster vc;
				if (c.virtualCls.get(maxJobs) == null) {

					vc = new VirtualCluster(c, maxJobs);
					c.virtualCls.put(maxJobs, vc);
				} else {
					vc = c.virtualCls.get(maxJobs);
				}

				// send regression tasks
				
					Integer count = totalSamplingSubmittedCounter.get(vc);
					if (count == null) {
						totalSamplingSubmittedCounter.put(vc, 0);
						count = 0;
					}

					System.out.println("count " + count + " node " + node);
					DAS4DoubleNode dNode = EnergyConst.isPartOfDoubleNode(node);
					if (dNode != null) {
						if (count.intValue() < (int) Math.max(maxJobs * 2, bot.noSampleJobs)) {
							System.out.println("sending sampling task");
							nextJob = bot.tasks.remove(random.nextInt(bot.tasks.size()));
							vc.samplingPoints.put(nextJob.jobID, nextJob);
							totalSamplingSubmittedCounter.put(vc, count.intValue()+1);
							noJobsToNodes.put(node, noJobs+1);
						} else {
							if (count != null && noJobs == 0 ) {
								System.out.println("double node waiting: " + node);
								waitingNodes.put(node, true);
								Boolean isNode1Waiting = waitingNodes.get(dNode.name1);
								Boolean isNode2Waiting = waitingNodes.get(dNode.name2);
								if (isNode1Waiting != null && isNode2Waiting != null) {
									if (isNode1Waiting == true && isNode2Waiting == true) {
										increaseNoJobs(dNode.name1);
										increaseNoJobs(dNode.name2);
										System.out.println("end Time " + vc.noThreads);
										//vc.endTime = System.currentTimeMillis();
										waitingNodes.put(dNode.name1, false);
										waitingNodes.put(dNode.name2, false);
										//totalSamplingSubmittedCounter.put(c, 0);
									}
								}	
							}
							nextJob = new NoJob(EnergyConst.wait);
						}
					} else {
						if (count.intValue() < (int) Math.max(maxJobs, bot.noSampleJobs) ) {
							System.out.println("sending sampling task");
							nextJob = bot.tasks.remove(random.nextInt(bot.tasks.size()));
							vc.samplingPoints.put(nextJob.jobID, nextJob);
							totalSamplingSubmittedCounter.put(vc, count.intValue()+1);
							noJobsToNodes.put(node, noJobs+1);
						} else {
							if (count != null && noJobs == 0 ) {
								increaseNoJobs(node);
								//vc.endTime = System.currentTimeMillis();
							}
							nextJob = new NoJob(EnergyConst.wait);
						}
					}
				}
			
		} else if (EnergyConst.maxJobsPerNodeList[indexMaxNoJobsToNodes.get(node)] >= EnergyConst.maxJobsPerNode) { 
			nextJob = sayGB(from);
			if (cluster instanceof DAS4Cluster) {
				DAS4Cluster c = (DAS4Cluster) cluster; 
				VirtualCluster vc = c.virtualCls.get(maxJobs);
			//	vc.endTime = System.currentTimeMillis();
			}
			
		} else {
			//System.out.println("send pause");
			nextJob = new NoJob(EnergyConst.pause);	
		}

		return nextJob;
	}

	public void terminateWorker(Cluster cluster, WorkerStats ws, String reason) {
	}


	private Job sayGB (IbisIdentifier to) {
		System.err.println("We say goodbye to " + to.location().toString());

		String cluster = to.location().getParent().toString();
		String node = to.location().getLevel(0);
		String clusterAlias = EnergyConst.allNodes.get(node);
		
		workers.get(clusterAlias).get(node).workerFinished(System.currentTimeMillis());
		workers.get(clusterAlias).get(node).setLatestJobStartTime(0);
		bot.Clusters.get(clusterAlias).setCrtNodes(bot.Clusters.get(clusterAlias).getCrtNodes()-1);
		try {
			bot.Clusters.get(clusterAlias).terminateNode(workers.get(clusterAlias).get(node).getIbisIdentifier(), myIbis);
		} catch (IOException e) {
			e.printStackTrace();
		}
		noTerminatedNodes++;
		terminatedNodes.put(node, true);
		return new NoJob(EnergyConst.terminate);
	}

	@Override
	protected void handleLostConnections() {
		/*String clusterName;
		String node;
		Cluster cluster;  
		for(SendPortIdentifier lost : masterRP.lostConnections()) {
			cluster = bot.Clusters.get(lost.ibisIdentifier().location().getParent().toString());
			clusterName = lost.ibisIdentifier().location().getParent().toString();
			node = lost.ibisIdentifier().location().getLevel(0);
			if(! workers.get(clusterName).get(node).isFinished()) {
				String jobID = findFailedJob(clusterName,node);
				workers.get(clusterName).get(node).workerFinished(System.currentTimeMillis());
				cluster.setCrtNodes(cluster.getCrtNodes()-1);
			}
		}*/
	}

	@Override
	public void run() {
		boolean undone = true;
		timeout = (long) (BoTRunner.INITIAL_TIMEOUT_PERCENT * bot.deadline * 60000);
		System.out.println("Timeout is now " + timeout);

		long actualStartTime = System.currentTimeMillis();

		assignJobs();

		while (undone) {
			try {
				ReadMessage rm = masterRP.receive(30000);

				Object received = rm.readObject();
				IbisIdentifier from = rm.origin().ibisIdentifier();
				rm.finish();

				Job nextJob = null;
				//System.out.println(bot.noReplicatedJobs); 7
				//System.out.println(bot.noSampleJobs); 0

				if (received instanceof JobRequest) {
					nextJob = handleJobRequest(from);
					nextJob.setNode(from.location().getLevel(0));

					SendPort workReplyPort = myIbis.createSendPort(masterReplyPortType);
					workReplyPort.connect(from, "worker");

					WriteMessage wm = workReplyPort.newMessage();
					wm.writeObject(nextJob);
					wm.finish();

					workReplyPort.close();
				} else if (received instanceof JobResult) {
					handleJobResult((JobResult) received, from);
					undone = !areWeDone();
					continue;
				} else {
					System.exit(1);
				}
			} catch (ReceiveTimedOutException rtoe) {
				System.err.println("I timed out!");
				undone = ! areWeDone();				

			} catch (ConnectionFailedException cfe) {
				noTerminatedNodes++;
				System.err.println("Node " + cfe.ibisIdentifier().location().toString() + 
						" failed before receiving job ");
			} catch (IOException ioe) {									
				ioe.printStackTrace();
				undone = ! areWeDone();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		try {
			energyStats.kill();
			energyStatsThread.join();
			masterRP.close(100);
			System.out.println("Hurray! I shut down masterRP!!!");
			myIbis.end();
			System.out.println("Hurray! I shut down ibis!!!");
		} catch (Exception e) {
			e.printStackTrace();
		}

		ArrayList<Cluster> clusterList = new ArrayList<Cluster>(bot.Clusters.values()); 
		ArrayList<VirtualCluster> virtualClusterList = new ArrayList<VirtualCluster>();

		// add all virtual clusters to a list
		for (Cluster cl: clusterList) {
			virtualClusterList.addAll(((DAS4Cluster)cl).virtualCls.values());

		}

		// select last cluster with 1 thread as base for sampling points normalization,

		int baseIndex = virtualClusterList.size()-1;
		Cluster base = virtualClusterList.get(baseIndex);
		base.isReferenceCluster = true;
		base.beta0 = 0;
		base.beta1 = 1;

		for (VirtualCluster cl: virtualClusterList) {
			System.out.println("noThreads " + cl.noThreads);
			System.out.println("sampling points counter " + cl.samplingPointsCounter);
			System.out.println("replicated tasks counter " + cl.replicatedTasksCounter);
		}

		for(int i=0; i < baseIndex; i++) {						
			Cluster cluster = virtualClusterList.get(i);
			//System.out.println(cluster.samplingPoints.size() + " sampling points size " + cluster.alias);
			cluster.linearRegression(base);
			for(Job j : cluster.samplingPoints.values()) {
				double t = j.runtimes.get(cluster.alias).doubleValue();
				double tbase = (t-cluster.beta0)/cluster.beta1;
				j.runtimes.put(base.alias,new Double(tbase));
				base.samplingPoints.put(j.jobID, j);
				base.samplingPointsCounter ++;
			}
			System.out.println("cluster " + cluster.alias + ": beta1=" + cluster.beta1
					+ ", beta0=" + cluster.beta0);
		}

		base.estimateX();
		base.estimateTi();

		for(Job j : base.samplingPoints.values()) {
			double tbase = j.runtimes.get(base.alias).doubleValue();			
			base.noDoneJobs ++;
			base.totalRuntimeSampleJobs += tbase;
			base.totalRuntimeDoneJobs += tbase;
			base.orderedSampleResultsSet.add(new JobResult(j.jobID, new JobStats((long)tbase*1000000000L)));
		}
		for(Job j : base.regressionPoints.values()) {
			double tbase = j.runtimes.get(base.alias).doubleValue();
			base.samplingPoints.put(j.jobID, j);
			base.samplingPointsCounter ++;
			base.noDoneJobs ++;
			base.totalRuntimeSampleJobs += tbase;
			base.totalRuntimeDoneJobs += tbase;
			base.orderedSampleResultsSet.add(new JobResult(j.jobID, new JobStats((long)tbase*1000000000L)));
		}
		for(Job j : base.extraPoints.values()) {
			double tbase = j.runtimes.get(base.alias).doubleValue();			
			base.noDoneJobs ++;    		
			base.totalRuntimeDoneJobs += tbase;    		
		}
		System.out.println("cluster " + base.alias + " has " + base.samplingPointsCounter + " samples;" 
				+ " size of sampling points array is " + base.samplingPoints.size());

		System.out.println("base cluster is " + base.alias + ": mean=" + base.meanX 
				+ ", variance=" + base.varXsq);

		for(Cluster cluster : virtualClusterList) {	
			if(!cluster.isReferenceCluster) {				
				cluster.estimateX(base);
				cluster.estimateTi(base);
				for(Job j : base.samplingPoints.values()) {
					double tbase = j.runtimes.get(base.alias).doubleValue();
					double t = tbase*cluster.beta1 + cluster.beta0;
					//cluster.alias = ec2-micro
					j.runtimes.put(cluster.alias,new Double(t));
					if(cluster.samplingPoints.put(j.jobID, j) == null) 	cluster.samplingPointsCounter ++;
					cluster.noDoneJobs ++;
					cluster.totalRuntimeSampleJobs += t;
					cluster.totalRuntimeDoneJobs += t;
					cluster.orderedSampleResultsSet.add(new JobResult(j.jobID,new JobStats((long)t*1000000000L)));
				}
				for(Job j : cluster.extraPoints.values()) {
					//cluster.alias = ec2-micro
					System.out.println("for extra points " + cluster.alias);
					double t = j.runtimes.get(cluster.alias).doubleValue();			
					cluster.noDoneJobs ++;    		
					cluster.totalRuntimeDoneJobs += t;    		
				}
				System.out.println("cluster " + cluster.alias + " has " + cluster.samplingPointsCounter + " samples;" 
						+ " size of sampling points array is " + cluster.samplingPoints.size());
				System.out.println("cluster " + cluster.alias + ": mean=" + cluster.meanX + ", variance=" + cluster.varXsq +
						", beta1=" + cluster.beta1 + ", beta0=" + cluster.beta0);
			}
		}



		long totalTime = (System.currentTimeMillis()-actualStartTime)/1000;
		sampleMakespan = totalTime;
		System.out.println("Total sampling phase took " + totalTime + " (sec), which is about " + totalTime/60 + "m" + totalTime%60 +"s");

		//Cluster mostProfitable = selectMostProfitable();		

		//System.out.println("Most profitable machine type: " + mostProfitable.alias 
		//		+ ", cost: " + mostProfitable.costUnit + ", Ti: " + mostProfitable.Ti + " minutes");

		System.out.println("Sampling statistics: ");
		System.out.println("alias;noThreads; energy(J); sampling_time(sec); avg_exec_time(s); energy/sec; tasks/sec");
		for (VirtualCluster cluster : virtualClusterList) {
			cluster.computeSamplingEnergyCost();
			
			System.out.print(cluster.alias + ";" + cluster.noThreads + ";" + 
					cluster.samplingEnergyCost + ";" + 
					cluster.samplingTime + ";" + 
					cluster.meanX + ";");
			double tasksPerSec;
			if (cluster.alias.contains(EnergyConst.clusterAliasDoubleNodes)) {
				tasksPerSec = (Math.max(bot.noSampleJobs, cluster.noThreads*2)/2.0) / cluster.samplingTime;
			} else {
				tasksPerSec =  Math.max(bot.noSampleJobs, cluster.noThreads) / cluster.samplingTime;
			}
			double energyPerSec = cluster.samplingEnergyCost / cluster.samplingTime;
			System.out.println(energyPerSec + ";" + tasksPerSec);
		}
		
		
		System.out.println("Sampling statistics: ");
		for (VirtualCluster cluster : virtualClusterList) {
			
			System.out.print(cluster.alias + ";" + cluster.noThreads + ";" + 
					cluster.samplingEnergyCost + ";" + 
					cluster.samplingTime + ";" + 
					cluster.meanX + ";");
			double tasksPerSec;
			if (cluster.alias.contains(EnergyConst.clusterAliasDoubleNodes)) {
				tasksPerSec = (Math.max(bot.noSampleJobs, cluster.noThreads*2)/2.0) / cluster.samplingTime;
			} else {
				tasksPerSec =  Math.max(bot.noSampleJobs, cluster.noThreads) / cluster.samplingTime;
			}
			double energyPerSec = cluster.samplingEnergyCost / cluster.samplingTime;
			System.out.println(energyPerSec + ";" + tasksPerSec);
		}

	}

	private void assignJobs() {
		Collection<Cluster> clusters = bot.Clusters.values();
		for (Cluster c : clusters) {
			if (c instanceof DAS4Cluster) {
				DAS4Cluster das4Cluster = (DAS4Cluster) c;
				for (int i = 0; i < das4Cluster.bookedNodes.size(); i++) {
					if (das4Cluster.bookedNodes.get(i) instanceof DAS4DoubleNode) {
						DAS4DoubleNode dNode = (DAS4DoubleNode) das4Cluster.bookedNodes.get(i);
						indexMaxNoJobsToNodes.put(dNode.name1, 0);
						System.out.println(dNode.name1 + " assign jobs ");
						noJobsToNodes.put(dNode.name1, 0);
						indexMaxNoJobsToNodes.put(dNode.name2, 0);
						System.out.println(dNode.name2 + " assign jobs ");
						noJobsToNodes.put(dNode.name2, 0);
					} else {
						indexMaxNoJobsToNodes.put(das4Cluster.bookedNodes.get(i).name, 0);
						noJobsToNodes.put(das4Cluster.bookedNodes.get(i).name, 0);
						
					}
				}
			} 
		}
	}

	// if maximum is reached return 0
	private int increaseNoJobs(String node) {
		int noJobs = EnergyConst.maxJobsPerNodeList[indexMaxNoJobsToNodes.get(node)];
		System.out.println("increase " + noJobs);
		int nextIndex = -1;
		if (noJobs >= EnergyConst.maxJobsPerNode) {
			indexMaxNoJobsToNodes.put(node, nextIndex);
			System.out.println("increasing number of jobs for node " + node + " from " + noJobs + " to -1");
			return 0;
		} else {
			nextIndex = indexMaxNoJobsToNodes.get(node) + 1;
			indexMaxNoJobsToNodes.put(node, nextIndex);
			energyStats.setNoTasks(node, EnergyConst.maxJobsPerNodeList[nextIndex]);
			System.out.println("increasing number of jobs for node " + node + " from " + noJobs + " to " +  EnergyConst.maxJobsPerNodeList[nextIndex]);

			return 1;
		}
	}

	@Override
	public void startInitWorkers() {

		Collection<Cluster> clusters = bot.Clusters.values();
		ArrayList<DAS4Node> bookedNodes = new ArrayList<DAS4Node>();
		System.out.println("clusters size " + clusters.size());
		for (Cluster c : clusters) {
			System.out
			.println("BoTRunner has found " + bot.tasks.size()
					+ " jobs; will send them to " + c.pendingNodes
					+ " initial workers on cluster " + c.alias);
			Process p = c.startNodes(/* deadline2ResTime() */"10:00:00",
					c.pendingNodes, bot.electionName,
					bot.poolName, bot.serverAddress);

			// connect booked nodes to energy query logic
			if (c instanceof DAS4Cluster) {
				bookedNodes.addAll(((DAS4Cluster) c).bookedNodes);
			} else {
				System.err.println("it should always be DAS4Cluster");
			}

		}
		energyStats = new EnergyStats(bookedNodes);
		energyStatsThread = new Thread(energyStats);
		energyStatsThread.start();
	}

	public double getSampleCost() {
		return sampleCost;
	}

	public long getSampleMakespan() {
		return sampleMakespan;
	}
}
