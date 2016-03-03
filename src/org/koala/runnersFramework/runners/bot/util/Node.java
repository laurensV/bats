package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.Iterator;

public class Node {

	ArrayList<Double> jobs;
	double crtJob;
	double initialRt;
	double totalRt;
	double wastedRt;	
	int noJobs;
	int noTasksMigrated=0;
	int noTasksReplicated=0;
	public Task task;
	public int nATU;
	public boolean nodeFinished = false;
	public String clusterName;
	
	
	public Node() {

		jobs = new ArrayList<Double> ();
		totalRt = 0;
		wastedRt = 0;		
		noJobs = 0;

	}
	
	public int doneJob() {
		jobs.add(initialRt);
		totalRt += initialRt;
		initialRt=0;
		noJobs ++;
			
		if(task == null) return 0;
		
		//System.out.println("Task " + task.id);
		
		if(task.replicated) {
			if(task.notYetFinished) {
				task.notYetFinished = false;
				return 1;
			}
			return 0;
		} 
		if(task.migratedUnterminated) {
			if(task.notYetFinished) {
				task.notYetFinished = false;
				return 1;
			}
			return 0;
		}
		return 1;
	}
	
	public int doneJob(ArrayList<Integer> check) {
		jobs.add(initialRt);
		totalRt += initialRt;
		initialRt=0;
		noJobs ++;
			
		if(task == null) return 0;
		
		//System.out.println("Task " + task.id);
		
		Iterator<Integer> it = check.iterator();
		Integer found = null;
		while(it.hasNext()){
			Integer i = it.next();
			if(i.intValue() == task.id) {
				found = i;
				break;
			}
		}
		
		if(task.replicated) {
			if(task.notYetFinished) {
				task.notYetFinished = false;
				if(found!=null) check.remove(check.indexOf(found));
				return 1;
			}
			return 0;
		} 
		if(task.migratedUnterminated) {
			if(task.notYetFinished) {
				task.notYetFinished = false;
				if(found!=null) check.remove(check.indexOf(found));
				return 1;
			}
			return 0;
		}
		if(found!=null) check.remove(check.indexOf(found));
		return 1;
	}
}
