package org.koala.runnersFramework.runners.bot.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

public class SimulatedSchedule implements Serializable {

    long budget;
    long cost;
    int atus;
    HashMap<String, Integer> machinesPerCluster;
    public boolean extraBudget = false;
    long bDeltaN;
    public int deltaN;
    public int deltaNExtraB;
	public long budgetMax;

    public SimulatedSchedule(long budget, long cost, int atus,
            HashMap<String, Integer> machinesPerCluster) {
        super();
        this.budget = budget;
        this.cost = cost;
        this.atus = atus;
        this.machinesPerCluster = machinesPerCluster;
    }

    @Override
    public String toString() {
    	String machines = "";
    	Iterator sols = machinesPerCluster.entrySet().iterator();
    	while(sols.hasNext()) {
    		machines += sols.next() + "\t";
    	}
        return "\t" + budget + "\t" + bDeltaN + "\t" + cost + "\t" + atus + "\t" + machinesPerCluster;
    }
}
