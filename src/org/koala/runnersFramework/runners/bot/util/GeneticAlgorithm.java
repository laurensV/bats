package org.koala.runnersFramework.runners.bot.util;


import org.koala.runnersFramework.runners.bot.Item;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.JFrame;

public class GeneticAlgorithm {
	Item[] items;
	int N;
	int wmin;
	int W;
	private long size;
	private long budget;
	private double time;
	private int atu;
	private int maxItems;
	public int noATUPlan; //makespan
	public long costPlan; //total cost
	
	public boolean debug = false;
	public boolean printConfig = true;
	public boolean success;
	ArrayList<Item> types = null;
	JFrame frame = null;
	PrintWriter out = null;
	
	ParetoSet ps = null;
	LinkedList<Double> history = new LinkedList<Double>(); 
	Combinations coef = null;
	
	ArrayList<Chromosome> allPS = new ArrayList<Chromosome>();
	ArrayList<Chromosome> population = null;
	ArrayList<Chromosome> badPopulation = null;
	ArrayList<Chromosome> intermediatePopulation = null;
	ArrayList<Chromosome> newPopulation = null;
	ArrayList<Chromosome> realPopulation = null;
	ArrayList<Chromosome> realParetoFr = null;
	ArrayList<Chromosome> balancedChrmList = new ArrayList<Chromosome>();
	ArrayList<BigDecimal> hdSlope = new ArrayList<BigDecimal>();
	LinkedList<BigDecimal> hdList = new LinkedList<BigDecimal>();
	Chromosome finalSol, cheapest, fastest, mostExpensive;
	int cheapestIndex, mostProfitIndex;
	
	int populationSize = 2000;
	
	
	//granularity in Cost and Makespan for the fitness computation
	int fntResCost = 500; //100
	int fntResMspan = 20; //20
	double elitism = 0.30;
	double crossover = 0.3; // 2*crossover parents
	double smartSelMspan = 0.2;
	double smartSelCost = 0.2;
	double smartSelSum = 0.5;
	//double schedule = 0.5;
	//int paretoSizeLimit = 15;
	Hashtable<String,Integer> typesHT = null;
	int totalFitness = 0;
	int noIter = 500;

	public GeneticAlgorithm(Item[] items, long budget, long size, int minCATU, int allCostATU, int atu, int maxItems) {
		this.items = items;
		wmin = minCATU;
		W = allCostATU;
		this.size = size;
		N = items.length - 1;
		this.atu = atu;
		this.maxItems = maxItems;
		
		intermediatePopulation = new ArrayList<Chromosome>();
		population = new ArrayList<Chromosome>();
		types = new ArrayList<Item>();
		
		typesHT = new Hashtable<String, Integer>();
		
		int minWeight = Integer.MAX_VALUE;
		double maxProfit = Double.MIN_VALUE;
		
		
		for (int i = 1; i < items.length; i++) {
			Integer count = typesHT.get(items[i].cluster);
			if (count == null) {
				typesHT.put(items[i].cluster, 1);
				types.add(items[i]);
			}
			else typesHT.put(items[i].cluster, ++count);
		}
		
		for (int i = 0; i < types.size(); i++) {
			if (types.get(i).weight < minWeight) {
				minWeight = types.get(i).weight;
				cheapestIndex = i;
			}
			types.get(i).maxItems = typesHT.get(types.get(i).cluster);
		}
		
		for (int i = 0; i < types.size(); i++) {
			double profitability = (types.get(cheapestIndex).weight * 
					(1/types.get(cheapestIndex).profit))/ (double) (types.get(i).weight * (1/types.get(i).profit));
			if (profitability > maxProfit) {
				mostProfitIndex = i;
				maxProfit = profitability;
			}
		}
		
		coef = new Combinations(200);
	}
	
