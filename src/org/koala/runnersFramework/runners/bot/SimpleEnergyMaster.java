package org.koala.runnersFramework.runners.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

import org.koala.runnersFramework.runners.bot.util.Chromosome;

import com.amazonaws.services.cloudwatch.model.HistoryItemType;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

public class SimpleEnergyMaster extends Master {
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

	private final int ENERGY_OPTIMIZ = 1;
	private final int TIME_OPTIMIZ = 0;

	private int direction = TIME_OPTIMIZ;
	private EnergyStats energyStats;

	private int alfa = 40;
	private int maxRand = 0;



	Random r = new Random();

	protected SimpleEnergyMaster(BoTRunner bot) throws Exception {
		super(bot);

		totalSamplingSubmittedCounter = new HashMap<Cluster, Integer>();

		schedules = new ArrayList<Schedule>();
		noJobsToNodes = new HashMap<String, Integer>();
		indexMaxNoJobsToNodes = new HashMap<String, Integer>();
		terminatedNodes = new HashMap<String, Boolean>();
		waitingNodes = new HashMap<String, Boolean>();

		bot.finishedTasks = new ArrayList<Job>();

		replicatedTasks = new ArrayList<Job>();
		Random randomSample = new Random(1111111111L);

		/*for(int i = 0; i < bot.noReplicatedJobs; i++) {
			replicatedTasks.add(bot.tasks.remove(randomSample.nextInt(bot.tasks.size())));
		}*/

		bot.noSampleJobs = bot.tasks.size();

		System.out.println("Sample size is: " + bot.noSampleJobs);

		Collection<Cluster> clusters = bot.Clusters.values();

		bot.noInitialWorkers = 1;
		this.noTerminatedNodes = 0;

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

		System.out.println("maxRand = " + maxRand);
	}


	@Override
	protected boolean areWeDone() {
		if (bot.tasks.size() == 0) {
			return true;
		}

		return false;
	}

