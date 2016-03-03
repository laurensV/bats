package org.koala.runnersFramework.runners.bot.util;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import org.math.plot.Plot2DPanel;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

public class ParetoSet {
	ArrayList<Chromosome> set = new ArrayList<Chromosome>();
	double hyperareaDifference; // lower better
	double slowHD;
	double paretoSpread;
	double minAbsoluteDist;
	double cluster;
	//double accuracy;
	BigDecimal sIn = BigDecimal.ZERO;
	BigDecimal sDo = BigDecimal.ZERO;
	double v = 20;
	int NDC = 0;
	Chromosome cheapest, fastest;
	double profitability = 0;
	double profitabilityNDC = 0;
	ArrayList<Chromosome> distinctChoices = new ArrayList<Chromosome>();
	//double profitabilityHalf = 0;

	public ParetoSet() {}

	// build Pareto Frontier starting from schedules in population
	public ParetoSet(ArrayList<Chromosome> population, Chromosome cheapest, Chromosome fastest) {
		this.set = buildPS(population);
		this.cheapest = cheapest;
		this.fastest = fastest;
		cheapest.scaledCost = 0;
		cheapest.scaledMakespan = 1;
		fastest.scaledCost = 1;
		fastest.scaledMakespan = 0;
	}
	
	void computeDistanceToUtopicPoint() {
		minAbsoluteDist = Double.MAX_VALUE;
		
		// fastest
		
		// cheapest
		double distance = Math.sqrt(Math.pow(cheapest.makespan-fastest.makespan,2));
		if (distance < minAbsoluteDist && distance != 0) {
			minAbsoluteDist = distance;
		}
		
		// fastest
		distance = Math.sqrt(Math.pow(fastest.costPlan-cheapest.costPlan,2));
		if (distance < minAbsoluteDist && distance != 0) {
			minAbsoluteDist = distance;
		}
		
		for (int i = 0; i < set.size(); i++) {
			//System.out.println("scaled cost " + set.get(i).scaledCost + "real cost" + set.get(i).costPlan);
			//System.out.println("scaled makespan " + set.get(i).scaledMakespan + "real makespan" + set.get(i).makespan);
			distance = Math.sqrt(Math.pow(set.get(i).makespan-fastest.makespan,2)  
					+ Math.pow(set.get(i).costPlan-cheapest.costPlan, 2));
			//System.out.println(distance + " distance");
			if (distance < minAbsoluteDist && distance != 0) {
				minAbsoluteDist = distance;
			}
		}
	}