	public void testHD() {
		ParetoSet testPS = new ParetoSet();
		ArrayList<Chromosome> set = new ArrayList<Chromosome>();

		Chromosome c1 = new Chromosome();
		Chromosome c2 = new Chromosome();
		Chromosome c3 = new Chromosome();
		Chromosome c4 = new Chromosome();
		Chromosome c5 = new Chromosome();
		c1.costPlan = 168;
		c2.costPlan = 165;
		c3.costPlan = 124;
		c4.costPlan = 100;
		c5.costPlan = 200;
		
		c1.makespan = 57.3;
		c2.makespan = 63.1;
		c3.makespan = 164.9;
		c4.makespan = 1;
		c5.makespan = 221;
		for (int i = 0; i < 30; i++) {
			Chromosome c = new Chromosome();
			c.costPlan = 101 + i;
			c.makespan = 220 - i;
			set.add(c);
		}
		
		//set.add(c1); set.add(c2); set.add(c3);
		set.add(c4); set.add(c5);
		
		testPS.set = set;
		long start = System.currentTimeMillis();
		//testPS.slowComputeHD();
		long end = System.currentTimeMillis();
		System.out.println("Time = " + (end - start)/(double)1000);
		
		start = System.currentTimeMillis();
		testPS.computeMetrics(coef);
		testPS.computeAccuracy(coef);
		end = System.currentTimeMillis();
		System.out.println("Time(fast) = " + (end - start)/(double)1000);
		System.out.println("HD = " + testPS.hyperareaDifference);
		
		System.exit(0);
	}
	
	public void computePSMeasurements() {
		ParetoSet ps = new ParetoSet(population, cheapest, fastest);
		ps.computeMetrics(coef);
		ps.computeAccuracy(coef);
		ps.computeParetoSpread();
	}
	
	public HashMap<String, Integer> findSol() {
		
		//testHD();
		
		HashMap<String, Integer> finalSolution = new HashMap<String, Integer>();
		boolean distanceToOCond = false;
		boolean hdCond = false;
		boolean printed = false;
		
		double lastDistanceToO = 0.0;
		
		realParetoFr = new ArrayList<Chromosome>();
		realPopulation = new ArrayList<Chromosome>();
		
		computeFastestSchedule();
		computeCheapestSchedule();
		createInitPopulation();
		
		population.add(cheapest);
		population.add(fastest);
	
		//testRandomSmallPopulation();
		
		long start = System.currentTimeMillis();
		int i = 0;
		double lastHD = 1;
		ps = null;
		do {
			genAlgIteration();
			
			ArrayList<Chromosome> improvedPopulation = new ArrayList<Chromosome>();
			improvedPopulation.addAll(population);
			improvedPopulation.addAll(allPS);
			ps = new ParetoSet(improvedPopulation, cheapest, fastest);
			allPS.addAll(ps.set);
			ps.sanityCheck();
			ps.computeMetrics(coef);
			
			if (hdCond == true && distanceToOCond == false) {
				
				if (lastDistanceToO == ps.minAbsoluteDist) {
					distanceToOCond = true;
				}
			}
			
			if (lastHD != 1 && hdCond == false) {
				if (ps.hyperareaDifference == lastHD ) {
					hdCond = true;
				}
			}

			//System.out.println(ps.profitability + " profit");
			//System.out.println(ps.profitabilityNDC);
			
			lastHD = ps.hyperareaDifference;
			lastDistanceToO = ps.minAbsoluteDist;
			
			if (hdCond && distanceToOCond) {
				if (!printed) {
					long timeStopCond = System.currentTimeMillis();
					
					System.out.println("Iteration \n" + i);
					System.out.println("Execution time in sec \n" + ((timeStopCond - start)/1000));
					
					
					ps.printMetrics();
					printed = true;
				}
			}
			
			
			i++;
			/*if (i >= noIter ) {
				break;	
			}*/
			
		} while (!printed);
		
		
		/*long end = System.currentTimeMillis();
		System.out.println("Iteration \n" + (i-1));
		System.out.println("Execution time in sec \n" + ((end - start)/1000));
		ps.printMetrics();
		*/
		//System.out.println("Pareto frontier size " + ps.set.size());
		
	/*	start = System.currentTimeMillis();
		
		generateRealPopulation(0, 0, new Chromosome(new ArrayList<Integer>(Collections.nCopies(types.size(), 0)), types, size, atu, budget));
		
		ParetoSet realPS = new ParetoSet(); 
		realParetoFr = ps.buildPS(realParetoFr);
		realPS.fastest = realParetoFr.get(0);
		realPS.cheapest = realParetoFr.get(realParetoFr.size()-1);
		
		for (int j = 1; j < realParetoFr.size()-1; j++) {
			realPS.set.add(realParetoFr.get(j));
		}
		
		realPS.computeMetrics(coef);
		end = System.currentTimeMillis();
		
		System.out.println("Real PS ");
		System.out.println("Execution time in sec \n" + ((end - start)/1000));
		realPS.printMetrics();
		*/
		//plotParetoFrontier();
		//printFrontiers();
		finalSol = ps.set.get(0);
		for (i = 0; i < finalSol.ind.size(); i++) {
			finalSolution.put(finalSol.types.get(i).cluster, finalSol.ind.get(i));
		}
		this.costPlan = finalSol.costPlan;
		this.noATUPlan = finalSol.noATUPlan;
		return finalSolution;
	}
	
