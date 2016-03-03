package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.Random;

import org.koala.runnersFramework.runners.bot.Item;

public class Chromosome {
	ArrayList<Integer> ind = null;
	ArrayList<Item> types = null;
	int fitness = 1;
	int costPlan;
	int noATUPlan;
	double makespan;
	double ceilMspan;
	long size;
	int atu;
	long budget;
	double timeLimit;
	boolean valid = true;
	int mutation = 15000;
	double blx = 0.3;
	
	// variables used for computing the hyperarea difference
	// good point 0 
	// bad point 1
	double scaledMakespan;
	double scaledCost;

	boolean limit = false; // for cheapest and fastest chrm;
	
	// constructor for testing purposes
	Chromosome() {}
	
	Chromosome(ArrayList<Integer> ind, ArrayList<Item> types, long size, int atu, long budget, double time){
		this.ind = ind;
		this.types = types;
		this.size = size;
		this.atu = atu;
		this.budget = budget;
		this.timeLimit = time;
		computeScheduleValues();
	}
	
	Chromosome(Chromosome c) {
		this.ind = new ArrayList<Integer>(c.ind);
		this.types = new ArrayList<Item>(c.types);
		this.fitness = c.fitness;
		this.costPlan = c.costPlan;
		this.noATUPlan = c.noATUPlan;
		this.makespan = c.makespan;
		this.ceilMspan = c.ceilMspan;
		this.size = c.size;
		this.atu = c.atu;
		this.budget = c.budget;
		this.valid = c.valid;
		this.mutation = c.mutation;
		this.blx = c.blx;
		this.limit = c.limit;
	}
	
	void computeScheduleValues() {
		double profit = 0;
		int cost = 0;
		for (int i = 0; i < ind.size(); i++) {
			profit += (double)ind.get(i) * types.get(i).profit;
			cost += (double)ind.get(i) * types.get(i).weight;
		}
		
		makespan = (double)size/(double)(atu*profit);
		makespan = Math.floor(makespan * 1000) / 1000.0;
		
		ceilMspan = Math.ceil((double)size/(double)(atu*profit));
		
		if (cost*ceilMspan > budget || cost <= 0 || profit <= 0 || ceilMspan <= 0 || makespan > timeLimit)
			valid = false;
		
		noATUPlan = (int) ceilMspan;
		costPlan = cost*noATUPlan;
	}
	
	String print() {
		String str = "";
		for (Integer i : ind) {
			str += i + " ";
		}
		return str;
		//return makespan + ";" + costPlan + ";" + fitness;
	}

	//classic
	public Chromosome[] crossoverClassic(Chromosome parent2, int maxItems) {
		Random r = new Random();
		Chromosome[] children = new Chromosome[2];
		children[0] = new Chromosome(new ArrayList<Integer>(), types, size, atu, budget, timeLimit);
		children[1] = new Chromosome(new ArrayList<Integer>(), types, size, atu, budget, timeLimit);
		children[0].valid = true;
		children[1].valid = true;
		int maxItems1 = 0, maxItems2 = 0;
		
		for (int i = 0; i < ind.size(); i++) {
			int coin = r.nextInt()%2;
			if (coin == 0) {
				int bit = r.nextInt()%32;
				int mask = (1 << bit) - 1;
				int child1 = (this.ind.get(i) & mask) | (parent2.ind.get(i) & ~mask);
				int child2 = (parent2.ind.get(i) & mask) | (this.ind.get(i) & ~mask);
				
				if (r.nextInt(mutation) == 1) {
					bit = r.nextInt()%32;
					child1 ^= (1 << bit);
				}
				
				if (r.nextInt(mutation) == 1) {
					bit = r.nextInt()%32;
					child2 ^= (1 << bit);
				}
					
				children[0].ind.add(child1);
				children[1].ind.add(child2);
				
				//check validity of chromosomes
				if (child1 > types.get(i).maxItems)
					children[0].valid = false;
				if (child2 > types.get(i).maxItems)
					children[1].valid = false;
				
				maxItems1 += child1;
				maxItems2 += child2;
			} else { 
				children[0].ind.add(this.ind.get(i));
				children[1].ind.add(parent2.ind.get(i));
				maxItems1 += this.ind.get(i);
				maxItems2 += parent2.ind.get(i);
			}
		}
		children[0].computeScheduleValues();
		children[1].computeScheduleValues();
		if (maxItems1 > maxItems)
			children[0].valid = false;
		if (maxItems2 > maxItems)
			children[1].valid = false;
		return children;
	}
	
	//BLX
	public Chromosome[] crossover(Chromosome parent2, int maxItems){
		Random r = new Random();
		Chromosome[] children = new Chromosome[2];
		children[0] = new Chromosome(new ArrayList<Integer>(), types, size, atu, budget, timeLimit);
		children[1] = new Chromosome(new ArrayList<Integer>(), types, size, atu, budget, timeLimit);
		children[0].valid = true;
		children[1].valid = true;

		int bit;
		int child1, child2;
		int maxItems1 = 0, maxItems2 = 0;
		for (int i = 0; i < ind.size(); i++) {
			int diff = (int) Math.abs((double)(parent2.ind.get(i) - this.ind.get(i)));

			int min = (int) Math.min(parent2.ind.get(i), this.ind.get(i));
			int max = (int) Math.max(parent2.ind.get(i), this.ind.get(i));
			min -= blx*diff;
			if (min < 0) min = 0;
			max += blx*diff;

			
			if (min == 0 && max == 0) {
				child1 = parent2.ind.get(i);
				child2 = this.ind.get(i);
			} else {
				if (min == max)
					max++;
				child1 = r.nextInt(max-min) + min;
				child2 = r.nextInt(max-min) + min;
			}
			if (r.nextInt(mutation) == 1) {
				bit = r.nextInt()%32;
				child1 ^= (1 << bit);
			}
			
			if (r.nextInt(mutation) == 1) {
				bit = r.nextInt()%32;
				child2 ^= (1 << bit);
			}
				
			children[0].ind.add(child1);
			children[1].ind.add(child2);
			
			//check validity of chromosomes
			if (child1 > types.get(i).maxItems)
				children[0].valid = false;
			if (child2 > types.get(i).maxItems)
				children[1].valid = false;
			maxItems1 += child1;
			maxItems2 += child2;
		}
		children[0].computeScheduleValues();
		children[1].computeScheduleValues();
		if (maxItems1 > maxItems)
			children[0].valid = false;
		if (maxItems2 > maxItems)
			children[1].valid = false;
		return children;
	}
	
	public void mutate() {
		Random r = new Random();
		int index = r.nextInt()%ind.size();
		int bit = r.nextInt()%32;
		int newVal = ind.get(index);
		newVal ^= (1 << bit); 
		ind.set(index, newVal);
	}
	
	public String toString() {
		String ret = "";
		for(int i = 0; i < types.size(); i++) {
			ret += ind.get(i) + " ";
		}
		return ret+"\n";
	}
}
