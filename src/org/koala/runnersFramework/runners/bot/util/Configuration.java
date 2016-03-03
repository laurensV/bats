package org.koala.runnersFramework.runners.bot.util;

import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

public class Configuration {

	HashMap<Job,Machine> schedPlan;
		
	public Configuration(Job job, Machine m) {
		schedPlan = new HashMap<Job,Machine>();
		schedPlan.put(job, m);
	}

	public void add(Configuration config) {
		Set<Entry<Job,Machine>> mappings = config.schedPlan.entrySet();
		for(Entry<Job,Machine> mapping : mappings) {
			schedPlan.put(mapping.getKey(), mapping.getValue());
		}
	}

	public void printBM() {
		// TODO Auto-generated method stub
		long longestRun=Long.MIN_VALUE;
		double budget=0;
		HashMap<Machine,Long> runs = new HashMap<Machine,Long>();
		
		HashMap<String,MachineType> Clusters = new HashMap<String,MachineType>();
		
		for(Job j : schedPlan.keySet()) {
			Machine m = schedPlan.get(j);
			if (!runs.containsKey(schedPlan.get(j))) {
				runs.put(m,new Long(0));
			}
			long run = runs.get(m).longValue() + j.runtime.get(m.mt).longValue();
			runs.put(m, run);
			if(run>longestRun) longestRun=run;			 
		}
		
		for(Machine m : runs.keySet()) {
			budget += Math.ceil(runs.get(m).doubleValue()/(Clusters.get(m.mt).atu*60))*Clusters.get(m.mt).costUnit;
		}
		
		System.out.println("Budget: " + budget + "\tMakespan: " + longestRun);
	}
}