	private void testRandomSmallPopulation() {
		addRandomChromosomes();
		removeInvalid();
		
		badPopulation = new ArrayList<Chromosome>();
		double deltaTime = (cheapest.makespan - fastest.makespan)/50 + fastest.makespan;
		double deltaCost = (fastest.costPlan - cheapest.costPlan)/2 + cheapest.costPlan;
		System.out.println(deltaTime + " " + deltaCost);
		for (int i = 0; i < population.size(); i++) {
			if (population.get(i).costPlan > deltaCost && population.get(i).makespan > deltaTime) {
				badPopulation.add(population.get(i));
			}
		}
		System.out.println(badPopulation.size());
	}
	
	private void plotPSComparison() {
		System.out.println("Bad PS ");
		ParetoSet badPS = new ParetoSet(badPopulation, cheapest, fastest);
		
		badPS.computeMetrics(coef);
		badPS.set.add(0, cheapest);
		badPS.set.add(fastest);
		ps.set.add(0, cheapest);
		ps.set.add(fastest);
		
		badPS.plot(badPS.set, ps.set);
	}

	private void printFrontiers() {
		System.out.println("estimated");
		for (Chromosome c: ps.set) {
			System.out.println(c.makespan + " " + c.costPlan);
		}
		System.out.println("real");
		for (Chromosome c: realParetoFr) {
			System.out.println(c.makespan + " " + c.costPlan);
		}
	}

