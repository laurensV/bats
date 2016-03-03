package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class StableDistributedBag extends Bag {

	double mean;
	double stdev;
	

	public StableDistributedBag(int size, long seed, 
			double mean, double stdev, double alpha, double beta,  
			double speedFactor, int costFactor, int cost) {
		this.mean = mean;
		this.stdev = stdev;
		this.seed= seed;
		this.size= size;
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.cost = cost;
		x = new ArrayList<Double>();
		generateStableBoT(size, seed, mean, alpha, beta, stdev, speedFactor);		
	}
	
	public StableDistributedBag(int size, 
			double mean, double stdev,   
			double speedFactor, int costFactor, int cost) {
		this.mean = mean;
		this.stdev = stdev;		
		this.size= size;
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.cost = cost;
		x = new ArrayList<Double>();
		
	}
	
	private void generateStableBoT (long size, long seed, 
			double mean, double alpha, double beta, double dispersion, 
			double speedFactor) {
		Random seeder = new Random(seed);
		
		Random u = new Random(seeder.nextLong());
		Random z = new Random(seeder.nextLong());
				
		long et;
	
				
		double nu = dispersion/Math.sqrt(2);
		double delta = mean;
		
		double v, w;
		
		double Bab, Sab;
		double X;
		
		int negativeETCount = 0;
		if(alpha != 1) {
			for(int i=0 ; i<size ; i++) {
				v = Math.PI*(u.nextDouble()-0.5);
				w = -Math.log(z.nextDouble());

				Bab = Math.atan(beta*Math.tan(Math.PI*alpha*0.5))/alpha;
				Sab = Math.pow(1+beta*beta*Math.pow(Math.tan(Math.PI*alpha*0.5), 2), Math.pow(2*alpha,-1));
				X = Sab * Math.sin(alpha*(v+Bab))/Math.pow(Math.cos(v), Math.pow(alpha,-1))*
				Math.pow(Math.cos(v-alpha*(v+Bab))/w, (1-alpha)/alpha);

				et = (long) (X*nu+mean);	//in seconds, for accuracy
				while((et<=0) || (et>delta+nu*8)) {
					if(et<=0) negativeETCount ++;

					v = Math.PI*(u.nextDouble()-0.5);
					w = -Math.log(z.nextDouble());

					Bab = Math.atan(beta*Math.tan(Math.PI*alpha*0.5))/alpha;
					Sab = Math.pow(1+beta*beta*Math.pow(Math.tan(Math.PI*alpha*0.5), 2), Math.pow(2*alpha,-1));
					X = Sab * Math.sin(alpha*(v+Bab))/Math.pow(Math.cos(v), Math.pow(alpha,-1))*
					Math.pow(Math.cos(v-alpha*(v+Bab))/w, (1-alpha)/alpha);
					et = (long) (X*nu+mean);	//in seconds, for accuracy
				}
				
				x.add(new Double(et));				
			}	
		} else {
			for(int i=0 ; i<size ; i++) {
				v = u.nextDouble();
				X = mean + dispersion/Math.sqrt(2)*Math.tan(Math.PI*(v-0.5));
				et = (long) X;
				while((et<=0) || (et>delta+nu*8)) {
					negativeETCount ++;
					v = u.nextDouble();
					X = mean + dispersion/Math.sqrt(2)*Math.tan(Math.PI*(v-0.5));
					et = (long) X;
				}
				
				x.add(new Double(et));			
			}
		}
		//System.out.println("Negative ETs: " + negativeETCount);
	}

	public StableDistributedBag copy() {
		StableDistributedBag tmp = new StableDistributedBag(this.size, this.mean, this.stdev,
				this.speedFactor, this.costFactor, this.cost);
		for(Double j : this.x){
			tmp.x.add(j.doubleValue());			
		}
		tmp.realExpectation = this.realExpectation;
		tmp.realVariance = this.realVariance;
		tmp.printXLS = this.printXLS;
		tmp.printSampleSummary = this.printSampleSummary;
		tmp.printNodeStats = this.printNodeStats;
		return tmp;
	}
	
	public static void main(String args[]) {
		
		Random randomizedSeed = new Random(99999999L);

		StableDistributedBag sdb = new StableDistributedBag(1000, 
				randomizedSeed.nextLong(),
				15*60, Math.sqrt(5)*60, 1.5, 0,
				4, 3, 3);
		
	}

	@Override
	public double estimateExecutionTime(double theta) {
		if(useHisto) return super.estimateExecutionTime(theta);
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMostPopularRT(double theta) {
		if(useHisto) return super.estimateExecutionTime(theta);
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void updateDistributionParameterEstimates(double rt) {
		meanXExecution = (meanXExecution*(histogram.size()-1)+rt)/(histogram.size());
		
		double var = 0.0;
		int noTasks = 0;
		
		Set<Map.Entry<Integer,Occurrence>> setHisto = histogram.entrySet();
		for(Map.Entry<Integer,Occurrence> r : setHisto) {
			var += r.getValue().occurrence * Math.pow(r.getKey()-meanXExecution, 2);				
			noTasks += r.getValue().occurrence;
		}
		varXsqExecution = var/noTasks;
				
	}
}