	ArrayList<Chromosome> buildPS(List<Chromosome> population) {
		ArrayList<Chromosome> localSet = new ArrayList<Chromosome>();
		Collections.sort(population, new Comparator<Chromosome>(){
			@Override
			public int compare(Chromosome o1, Chromosome o2) {
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
				if (population.get(j).costPlan < population.get(i).costPlan) {
					i = j;
					done = false;
					break;
				}
			}
		}

		return localSet;
	}

	// set needs to be sorted increasing by cost before calling this method
	void computeParetoSpread() {
		double hCost = set.get(set.size()-1).costPlan - set.get(0).costPlan;
		double hMspan = set.get(0).makespan - set.get(set.size()-1).makespan;
		double HCost = fastest.costPlan - cheapest.costPlan;
		double HMspan = cheapest.makespan - fastest.makespan;
		
		paretoSpread = hCost*hMspan/(HCost*HMspan);
	}

	void computeScaledObjectives() {
		// sort increasing by cost
		Collections.sort(set, new Comparator<Chromosome>() {
			@Override
			public int compare(Chromosome o1, Chromosome o2) {
				return o1.costPlan - o2.costPlan;
			}
		});
		//Chromosome first = set.get(0);
		//Chromosome last = set.get(set.size()-1);
		//if (first.costPlan > last.costPlan) {
		//	System.out.println("Wrong sort!");
		//}


		// set scaled cost
		int diff = fastest.costPlan - cheapest.costPlan;
		int toRemove = -1;
//		first.scaledCost = 0; last.scaledCost = 1;
		for (int i = 0; i < set.size(); i++) {
			set.get(i).scaledCost = (set.get(i).costPlan - cheapest.costPlan)/(double)diff;
			if (set.get(i).scaledCost >= 1) {
				//System.out.println("cost grater than the cost for the fastest");
				set.get(i).scaledCost = 0.99;
				toRemove = i;
			}
		}
		if (toRemove >= 0) {
			set.remove(toRemove);
		}


		// sort increasing by makespan
		Collections.sort(set, new Comparator<Chromosome>() {
			@Override
			public int compare(Chromosome o1, Chromosome o2) {
				if (o1.makespan - o2.makespan < 0)
					return -1;
				else if (o1.makespan - o2.makespan > 0) 
					return 1;
				else return 0;
			}
		});
		//first = set.get(0);
		//last = set.get(set.size()-1);
		//if (first.makespan > last.makespan) {
		//	System.out.println("Wrong sort!");
		//}


		// set scaled makespan
		double diffMakespan = cheapest.makespan - fastest.makespan;
		toRemove = -1;
		for (int i = 0; i < set.size(); i++) {
			set.get(i).scaledMakespan = (double)((set.get(i).makespan-fastest.makespan)/(double)diffMakespan);
			if (set.get(i).scaledMakespan >= 1) {
				//System.out.println(set.get(i).print());
				//System.out.println("time grater than the time for the cheapest");
				set.get(i).scaledMakespan = 0.99;
				toRemove = i;
			}
		}
		if (toRemove >= 0) {
			set.remove(toRemove);
		}
	}


	double computeSum(int start, int end, int np, double maxM) {
		double sum = 0;
		if (end == np) {
			// compute last sum
			for (int i = start; i <= end; i++) {
				if (maxM == 0)
					sum += (1-set.get(i-1).scaledCost) * (1-set.get(i-1).scaledMakespan);
				else sum += (1-set.get(i-1).scaledCost) * (1-maxM);
			}
		} else {
			double maxCNew, maxMNew;
			for (int i = start; i <= end; i++) {
				if (maxM == 0)
					sum += computeSum(i+1, end + 1, np, set.get(i-1).scaledMakespan);
				else sum += computeSum(i+1, end  + 1, np, maxM);
			}
		}
		return sum;	
	}

	void prepareForComputations() {
		
		Collections.sort(set, new Comparator<Chromosome>(){
			@Override
			public int compare(Chromosome o1, Chromosome o2) {
				if (o1.scaledCost - o2.scaledCost < 0)
					return -1;
				else if (o1.scaledCost - o2.scaledCost > 0) 
					return 1;
				else return 0;
			}});

	} 

	void slowComputeHD() {
		prepareForComputations();

		System.out.println("Compute HyperAreaDifference");

		int np = set.size();
		// index starts at 1
		double sum = 0;
		for (int r = 1; r <= np; r++) {
			int sign = 0;
			if (r % 2 != 0)
				sign = 1;
			else sign = -1;

			// recursive call with start end interval for sum
			// depth of recursive call is set by r
			//System.out.println("sign = " + sign + " and r =" + r);
			sum += sign * computeSum(1, np - r + 1, np, 0);
		}

		slowHD = 1 - sum;
	}

	void computeMetrics(Combinations coef) {
		computeScaledObjectives();
		computeDistanceToUtopicPoint();
		prepareForComputations();
		
		
		computeHyperareaDifference(coef);
		computeParetoSpread();
		computeNDC();
		computeCluster();
		computeProfitability();
		computeProfitabilityNDC();
	}
	
	private void computeProfitabilityNDC() {
		double ideal = (double)cheapest.costPlan * fastest.makespan;
		double sumProf = 0;

		for (int i = 0; i < distinctChoices.size(); i++) {
			sumProf += ideal/(double)(distinctChoices.get(i).makespan * (double)distinctChoices.get(i).costPlan);
		}
		
		profitabilityNDC = sumProf / (double) distinctChoices.size();
	}

	private void computeCluster() {
		cluster = (set.size()+2)/(double)NDC;
	}

	private void computeProfitability() {
		double ideal = (double)cheapest.costPlan * fastest.makespan;
		
		double sumProf = 0;
		//double sumProfHalf = 0;
		sumProf +=  ideal/(double)(cheapest.makespan * (double)cheapest.costPlan);
		sumProf +=  ideal/(double)(fastest.makespan * (double)fastest.costPlan);
		for (int i = 0; i < set.size(); i++) {
			sumProf += ideal/(double)(set.get(i).makespan * (double)set.get(i).costPlan);
		}
		
		profitability = sumProf/(double) set.size();
	}

	void computeNDC(){
		HashMap<String, Boolean> squares = new HashMap<String, Boolean>();
		int m =  (int)Math.floor(cheapest.scaledMakespan * v);
		int c = (int)Math.floor(cheapest.scaledCost * v);
		String key = m + "," + c;
		squares.put(key, true);
		distinctChoices.add(cheapest);
		
		m = (int)Math.floor(fastest.scaledMakespan * v);
		c = (int)Math.floor(fastest.scaledCost * v);
		key = m + "," + c;
		squares.put(key, true);
		distinctChoices.add(fastest);
		
		for (int i = 0; i < set.size(); i++) {
			m = (int)Math.floor(set.get(i).scaledMakespan * v);
			c = (int)Math.floor(set.get(i).scaledCost * v);
			key = m + "," + c;
			if (squares.get(key) == null) {
				distinctChoices.add(set.get(i));
				squares.put(key, true);
			} 
		}
		NDC = squares.size();
	}
	
	// set has to be sorted decreasing by makespan (increasing by cost)
	void computeHyperareaDifference(Combinations coef) {
		BigDecimal one = BigDecimal.ONE;
		computeSin_Sdo(coef, true);
		//System.out.println(sIn);
		//System.out.println(one.subtract(sIn));
		hyperareaDifference = one.subtract(sIn).doubleValue();
	}

	
	void computeAccuracy(Combinations coef) {
		if (sIn == BigDecimal.ZERO) {
			System.out.println("sin not computed");
			prepareForComputations();
			computeSin_Sdo(coef, true);
		}
		BigDecimal one = BigDecimal.ONE;
		//System.out.println("sIn " + sIn);
		//System.out.println("sDo " + sDo);
		// computeSdo
		computeSin_Sdo(coef, false);
		//System.out.println("sIn " + sIn);
		//System.out.println("sDo " + sDo);
		BigDecimal part = one.subtract(sIn);
		//accuracy = part.subtract(sDo).doubleValue();
	}
	
	// for computing Sin second argument must be true, and for Sdo must be false
	void computeSin_Sdo(Combinations coef, boolean in){
		ArrayList<Chromosome> newSet = new ArrayList<Chromosome>();
		newSet.add(cheapest);
		newSet.addAll(set);
		newSet.add(fastest);
		
		int n = newSet.size();
		//	System.out.println(n);
		BigDecimal sum = BigDecimal.ZERO; 
		int sign;
		for (int r = 2; r <= n; r++) {
			//	System.out.println("adding to the sum");
			if (r%2 != 0) sign = 1;
			else sign = -1;
			//	System.out.println(sign);
			for (int i = 1; i <= n-r+1; i++) 
				for (int j = i+r-1; j <= n; j++) {
					BigDecimal decimalCoef = new BigDecimal(coef.comb[j-i-1][r-2]);
					if (in)
						sum = sum.add(decimalCoef.multiply(new BigDecimal((sign*(1-newSet.get(i-1).scaledMakespan)*(1-newSet.get(j-1).scaledCost)))));
					else 
						sum = sum.add(decimalCoef.multiply(new BigDecimal((sign*(1-newSet.get(i-1).scaledCost)*(1-newSet.get(j-1).scaledMakespan)))));
				}
		}
		for (int i = 0; i < n; i++)
			sum = sum.add(new BigDecimal(1 - newSet.get(i).scaledCost).multiply(new BigDecimal(1 - newSet.get(i).scaledMakespan)));
		if (in)
			sIn = sum;
		else sDo = sum;
	}
	
	void printMetrics() {
		System.out.println("hyperarea difference ");
		System.out.println(hyperareaDifference);
		
		System.out.println("pareto spread ");
		System.out.println(paretoSpread);
		
		System.out.println("number of distinct choices");
		System.out.println(NDC);
		
		System.out.println("cluster ");
		System.out.println(cluster);
		
		System.out.println("profitability ");
		System.out.println(profitability);
		
		System.out.println("profitabilityNDC");
		System.out.println(profitabilityNDC);
		
		System.out.println("distance to utopic point");
		System.out.println(minAbsoluteDist);
		
		//System.out.println(hyperareaDifference+";"+paretoSpread+";"+NDC+";"+cluster+";"+profitability+";"+minAbsoluteDist);
		//System.out.println(profitabilityHalf);

		System.out.println("set size ");
		System.out.println((set.size()+2));
		System.out.println("set");
		System.out.println(cheapest.makespan+";"+cheapest.costPlan);
		for (int i = 0; i < set.size(); i++) {
			System.out.println(set.get(i).makespan+";"+set.get(i).costPlan);
		}
		System.out.println(fastest.makespan+";"+fastest.costPlan);
		
		

	}
	
	void plot(ArrayList<Chromosome> set1, ArrayList<Chromosome> set2) {
		double[] x = new double[set.size()];
		double[] y = new double[set.size()];
		
		for (int i = 0; i < set1.size(); i++) {
			x[i] = set1.get(i).makespan;
			y[i] = set1.get(i).costPlan;
		}
		
		double[] X = new double[set2.size()];
		double[] Y = new double[set2.size()];
		
		for (int i = 0; i < set2.size(); i++) {
			X[i] = set2.get(i).makespan;
			Y[i] = set2.get(i).costPlan;
		}
		Plot2DPanel plot = new Plot2DPanel();
		plot.addScatterPlot("A", Color.black, x, y);
		plot.addScatterPlot("A", Color.black, X, Y);
		plot.addLinePlot("my plot", x, y);
		plot.addLinePlot("tralal", X, Y);
		JFrame frame = new JFrame("a plot panel");
		frame.setSize(600, 600);
		frame.setContentPane(plot);
		frame.setVisible(true);
		
	}

	public void sanityCheck() {
		HashMap<Chromosome, Boolean> toRemove = new HashMap<Chromosome, Boolean>();
		
		for (int i = 0; i < set.size(); i++) {
			if (set.get(i).makespan >= cheapest.timeLimit) {
				toRemove.put(set.get(i), true);
			}
		}
		Iterator<Map.Entry<Chromosome,Boolean>> it = toRemove.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<Chromosome,Boolean> pairs = (Map.Entry<Chromosome,Boolean>)it.next();
	        set.remove(pairs.getKey());
	    }
	}
}