	private void generateRealPopulation(int typeIndex, int total, Chromosome chrm) {
		int nrItems = 0;
		if (typeIndex >= types.size()) {
			if (total > 0 && chrm.valid) {
				if (realParetoFr.size() == 0 || realParetoFr.size() == 1) {
					realParetoFr.add(chrm);
				} else {
					boolean add = false;
					//take lower makespan
					if (chrm.makespan < realParetoFr.get(0).makespan && chrm.costPlan >= realParetoFr.get(0).costPlan)
						add = true;
					
					//take lower cost
					if (chrm.costPlan < realParetoFr.get(realParetoFr.size()-1).costPlan 
							&& chrm.makespan >= realParetoFr.get(realParetoFr.size()-1).makespan) 
						add = true;
					
					for (int i = 0; i < realParetoFr.size()-1 && add == false; i++) {
						if (chrm.costPlan < realParetoFr.get(i).costPlan && chrm.costPlan >= realParetoFr.get(i+1).costPlan && 
								chrm.makespan < realParetoFr.get(i+1).makespan && chrm.makespan >= realParetoFr.get(i).makespan){
							add = true;
							break;
						}
						if (chrm.costPlan <= realParetoFr.get(i).costPlan && chrm.makespan <= realParetoFr.get(i).makespan) {
							add = true;
							//System.out.println("removing " + realParetoFr.get(i).makespan +  " " + realParetoFr.get(i).costPlan);
							realParetoFr.remove(i);
							break;
						}
						if (chrm.costPlan <= realParetoFr.get(i+1).costPlan && chrm.makespan <= realParetoFr.get(i+1).makespan) {
							add = true;
							//System.out.println("removing " + realParetoFr.get(i).makespan +  " " + realParetoFr.get(i).costPlan);
							realParetoFr.remove(i+1);
							break;
						}
					}
					if (add) {
						realParetoFr.add(chrm);
						realParetoFr = ps.buildPS(realParetoFr);
					}
				}
			}
		} else {
			while(total+nrItems <= maxItems && nrItems <= types.get(typeIndex).maxItems) {
				chrm.ind.set(typeIndex, nrItems);
				chrm.computeScheduleValues();
				if (chrm.valid == true || nrItems==0)
					generateRealPopulation(typeIndex+1, total+nrItems, new Chromosome(chrm));
				chrm.valid = true;
				nrItems++;
			}
		}
	}

	// compute the cheapest schedule 
	// one instance of the cheapest machine
	private void computeCheapestSchedule() {
		int cost = Integer.MAX_VALUE;
		double timeCheapest = Double.MAX_VALUE;
		ArrayList<Integer> chrm = new ArrayList<Integer>(Collections.nCopies(types.size(), 0));
		for (int i = 1; i <= types.get(mostProfitIndex).maxItems; i++) {
			chrm.set(mostProfitIndex, i);
			Chromosome current = new Chromosome(new ArrayList<Integer>(chrm), types, size, atu, budget, Double.MAX_VALUE);
			if (current.costPlan < cost) {
				cost = current.costPlan;
				cheapest = current;
			} else if (current.costPlan == cost) {
				if (current.makespan < timeCheapest) {
					timeCheapest = current.makespan;
					cheapest = current;
				}
			}
		}
		cheapest.limit = true;
		time = cheapest.makespan; 
		//System.out.println("cheapest " + cheapest.print());
		//System.out.println("cheapest  " + cheapest.costPlan + " " + cheapest.makespan);
	}
	
	// compute the fastest schedule
	// max number of machines of the most profitable type - greedy approach
	private void computeFastestSchedule() {
		ArrayList<Integer> chrm = new ArrayList<Integer>();
		ArrayList<Item> sortedDecByProfit = new ArrayList<Item>(types);
		
		Collections.sort(sortedDecByProfit, new Comparator<Item>() {
			@Override
			public int compare(Item o1, Item o2) {
				if (o2.profit-o1.profit < 0)
					return -1;
				else if (o2.profit == o1.profit)
					return 0;
				else return 1;
			}
		});
		
		chrm = new ArrayList<Integer>();
		for (int i = 0; i < types.size(); i++) {
			chrm.add(0);
		}
		int count = maxItems;
		int index = 0;
		while(count > 0 && index < sortedDecByProfit.size()){
			int noItemsTypeIndex = sortedDecByProfit.get(index).maxItems;
			if (count >= noItemsTypeIndex){
				count -= noItemsTypeIndex;	
			} else {
				noItemsTypeIndex = count;
				count = 0;
			}
			for (int i = 0; i < types.size(); i++) {
				if (types.get(i).cluster.equals(sortedDecByProfit.get(index).cluster)) {
					chrm.set(i, noItemsTypeIndex);
				}
			}
			index++;
		}
		fastest = new Chromosome(chrm, types, size, atu, Long.MAX_VALUE, Double.MAX_VALUE);
		budget = fastest.costPlan;
		fastest.limit = true;
		
	}
	
