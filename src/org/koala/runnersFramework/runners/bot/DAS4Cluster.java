package org.koala.runnersFramework.runners.bot;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;

public class DAS4Cluster extends Cluster {

	private static final long serialVersionUID = 1L;
	String FS;
	// variable for testing purposes.
	String speedFactor;
	//int ibisID = 1;

	// in String[] we store the two nodes

	// in String[] we store a node and its corresponding special cmd
	ArrayList<DAS4Node> nodes;

	ArrayList<DAS4Node> bookedNodes = new ArrayList<DAS4Node>();
	HashMap<Integer, VirtualCluster> virtualCls = new HashMap<Integer, VirtualCluster>();

	LinkedList<Integer> noThreadsQueue = new LinkedList<Integer>();
	LinkedList<Double> tasksPerSecHistory = new LinkedList<Double>();
	LinkedList<Double> energyPerSecHistory = new LinkedList<Double>();
	LinkedList<Integer> noThreadsHistory = new LinkedList<Integer>();
	HashMap<Integer, Boolean> noThreadsMap = new HashMap<Integer, Boolean>();
	ArrayList<NoThreadProb> noThreadProbList = new ArrayList<NoThreadProb>();
	PriorityQueue<Double> hillHistory = new PriorityQueue<Double>(1, Collections.reverseOrder());
	ArrayList<Double> maxHillHistory = new ArrayList<Double>();
	ArrayList<Double> lastHillHistory = new ArrayList<Double>();
	ArrayList<Integer> lastTNHistory = new ArrayList<Integer>();
	ArrayList<VirtualCluster> ps = new ArrayList<VirtualCluster>();

	// control variables for virtual clusters creation during sampling
	boolean pause = false;
	boolean allJobsReceived = false;
	boolean minTasksDone = false;
	boolean minTimeDone = false;
	boolean wasPaused = true;
	//long startTime = -1;
	int maxRand;
	private int sign = 1;
	private int step = 4;
	int currentMaxNoThreads = EnergyConst.maxJobsPerNodeList[0];


	public DAS4Cluster(String hostname, String alias, long timeUnit, double costUnit, int maxNodes, String speedFactor) {
		super(hostname, alias, timeUnit, costUnit, maxNodes);
		this.FS = alias.substring(alias.lastIndexOf("@")+1);
		this.speedFactor = "" + speedFactor;

		this.nodes = EnergyConst.fatNodes;

		maxRand = createPseudoRandomGen(16);
		noThreadsMap.put(-1, true);
	}



	// return reservation number
	int[] reserveNodes(int i, String time) throws IOException {
		int[] resNo = new int[1];
		System.out.println("reserve single nodes " + i);

		String command = "ssh " + hostname + " preserve -t " + time + " -native '" + nodes.get(i).args + " -q \"fat.q@" + nodes.get(i).name + ".cm.cluster\"'";

		System.out.println(command + " command");
		//preserve -t 00:15:00 -q "fat.q@node080.cm.cluster" -native '-l fat,m_type=sandybridge'
		resNo[0] = 4912318; //runPreserve(command);

		bookedNodes.add(nodes.get(i));
		return resNo;
	}

	@Override
	public void terminateNode(IbisIdentifier from, Ibis myIbis)
			throws IOException {
	}

