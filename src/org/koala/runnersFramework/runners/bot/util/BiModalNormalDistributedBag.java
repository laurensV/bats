package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.Random;

public class BiModalNormalDistributedBag extends Bag {

	double mean1, mean2;
	double stdev1, stdev2;
	double alpha;
	long seed2;
	long seed1;
	
	public BiModalNormalDistributedBag(int size, 
			long seed1, double mean1, double stdev1, 
			long seed2, double mean2, double stdev2, 
			double alpha, long alphaSeed,
			double speedFactor, int costFactor, int cost) {
		this.mean1 = mean1;
		this.stdev1 = stdev1;
		this.mean2 = mean2;
		this.stdev2 = stdev2;
		this.alpha = alpha;
		this.seed1 = seed1;
		this.seed2 = seed2;
		this.size= size;
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.cost = cost;
		x = new ArrayList<Double>();
		generateBiModalNormalDistributedBoT(size, seed1, mean1, stdev1, 
												  seed2, mean2, stdev2, 
												  alpha, alphaSeed, speedFactor);		
	}
	
	public BiModalNormalDistributedBag(int size, double mean1, double stdev1, 
			double mean2, double stdev2,
			double alpha,
			double speedFactor, int costFactor, int cost) {
		this.size = size;
		this.mean1 = mean1;
		this.stdev1 = stdev1;	
		this.mean2 = mean2;
		this.stdev2 = stdev2;
		this.alpha = alpha;
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.cost = cost;	
		x = new ArrayList<Double>();
	}
	
	
	private void generateBiModalNormalDistributedBoT (int size, 
			long seed1, double mean1, double stdev1, 
			long seed2, double mean2, double stdev2, 
			double alpha, long alphaSeed,
			double speedFactor) {
		
		long et;

		Random random1 = new Random(seed1);
		Random random2 = new Random(seed2);
		Random whichD = new Random(alphaSeed);
			
		int negative = 0;
		int count1 = 0;
		int count2 = 0;
		
		for(int i=0 ; i<size ; i++) {
			if(whichD.nextDouble() < alpha) {
				et = (long) (random1.nextGaussian()*stdev1+mean1);	//in seconds, for accuracy
				while(et<=0) {
					negative ++;
					et = (long) (random1.nextGaussian()*stdev1+mean1);	//in seconds, for accuracy
				}
				count1 ++;
			} else {
				et = (long) (random2.nextGaussian()*stdev2+mean2);	//in seconds, for accuracy
				while(et<=0) {
					negative ++;
					et = (long) (random2.nextGaussian()*stdev2+mean2);	//in seconds, for accuracy
				}
				count2 ++;
			}				
			x.add(new Double(et));		
			
		}	
		//System.out.println("count1=" + count1 + "; count2=" + count2);
	}
	
	public BiModalNormalDistributedBag copy() {
		BiModalNormalDistributedBag tmp = new BiModalNormalDistributedBag(this.size, 
				this.mean1, this.stdev1,
				this.mean2, this.stdev2, 
				this.alpha,
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
		long experimentBagGeneratingSeed1 = randomizedSeed.nextLong();
		long experimentBagGeneratingSeed2 = randomizedSeed.nextLong();
		Random alphaRandomizedSeed = new Random(88888888L);
		long alphaSeed = alphaRandomizedSeed.nextLong();
		BiModalNormalDistributedBag bmnd = new BiModalNormalDistributedBag(1000,
				experimentBagGeneratingSeed1, 48, 29,
				experimentBagGeneratingSeed2, 689,74, 
				0.3,alphaSeed,4,3,3);
		bmnd.print();
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
		// TODO Auto-generated method stub
		
	}
	
}