	private void addRandomChromosomes() {
		int count = populationSize;
		Random r = new Random();
		int total = 0;
		while (count > 0 && population.size() < populationSize) {
			total = 0;
			ArrayList<Integer> chrm = new ArrayList<Integer>();
			for (int i = 0; i < types.size(); i++) {
				int max = types.get(i).maxItems;
				if (maxItems < total) break;
				int nextInt = r.nextInt(Math.min(max+1,maxItems+1-total));
				total += nextInt;
				chrm.add(nextInt);
			}
			//if the number of machines for the randomly generated schedule
			//does not exceed the limit on the overall number of machines
			//it is added to the population
			if (total <= maxItems) {
				Chromosome cm =  new Chromosome(chrm, types, size, atu, budget, time);
				if (cm.valid == true) {
					count--;
					population.add(cm);
				}
			}
		}
	}
	
	
	public void createInitPopulation() {
		//printPopulation();
		addRandomChromosomes();
		//printPopulation();
	}
	
	public void removeInvalid(){
		// remove invalid chromosomes
		Iterator<Chromosome> it = population.iterator();
		while (it.hasNext()) {
			Chromosome chrm = it.next();
			if (chrm.valid == false)
				it.remove();
		}
		
		it = intermediatePopulation.iterator();
		while (it.hasNext()) {
			Chromosome chrm = it.next();
			if (!chrm.valid)
				it.remove();
		}
	}
	
	public void genAlgIteration() {
		//remove invalid chromosomes
		while (population.size() < populationSize) {
			//removeInvalid();
			addRandomChromosomes();
			removeInvalid();
		}

		//find valid max and min cost chromosomes 
		Collections.sort(population, new Comparator<Chromosome>(){
			@Override
			public int compare(Chromosome arg0, Chromosome arg1) {
				return arg0.costPlan - arg1.costPlan;
			}
		});

		Chromosome costMin = population.get(0);
		Chromosome costMax = population.get(population.size()-1);
		
		//find valid max and min cost chromosomes 
		Collections.sort(population, new Comparator<Chromosome>(){
			@Override
			public int compare(Chromosome arg0, Chromosome arg1) {
				if (arg0.makespan > arg1.makespan)
					return 1;
				else if (arg0.makespan == arg1.makespan)
					return 0;
				else return -1;
			}
		});

		
		
		Chromosome mspanMin = population.get(0);
		Chromosome mspanMax = population.get(population.size()-1);
	
		//compute band dimensions
		int bandCostSize = (costMax.costPlan - costMin.costPlan + fntResCost)/fntResCost;
		// multiplied by 100 to be able to have double values
		int bandMspanSize = ((int)(mspanMax.makespan*100 - mspanMin.makespan*100) + fntResMspan)/fntResMspan;
		
		//compute fitness
		int maxFitness = Integer.MIN_VALUE;
		for (int i = 0; i < fntResCost; i++)
			for (int j = 0; j < fntResMspan; j++) {
				double c1 = costMin.costPlan + i * bandCostSize;
				double c2 = costMin.costPlan + (i+1) * bandCostSize;
				double m1 = mspanMin.makespan + (j * bandMspanSize)/100;
				double m2 = mspanMin.makespan + ((j + 1) * bandMspanSize)/100;
			
				int fitness = (int)(1000000 * (Math.max((double)(fntResCost-i)/(double)fntResCost, (double)(fntResMspan-j)/(double)fntResMspan) * 
						(double)(fntResCost-i)/(double)fntResCost * (double)(fntResMspan-j)/(double)fntResMspan));
				
				if (fitness <= 0) fitness = 1;
				
				for (Chromosome chrm : population) {
					if (!chrm.limit && chrm.costPlan <= c2 && chrm.costPlan >= c1 && chrm.makespan <= m2 && chrm.makespan >= m1) {
						chrm.fitness = fitness;
					}
					
				}
			}
		
		Collections.sort(population, new Comparator<Chromosome>(){
			@Override
			public int compare(Chromosome o1, Chromosome o2) {
				return o2.fitness - o1.fitness;
			}
		});
		
		//compute intermediate population
		intermediatePopulation = new ArrayList<Chromosome>();
		removeInvalid();
		
		//elitism
		int count = 0;
		Iterator<Chromosome> it = population.iterator();
		while(it.hasNext()) {
			Chromosome chrm = it.next();
			intermediatePopulation.add(chrm);
			count++;
			if (count > population.size()*elitism)
				break;
		}

		//crossover
		computeTotalFitness();
		count = 0;
		
		int initialPopulationSize = population.size();
		while (count < initialPopulationSize*crossover) {
			count++;
			Chromosome parent1 = smartWheelSelection();
			Chromosome parent2 = smartWheelSelection();

			//System.out.println(parent1);
			Chromosome[] children = parent1.crossover(parent2, maxItems);

			intermediatePopulation.add(children[0]);
			intermediatePopulation.add(children[1]);
			
			
		}

		//remove invalid elements - for intermediate population this time
		removeInvalid();

		//smart selection
		//create new population from intermediate population

		//smartSelection();

		population = intermediatePopulation;
		//population = newPopulation;
	}
	
