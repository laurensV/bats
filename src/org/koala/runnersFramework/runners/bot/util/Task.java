package org.koala.runnersFramework.runners.bot.util;

import java.util.HashMap;


public class Task {

	public int id;
	public double rt;
	public boolean replicated = false;
	public double tau;
	public double elapsed;
	public double estimatedOverMRt;
	public boolean notYetFinished = true;
	public boolean migrated = false;
	public boolean migratedUnterminated = false;
	public double moreTasks;
	public int timesReplicated = 0;
	public double estimatedRt = 0;
	public Node primary;
	
	public HashMap<String,Node> replicas; 
	
	public Task(int id, double rt) {
		this.id = id;
		this.rt = rt;
		replicas = new HashMap<String,Node>();
	}
}