	@Override
	protected Job handleJobRequest(IbisIdentifier from) {
		String clusterName = from.location().getParent().toString();
		String node = from.location().getLevel(0);
		String clusterAlias = EnergyConst.allNodes.get(node);
		Cluster cluster = bot.Clusters.get(clusterAlias);

		/*DEBUG*/
		System.err.println("job request from node " + from.location().toString() + " in cluster " + clusterAlias);

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

		System.err.println(from.location().toString() + " returned result of job " + 
				received.getJobID() + " executed for (sec)" + received.getStats().getRuntime()/(double)1000000000);


		int noJobs = noJobsToNodes.get(node);
		//int samplingCounter = 0;

		if (cluster instanceof DAS4Cluster) {
			DAS4Cluster c = (DAS4Cluster) cluster;
			int maxJobs = c.currentMaxNoThreads;

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

			doneJob.runtimes.put(vc.alias, new Double(received.getStats().getRuntime()/(double)1000000000)) ;
			doneJob.done = true;

			bot.finishedTasks.add(doneJob);
		}

		noJobs--;
		noJobsToNodes.put(node, noJobs);

		if (noJobs == 0 && terminatedNodes.get(node) != null && terminatedNodes.get(node) == true) {
			DAS4DoubleNode dNode = EnergyConst.isPartOfDoubleNode(node);

			for (int i = 0; i < energyStats.nodes.size(); i++) {
				if (dNode == null) {
					if (!(energyStats.nodes.get(i) instanceof DAS4DoubleNode) && energyStats.nodes.get(i).name.equals(node)) {
						energyStats.pduThreads.get(i).kill();
					}
				} else {
					if (terminatedNodes.get(dNode.name1) != null && 
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
		int maxJobs = 0;
		if (cluster instanceof DAS4Cluster) {
			maxJobs = ((DAS4Cluster)cluster).currentMaxNoThreads;
		}

		//System.out.println("find next job node " + node + " noJobs " + noJobs + " maxJobs " + maxJobs);
		energyStats.setNoTasks(node, maxJobs);

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

				Integer count = totalSamplingSubmittedCounter.get(vc);
				DAS4DoubleNode dNode = EnergyConst.isPartOfDoubleNode(node);

				if (count == null) {
					totalSamplingSubmittedCounter.put(vc, 0);
					count = 0;
				}
				
				long currentTime = System.currentTimeMillis();
				
				// double nodes
				if (dNode != null && count > vc.noThreads * 8) {
					c.minTasksDone = true;
				}
				// single nodes , vc.noThreads * 4
				if (dNode == null && count > vc.noThreads * 4) {
					c.minTasksDone = true;
				}

				if ((currentTime - vc.samplingStartTime)/1000.0 > 10) {
					c.minTimeDone = true;
				}


				if (!c.pause) {
					if (count.intValue() < bot.noSampleJobs) {
						nextJob = bot.tasks.remove(random.nextInt(bot.tasks.size()));
						vc.samplingPoints.put(nextJob.jobID, nextJob);
						totalSamplingSubmittedCounter.put(vc, count.intValue()+1);
						noJobsToNodes.put(node, noJobs+1);
					} 
					if (c.wasPaused == true) {
						System.out.println("start time");
						vc.samplingStartTime = currentTime;
						c.wasPaused = false;
					}
				}

				// if it's part of a double node
				if (dNode != null) {
					if (c.pause) {
						if (count != null && noJobs == 0) {
							waitingNodes.put(node, true);
							Boolean isNode1Waiting = waitingNodes.get(dNode.name1);
							Boolean isNode2Waiting = waitingNodes.get(dNode.name2);
							if (isNode1Waiting != null && isNode2Waiting != null) {
								if (isNode1Waiting == true && isNode2Waiting == true) {
									if (c.allJobsReceived == false) {
										System.out.println("all jobs received");
										energyStats.addPauseToEnergyMeas(node);
										if (vc.samplingTimeSet == false) {
											currentTime = System.currentTimeMillis();
											vc.samplingTimeSet = true;
											vc.samplingTime = (currentTime - vc.samplingStartTime)/1000.0;
											System.out.println("end time: " + vc.samplingTime);
										}
									}
									c.allJobsReceived = true;

									energyStats.setNoTasks(node, 0);
									if (!c.noThreadsQueue.isEmpty()) {
										c.pause = false;
										c.allJobsReceived = false;
										c.minTasksDone = false;
										c.minTimeDone = false;
										waitingNodes.put(dNode.name1, false);
										waitingNodes.put(dNode.name2, false);
										c.wasPaused = true;
										
										c.currentMaxNoThreads = c.noThreadsQueue.pollLast();
										energyStats.setNoTasks(node, c.currentMaxNoThreads);
										
									}
								} 
							}
						}

						nextJob = new NoJob(EnergyConst.wait);
					}
				} else {

					if (c.pause) {
						if (count != null && noJobs == 0) {
							if (c.allJobsReceived == false) {
								energyStats.addPauseToEnergyMeas(node);
							}
							c.allJobsReceived = true;
							currentTime = System.currentTimeMillis();
							energyStats.setNoTasks(node, 0);
							if (!c.noThreadsQueue.isEmpty()) {
								c.currentMaxNoThreads = c.noThreadsQueue.pollLast();
								energyStats.setNoTasks(node, c.currentMaxNoThreads);

								/*if (c.virtualCls.get(currentMaxNoThreads) != null) {
									c.virtualCls.get(currentMaxNoThreads).startTime = System.currentTimeMillis();

								}*/
								c.pause = false;
								c.allJobsReceived = false;
								c.minTasksDone = false;
								c.minTimeDone = false;
								//alfa = 500;
							} else {
								//System.out.println("add time: " + (currentTime - vc.startTime)/1000.0);
								vc.samplingTime += (currentTime - vc.samplingStartTime)/1000.0;
								vc.paused = true;
							}
						}

						nextJob = new NoJob(EnergyConst.wait);
					} 
				}
			}


		} else {
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
	protected void handleLostConnections() {}

	@Override
	public void run() {
		boolean undone = true;
		timeout = (long) (60000);
		System.out.println("Timeout is now " + timeout);
		SendPort workReplyPort = null;
		try {
			workReplyPort = myIbis.createSendPort(masterReplyPortType);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		HashMap<String, Boolean> connectedWorkers = new HashMap<String, Boolean>();

		//long startTime = System.currentTimeMillis();

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
					
					/*System.out.println(connectedWorkers.get(from.location().toString()));
					if (connectedWorkers.get(from.location().toString()) == null) {
						// create connection
						workReplyPort.connect(from, "worker");
						// save worker
						connectedWorkers.put(from.location().toString(), Boolean.TRUE);
					}*/
					workReplyPort = myIbis.createSendPort(masterReplyPortType);
					workReplyPort.connect(from, "worker");
					WriteMessage wm = workReplyPort.newMessage();
					wm.writeObject(nextJob);
					wm.finish();

					
					workReplyPort.disconnect(workReplyPort.connectedTo()[0]);
					workReplyPort.close();
				} else if (received instanceof JobResult) {
					handleJobResult((JobResult) received, from);
					undone = !areWeDone();
					continue;
				} else {
					System.exit(1);
				}

				long currentTime = System.currentTimeMillis();

				Collection<Cluster> clusters = bot.Clusters.values();

				/*if (minTasksDone) {
					System.out.println("min tasks done");
				}*/
				//&& (currentTime - startTime)/1000 > 10
				for (Cluster cluster: clusters) {
					DAS4Cluster cl = (DAS4Cluster) cluster;
					if (cl.minTasksDone && cl.minTimeDone) {
						//cl.startTime = currentTime;
						cl.pause = true;
					}
					if (cl.allJobsReceived == true) {
						//System.out.println("before decide next no threads");
						cl.printSamplingStats();
						//computeEnergyMakespanEstim((new ArrayList<VirtualCluster>(cl.virtualCls.values())));
						cl.decideNextNoThreads();
					}
				}

			} catch (ReceiveTimedOutException rtoe) {
				//System.out.println(" received time out exception ");
				System.err.println("I timed out!");
				undone = ! areWeDone();				

			} catch (ConnectionFailedException cfe) {
				noTerminatedNodes++;
				System.out.println("Node " + cfe.ibisIdentifier().location().toString() + 
						" failed before receiving job ");
				System.out.println(cfe.getLocalizedMessage());
				System.out.println(cfe.getMessage());
				System.out.println(cfe.getCause());
				cfe.printStackTrace();
				
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

	}

	/*private void computeEnergyMakespanEstim(ArrayList<VirtualCluster> virtualClusterList) {
		//ArrayList<VirtualCluster> virtualClusterList = new ArrayList<VirtualCluster>();

		// add all virtual clusters to a list
		//for (Cluster cl: clusterList) {
		//	virtualClusterList.addAll(((DAS4Cluster)cl).virtualCls.values());

		//}
		 
		// select last cluster with 1 thread as base for sampling points normalization,

		int baseIndex = virtualClusterList.size()-1;
		Cluster base = virtualClusterList.get(baseIndex);
		base.isReferenceCluster = true;
		base.beta0 = 0;
		base.beta1 = 1;

		for (VirtualCluster cl: virtualClusterList) {
			//	System.out.println("noThreads " + cl.noThreads);
			//	System.out.println("sampling points counter " + cl.samplingPointsCounter);
			//	System.out.println("regression points " + cl.regressionPoints.size());
			//	System.out.println("replicated tasks counter " + cl.replicatedTasksCounter);
			cl.extraPoints = new HashMap<String, Job>();
		}

		for(int i=0; i < baseIndex; i++) {						
			Cluster cluster = virtualClusterList.get(i);
			//System.out.println(cluster.samplingPoints.size() + " sampling points size " + cluster.alias);
			cluster.linearRegression(base);
			for(Job j : cluster.samplingPoints.values()) {
				double t = j.runtimes.get(cluster.alias).doubleValue();
				double tbase = (t-cluster.beta0)/cluster.beta1;
				j.runtimes.put(base.alias,new Double(tbase));
				base.extraPoints.put(j.jobID, j);
				//base.samplingPointsCounter ++;
			}
			System.out.println("cluster " + cluster.alias + ": beta1=" + cluster.beta1
					+ ", beta0=" + cluster.beta0);
		}

		base.estimateX();
		base.estimateTi();

		//for(Job j : base.samplingPoints.values()) {
		//	double tbase = j.runtimes.get(base.alias).doubleValue();			
	//		base.noDoneJobs ++;
	//		base.totalRuntimeSampleJobs += tbase;
	//		base.totalRuntimeDoneJobs += tbase;

		//}
		//for(Job j : base.regressionPoints.values()) {
		//	double tbase = j.runtimes.get(base.alias).doubleValue();
			//base.samplingPoints.put(j.jobID, j);
			//base.samplingPointsCounter ++;
		//	base.noDoneJobs ++;
		//	base.totalRuntimeSampleJobs += tbase;
		//	base.totalRuntimeDoneJobs += tbase;
//
//		}
		 
		System.out.println("cluster " + base.alias + " has " + base.samplingPoints.size() + " samples;" 
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
					cluster.extraPoints.put(j.jobID, j);
					//cluster.noDoneJobs ++;
					//cluster.totalRuntimeSampleJobs += t;
					//cluster.totalRuntimeDoneJobs += t;
					 
				}

				System.out.println("cluster " + cluster.alias + " has " + cluster.samplingPoints.size() + " samples;" 
						+ " size of sampling points array is " + cluster.samplingPoints.size());
				System.out.println("cluster " + cluster.alias + ": mean=" + cluster.meanX + ", variance=" + cluster.varXsq +
						", beta1=" + cluster.beta1 + ", beta0=" + cluster.beta0);
			}
		}
	}
*/


	private void assignJobs() {
		Collection<Cluster> clusters = bot.Clusters.values();
		for (Cluster c : clusters) {
			if (c instanceof DAS4Cluster) {
				DAS4Cluster das4Cluster = (DAS4Cluster) c;
				for (int i = 0; i < das4Cluster.bookedNodes.size(); i++) {
					if (das4Cluster.bookedNodes.get(i) instanceof DAS4DoubleNode) {
						DAS4DoubleNode dNode = (DAS4DoubleNode) das4Cluster.bookedNodes.get(i);
						indexMaxNoJobsToNodes.put(dNode.name1, 0);
						//System.out.println(dNode.name1 + " assign jobs ");
						noJobsToNodes.put(dNode.name1, 0);
						indexMaxNoJobsToNodes.put(dNode.name2, 0);
						//System.out.println(dNode.name2 + " assign jobs ");
						noJobsToNodes.put(dNode.name2, 0);

					} else {
						indexMaxNoJobsToNodes.put(das4Cluster.bookedNodes.get(i).name, 0);
						noJobsToNodes.put(das4Cluster.bookedNodes.get(i).name, 0);

					}
				}
			} 
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
			Process p = c.startNodes(/* deadline2ResTime() */"06:00:00",
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