	private void smartSelection() {
		newPopulation = new ArrayList<Chromosome>();
		
		//sort by makespan
		Collections.sort(intermediatePopulation, new Comparator<Chromosome>() {
			@Override
			public int compare(Chromosome arg0, Chromosome arg1) {
				if (arg0.makespan > arg1.makespan)
					return 1;
				else if (arg0.makespan == arg1.makespan)
					return 0;
				else return -1;
			}
		});
		
		Iterator<Chromosome> it = intermediatePopulation.iterator();
		int count = 0;
		int size = intermediatePopulation.size();
		while (count <= size*smartSelMspan && it.hasNext()) {
			count++;
			newPopulation.add(it.next());
			it.remove();
		}
		
		//sort by cost
		Collections.sort(intermediatePopulation, new Comparator<Chromosome>() {
			@Override
			public int compare(Chromosome arg0, Chromosome arg1) {
				return arg0.costPlan - arg1.costPlan;
			}
		});
		
		it = intermediatePopulation.iterator();
		count = 0;
		while (count <= size*smartSelCost && it.hasNext()) {
			count++;
			newPopulation.add(it.next());
			it.remove();
		}
		
		//sort by sum of cost and time
		Collections.sort(intermediatePopulation, new Comparator<Chromosome>() {
			@Override
			public int compare(Chromosome arg0, Chromosome arg1) {
				if (arg0.makespan+arg0.costPlan > arg1.makespan+arg1.costPlan)
					return 1;
				else if (arg0.makespan+arg0.costPlan == arg1.makespan+arg1.costPlan)
					return 0;
				else return -1;
			}
		});
		
		it = intermediatePopulation.iterator();
		count = 0;
		while (count <= size*smartSelSum && it.hasNext() && newPopulation.size() <= populationSize) {
			count++;
			newPopulation.add(it.next());
			it.remove();
		}
	}
	
	private void computeTotalFitness() {
		totalFitness = 0;
		for (Chromosome chrm : population) {
			totalFitness += chrm.fitness;
		}
	}

	private Chromosome smartWheelSelection() {
		Random r = new Random();
		int select = r.nextInt(totalFitness);
		int total = 0;
		Iterator<Chromosome> it = population.iterator();
		while(it.hasNext()) {
			Chromosome chrm = it.next();
			total += chrm.fitness;
			if (select <= total){
				totalFitness -= chrm.fitness;
				it.remove();
				return chrm;
			}
		}
		return null;
	}
	
	public void printPopulation(String str, PrintWriter out){
		for (Chromosome chrm : population) {
			out.println(chrm.print()+";" + str);
		}
	}
}