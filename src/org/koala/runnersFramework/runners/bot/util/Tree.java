package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class Tree {
	ArrayList<Tree> kids;
	int jobId;
	String mtype;
	String otherType;
	int machine;
	
	int nextFreeMachine = 0;
	int nextFreeMachineOtherType = 0;
	
	Bag myBag;
	
	public Tree(int jobId, String mtype, String otherType, int machine, int nextFreeMachine, int nextFreeMachineOtherType2) {
	
		this.kids = new ArrayList<Tree>();
		this.jobId = jobId;
		this.mtype = mtype;
		this.otherType = otherType;
		this.machine = machine;
		if(machine == nextFreeMachine) {
			nextFreeMachine = (nextFreeMachine + 1)%32;
		}
		this.nextFreeMachineOtherType = nextFreeMachineOtherType; 
	}

	public Tree() {
		this.kids = new ArrayList<Tree>();
	}

	public void print() {
		Stack<Tree> stack = new Stack<Tree>();
		for(Tree kid : kids) {
			stack.push(kid);
		}
		HashMap<String,MachineType> Clusters = new HashMap<String,MachineType>();
		long[] runtimeS = new long[Clusters.get("slow").Mmax];
		long[] runtimeF = new long[Clusters.get("fast").Mmax];
		
		while(!stack.empty()) {
			Tree rest = stack.pop();
			if(rest.mtype.compareTo("fast") == 0) {
				runtimeF[rest.machine] += myBag.x.get(rest.jobId)/myBag.speedFactor;
			} else {
				runtimeS[rest.machine] += myBag.x.get(rest.jobId);
			}
			if(rest.kids.size() != 0) {
				for(Tree kid : rest.kids) {
					stack.push(kid);
				}
			} else {
				long maxRT = Long.MIN_VALUE;
				long budget = 0;
				long machinesUsedS = 0;
				long machinesUsedF = 0;
				for(int i=0; i<runtimeS.length; i++) {
					if(maxRT<runtimeS[i]) maxRT = runtimeS[i];
					if(runtimeS[i] != 0) machinesUsedS ++;
					budget += Math.ceil((double)runtimeS[i]/60/Clusters.get("slow").atu)*Clusters.get("slow").costUnit;
					runtimeS[i] = 0;
				}
				for(int i=0; i<runtimeF.length; i++) {
					if(maxRT<runtimeF[i]) maxRT = runtimeF[i];
					if(runtimeF[i] != 0) machinesUsedF ++;
					budget += Math.ceil((double)runtimeF[i]/60/Clusters.get("fast").atu)*Clusters.get("fast").costUnit;
					runtimeF[i] = 0;
				}
				System.out.println("budget: " + budget + "\t longest run: " + maxRT + "\t slow: " + machinesUsedS +
						"\t fast: " + machinesUsedF);
			}
		}
		
	}
		
}
