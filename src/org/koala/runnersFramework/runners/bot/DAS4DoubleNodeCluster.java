package org.koala.runnersFramework.runners.bot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DAS4DoubleNodeCluster extends DAS4Cluster {
	
	private static final long serialVersionUID = 1L;
	ArrayList<DAS4DoubleNode> doubleNodes;
	
	public DAS4DoubleNodeCluster(String hostname, String alias, long timeUnit,
			double costUnit, int maxNodes, String speedFactor) {
		
		super(hostname, alias, timeUnit, costUnit, maxNodes, speedFactor);
		doubleNodes = EnergyConst.doubleNodes;
	}
	
	@Override
	public Process startNodes(String time, int noWorkers,
			String electionName, String poolName, String serverAddress) {

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

	// return reservation numbers
	@Override 
	int[] reserveNodes(int i, String time) throws IOException {
		int resNo[] = new int[2];
		System.out.println("reserve double nodes " + i);

		String command = "preserve -t " + time + " -q \"all.q@" + doubleNodes.get(i).name1 + ".cm.cluster\"";
		
		resNo[0] = 4912362;//runPreserve(command);
		//4597260; 
		command = "preserve -t " + time + " -q \"all.q@" + doubleNodes.get(i).name2 + ".cm.cluster\"";
		
		resNo[1] =  4912363;//runPreserve(command);
				//4597261;
		bookedNodes.add(doubleNodes.get(i));
		System.out.println("reservation numbers " + resNo[0] + " " + resNo[1]);

		return resNo;
	}

	
}