	@Override
	public Process startNodes(String time, int noWorkers, String electionName,
			String poolName, String serverAddress) {
		if(noWorkers == 0) return null;

		for (int i = 0; i < noWorkers; i++) {
			try {
				int[] resNo = this.reserveNodes(i, time);
				// wait 2 sec
				Thread.sleep(2000);
				for (int j = 0; j < resNo.length; j++) {
					String command = "ssh " + hostname + " "
							+ "module load prun;module load java; prun -v -1 -reserve "
							+ resNo[j] + " -np 1 -asocial -t " + time + " java -classpath " 
							+ " /home/ava360/bats/BaTSwithEnergySimpleVersion/lib/conpaas-bot.jar:"
							+ "/home/ava360/bats/BaTSwithEnergySimpleVersion/ipl-2.2/lib/*:"
							+ "/home/ava360/bats/BaTSwithEnergySimpleVersion/ipl-2.2/external/* "
							+ " -Dibis.location.postfix=" + hostname 
							+ " org.koala.runnersFramework.runners.bot.DAS4Worker "
							+ electionName + " " 
							+ poolName + " "
							+ serverAddress + " "
							+ speedFactor + " "
							+ "> /home/ava360/bats/BaTSwithEnergySimpleVersion/worker/worker"+resNo[j]+".log "
							+ "2> /home/ava360/bats/BaTSwithEnergySimpleVersion/worker/worker"+resNo[j]+".err";

					System.out.println(command);
					Runtime.getRuntime().exec(command);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	//return reservation number
	public int runPreserve(String command) throws IOException {
		System.out.println(command);

		Process p = Runtime.getRuntime().exec(command);

		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		String line = err.readLine();
		while (line != null) {
			System.out.println(line);
			line = err.readLine();
		}
		line = in.readLine();
		String[] words = line.split(" ");
		return Integer.parseInt(words[2].substring(0, words[2].length()-1));
	}


	private int createPseudoRandomGen(int no_cores) {
		noThreadProbList = new ArrayList<NoThreadProb>();
		int returnMaxRand = 0;

		for (int i = 1; i <= no_cores && i <= EnergyConst.maxJobsPerNode; i++) {
			noThreadProbList.add(new NoThreadProb(i, 1));
			returnMaxRand += 1;
		}
		for (int i = no_cores + 1; i <= no_cores * 2 && i <= EnergyConst.maxJobsPerNode; i++) {
			noThreadProbList.add(new NoThreadProb(i, 2));
			returnMaxRand += 2;
		}
		for (int i = 2 * no_cores + 1; i <= no_cores * 3 && i <= EnergyConst.maxJobsPerNode; i++) {
			noThreadProbList.add(new NoThreadProb(i, 3));
			returnMaxRand += 3;
		}
		for (int i = 3 * no_cores + 1; i <= no_cores * 4 && i <= EnergyConst.maxJobsPerNode; i++) {
			noThreadProbList.add(new NoThreadProb(i, 4));
			returnMaxRand += 4;
		}
		for (int i = 4 * no_cores + 1; i <= no_cores * 5 && i <= EnergyConst.maxJobsPerNode; i++) {
			noThreadProbList.add(new NoThreadProb(i, 3));
			returnMaxRand += 3;
		}
		for (int i = 5 * no_cores + 1; i <= no_cores * 6 && i <= EnergyConst.maxJobsPerNode; i++) {
			noThreadProbList.add(new NoThreadProb(i, 2));
			returnMaxRand += 2;
		}
		for (int i = 6 * no_cores + 1; i <= no_cores * 7 && i <= EnergyConst.maxJobsPerNode; i++) {
			noThreadProbList.add(new NoThreadProb(i, 1));
			returnMaxRand += 1;
		}

		return returnMaxRand;
	}

	class NoThreadProb {
		int noThreads;
		int prob;

		NoThreadProb(int noThreads, int prob){
			this.noThreads = noThreads;
			this.prob = prob;
		}
	}


	void decideNextNoThreads() {

		int nextNoThreads;
		//System.out.println("decide next no threads");

		if (tasksPerSecHistory.size() > 1 && tasksPerSecHistory.get(0) < tasksPerSecHistory.get(1)) {
			sign *= (-1);
			//System.out.println("Change of sign");
		}

		if (tasksPerSecHistory.size() > 1) {
			//System.out.println("change of step " + (int)((tasksPerSecHistory.get(0) - tasksPerSecHistory.get(1))*10.0));
			double tasksPerSec = tasksPerSecHistory.get(0);
			if (tasksPerSecHistory.get(1) > tasksPerSec) {
				tasksPerSec = tasksPerSecHistory.get(1);
			}

			double normalizedTasksPerSec = (tasksPerSecHistory.get(0) - tasksPerSecHistory.get(1))/tasksPerSec;
			//System.out.println(normalizedTasksPerSec + " normalized tasks per sec");
			int dec = 1;

			//	System.out.println("dec " + dec);
			double stepD = normalizedTasksPerSec*10;
			//System.out.println("stepD " + stepD);
			step = (int)Math.ceil(stepD);
			if (stepD < 0 && stepD > -1) {
				step = -1;
			}

			//System.out.println("step " + step);

			nextNoThreads = currentMaxNoThreads + step;
			checkAndRestart(nextNoThreads);
			int hill = checkHill(nextNoThreads);
			if (hill > 0) {
				nextNoThreads = hill;
				checkAndRestart(nextNoThreads);
			} else if (hill == 0) {
				int randomRestart = checkAndRestart(-1);
				//System.out.println("random");
				double max = Collections.max(lastHillHistory);
				System.out.println("hill " + max);
				hillHistory.add(max);
				lastHillHistory = new ArrayList<Double>();
				lastTNHistory = new ArrayList<Integer>();
				nextNoThreads = randomRestart;
				maxHillHistory.add(hillHistory.peek());
				sign = 1;
			}

		} else {
			//System.out.println("else from decide next no threads");
			nextNoThreads = currentMaxNoThreads + step;
		}

		//nextNoThreads = currentMaxNoThreads + 1;

		if (nextNoThreads <= 0) {
			nextNoThreads = 1;
		}

		//System.out.println("next no threads  " + nextNoThreads);
		noThreadsQueue.add(nextNoThreads);
		if (checkStopCondition()) {
			ArrayList<VirtualCluster> virtualClusterList = new ArrayList<VirtualCluster>(virtualCls.values());


			ps = computePS(virtualClusterList);

			System.out.println("Pareto Set");
			System.out.println("alias; no-threads; energy ; makespan");
			for (VirtualCluster vc : ps) {
				System.out.println(vc.alias + " ; " + vc.noThreads + " ; " + vc.energyPerBag + " ; " + vc.makespan);
			}

			System.exit(0);

		}

		// piece of code for running whole bag with the same number of threads
		//	nextNoThreads = currentMaxNoThreads;
		//	noThreadsQueue.add(nextNoThreads);
	}

	private int checkHill(int nextNoThreads) {
		int hill = -1;
		//System.out.println("next " + nextNoThreads);

		boolean found = false;
		for (int i = 0; i < lastTNHistory.size(); i++) {
			if (nextNoThreads == lastTNHistory.get(i)) {
				found = true;
				break;
			}
		}


		// maximul este margine de interval si coincide cu nextNoThreads
		double max = Double.MIN_VALUE;
		int index = 0;
		for (int i = 0; i < lastHillHistory.size(); i++) {
			if (lastHillHistory.get(i) > max) {
				max = lastHillHistory.get(i).doubleValue();
				index = i;
			}
		}
		//System.out.println("max is at index " + index);

		int tn = lastTNHistory.get(index);
		//System.out.println("tn " + tn);
		if (tn == Collections.max(lastTNHistory)) {
			if (found) {
				hill = tn+1;
			}
		} else if (tn == Collections.min(lastTNHistory)) {
			if (found) {
				hill = tn-1;
			}
		} else {
			hill = 0;
		}



		return hill;
	}



	private ArrayList<VirtualCluster> computePS(ArrayList<VirtualCluster> population) {
		ArrayList<VirtualCluster> localSet = new ArrayList<VirtualCluster>();
		Collections.sort(population, new Comparator<VirtualCluster>(){
			@Override
			public int compare(VirtualCluster o1, VirtualCluster o2) {
				if (o1.makespan > o2.makespan)
					return 1;
				else if (o1.makespan == o2.makespan)
					return 0;
				else return -1;
			}
		});

		int i = 0;
		boolean done = false;
		while (!done) {
			if (localSet.size() == 0 || 
					localSet.size() > 0 && 
					localSet.get(localSet.size()-1).makespan != population.get(i).makespan) {
				localSet.add(population.get(i));
			}
			done = true;
			for (int j = i+1; j < population.size(); j++){
				if (population.get(j).energyPerBag < population.get(i).energyPerBag) {
					i = j;
					done = false;
					break;
				}
			}
		}

		return localSet;

	}

	private boolean checkStopCondition() {
		int size = hillHistory.size();
		if (size < 2) {
			return false;
		} else {
			double first = hillHistory.poll();
			double second = hillHistory.peek();
			double normalizedDiff = (first - second)/first;
			System.out.println("Difference between the two biggest hills " + normalizedDiff);
			hillHistory.add(first);
			// has been 0.05 - too big
			if (normalizedDiff < 0.03) {
				return true;
			}
			//maxTasksPerSecHistory.add();
			/*int i = maxHillHistory.size() - 1;
			System.out.println("max hill hist " + i);
			for (int j = 0; j < maxHillHistory.size(); j++) {
				System.out.println(maxHillHistory.get(j));	
			}
			 */	

		}
		return false;
	}

	// if nextNoThreads == -1 just restart
	private int checkAndRestart(int nextNoThreads) {
		int newStart = -1;
		if (nextNoThreads >= 1 && noThreadsMap.get(nextNoThreads) == null) {
			return newStart;
		}
		while (noThreadsMap.get(newStart) != null) {
			int rand = new Random().nextInt(maxRand);
			int sum = 0;
			for (NoThreadProb tp : noThreadProbList) {
				if (rand <= sum) {
					System.out.println(newStart + " new start ");
					newStart = tp.noThreads;
					break;
				} else {
					sum += tp.prob;
				}
			}
		}

		return newStart;
	}

	void printSamplingStats() {
		System.out.println("Sampling statistics: ");
		System.out.println("alias;noThreads; energy(J); sampling_time(sec); "
				+ "energy/sec; tasks/sec; en-avg; en-median; en-max; total-en-avg; "
				+ "total-en-median; total-en-max; makespan; size");
		for (VirtualCluster cluster : virtualCls.values()) {

			cluster.computeSamplingEnergyCost();

			System.out.print(cluster.alias + ";" + cluster.noThreads + ";" + 
					cluster.samplingEnergyCost + ";" + 
					cluster.samplingTime + ";");
			//System.out.println(cluster.samplingPointsCounter + " deimpartit");
			cluster.tasksPerSec =  cluster.samplingPointsCounter / cluster.samplingTime;

			cluster.energyPerSec = cluster.samplingEnergyCost / cluster.samplingTime;

			System.out.print(cluster.energyPerSec + ";" + cluster.tasksPerSec + ";");
			if (cluster.noThreads == currentMaxNoThreads) {
				tasksPerSecHistory.addFirst(cluster.tasksPerSec);
				noThreadsHistory.addFirst(currentMaxNoThreads);
				lastHillHistory.add(cluster.tasksPerSec);
				lastTNHistory.add(currentMaxNoThreads);
				noThreadsMap.put(currentMaxNoThreads, true);
				//System.out.println("currentMaxNoThreads " + currentMaxNoThreads);
				//System.out.println("addFirst " + tasksPerSec);
			}

			cluster.makespan = 15000.0/cluster.tasksPerSec;
			cluster.energyPerBag = cluster.makespan * cluster.energyPerSec;
			cluster.medianEnergyPerBag = cluster.makespan * cluster.medianEstimatedEnergyPerSec;
			cluster.maxEnergyPerBag = cluster.makespan * cluster.maxEstimatedEnergyPerSec;
			cluster.avgEnergyPerBag = cluster.makespan * cluster.avgEstimatedEnergyPerSec;

			System.out.print(cluster.avgEstimatedEnergyPerSec + ";" + cluster.medianEstimatedEnergyPerSec + ";" + cluster.maxEstimatedEnergyPerSec + ";");
			System.out.print(cluster.avgEnergyPerBag + ";" + cluster.medianEnergyPerBag + ";" + cluster.maxEnergyPerBag + ";" + cluster.makespan + ";");

			System.out.println(cluster.samplingPointsCounter );
		}

		System.out.println("Threads history: ");
		for (int i: noThreadsHistory){
			System.out.print(i + " ");
		}
		System.out.println();
	}
}
