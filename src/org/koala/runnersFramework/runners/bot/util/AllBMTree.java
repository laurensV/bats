package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.HashMap;

public class AllBMTree {

	Bag myBag;	
	Tree allPlans;
	
	public AllBMTree(Bag bag) {
		myBag = bag;
		allPlans = generateAllBMBkt(myBag.x.size()-1);
		allPlans.print();
		System.out.println("Done");
		/*for(Configuration plan : allPlans) {
			plan.printBM();
		}*/
	}
	/*
	private ArrayList<Configuration> generateAllBM(int i) {
		ArrayList<Configuration> plans = new ArrayList<Configuration>();
		for()
	}*/

	private Tree generateAllBMBkt(int i) {
		Tree tree;
		HashMap<String,MachineType> Clusters = new HashMap<String,MachineType>();
		if(i==0) {
			tree = new Tree();
			for(MachineType mt : Clusters.values()) {
				Tree initNode = new Tree(0,mt.name, mt.name.compareTo("fast") == 0 ? "slow" : "fast", 0,0,0);
				tree.kids.add(initNode);
			}			
		} else {
			System.out.println("job no " + i);
			tree = generateAllBMBkt(i-1);
			for(Tree kid : tree.kids) {				
				for(int m=0; m<=kid.nextFreeMachine; m++) {
					kid.kids.add(new Tree(i,kid.mtype,kid.otherType, m, kid.nextFreeMachine, kid.nextFreeMachineOtherType));
				}
				for(int m=0; m<=kid.nextFreeMachineOtherType; m++) {
					kid.kids.add(new Tree(i,kid.otherType, kid.mtype, m, kid.nextFreeMachineOtherType, kid.nextFreeMachine));
				}				
			}			
		}
		return tree;
	}

	public static void main (String args[]) {
		
		int size = args.length > 0 ? Integer.parseInt(args[0]) : 100;
		/*args related to distribution time interval are expressed in minutes by the user*/
		double mean = args.length > 1 ? Double.parseDouble(args[1])*60 : 15*60;
		double variance = args.length > 2 ? Double.parseDouble(args[2])*60 : 4*60;
	}
}
