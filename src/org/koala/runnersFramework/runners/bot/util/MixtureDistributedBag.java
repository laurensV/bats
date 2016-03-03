package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.Random;

public class MixtureDistributedBag extends Bag {

	double mean1, mean2;
	double stdev1, stdev2;
	double alpha;
	double low2;
	double high2;
	double mean3;
	double stdev3;
	double low4;
	double high4;
	double beta;
	double gamma;
	long seed4;
	long seed3;
	long seed2;
	long seed1;
	
	public MixtureDistributedBag(int size, 
			long seed1, double mean1, double stdev1, 
			long seed2, double low2, double high2, 
			long seed3, double mean3, double stdev3, 
			long seed4, double low4, double high4,
			double alpha, double beta, double gamma, long whichDSeed,
			double speedFactor, int costFactor, int cost) {
		this.mean1 = mean1;
		this.stdev1 = stdev1;
		this.seed1 = seed1;		
		this.low2 = low2;
		this.high2 = high2;
		this.seed2 = seed2;
		this.mean3 = mean3;
		this.stdev3 = stdev3;
		this.seed3 = seed3;
		this.low4 = low4;
		this.high4 = high4;
		this.seed4 = seed4;
		this.alpha = alpha;		
		this.beta = beta;	
		this.gamma = gamma;		
		this.size= size;
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.cost = cost;
		x = new ArrayList<Double>();
		generateMixtureDistributedBoT(size, seed1, mean1, stdev1, 
											seed2, low2, high2, 
											seed3, mean3, stdev3,
											seed4, low4, high4,
											alpha, beta, gamma,
												  whichDSeed, 
												  speedFactor);		
	}
	
	public MixtureDistributedBag(int size,  
			double mean1, double stdev1, double low2, double high2, 
			double mean3, double stdev3, double low4, double high4,
			double speedFactor, int costFactor, int cost) {
		this.size = size;
		this.mean1 = mean1;
		this.stdev1 = stdev1;	
		this.low2 = low2;
		this.high2 = high2;
		this.mean3 = mean3;
		this.stdev3 = stdev3;
		this.low4 = low4;
		this.high4 = high4;
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.cost = cost;	
		x = new ArrayList<Double>();
	}
	
	
	private void generateMixtureDistributedBoT (int size, 
			long seed1, double mean1, double stdev1,
			long seed2, double low2, double high2, 
			long seed3, double mean3, double stdev3, 
			long seed4, double low4, double high4,
			double alpha, double beta, double gamma,
			long whichDSeed, 
			double speedFactor) {
		
		long et;

		Random random1 = new Random(seed1);
		Random random2 = new Random(seed2);
		Random random3 = new Random(seed3);
		Random random4 = new Random(seed4);
		Random whichD = new Random(whichDSeed);
			
		int negative = 0;
		int count1 = 0;
		int count2 = 0;
		int count3 = 0;
		int count4 = 0;
		
		for(int i=0 ; i<size ; i++) {
			double p = whichD.nextDouble();
			if(p < alpha) {
				et = (long) (random1.nextGaussian()*stdev1+mean1);	//in seconds, for accuracy
				while(et<=0) {
					negative ++;
					et = (long) (random1.nextGaussian()*stdev1+mean1);	//in seconds, for accuracy
				}
				count1 ++;
			} else if (p < (alpha + beta)) {
				et = (long) (random2 .nextDouble()*(high2-low2)+low2);	//in seconds, for accuracy
				while(et<=0) {
					negative ++;
					et = (long) (random2.nextDouble()*(high2-low2)+low2);	//in seconds, for accuracy
				}
				count2 ++;
			} else if (p < (alpha + beta + gamma)) {
				et = (long) (random3.nextGaussian()*stdev3+mean3);	//in seconds, for accuracy
				while(et<=0) {
					negative ++;
					et = (long) (random3.nextGaussian()*stdev3+mean3);	//in seconds, for accuracy
				}
				count3 ++;
			} else {
				et = (long) (random4.nextDouble()*(high4-low4)+low4);	//in seconds, for accuracy
				while(et<=0) {
					negative ++;
					et = (long) (random4.nextDouble()*(high4-low4)+low4);	//in seconds, for accuracy
				}
				count4 ++;
			}
			
			x.add(new Double(et));		
			
		}	
		//System.out.println("count1=" + count1 + "; count2=" + count2);
	}
	
	public MixtureDistributedBag copy() {
		MixtureDistributedBag tmp = new MixtureDistributedBag(this.size, 
				this.mean1, this.stdev1, this.low2, this.high2, 
				this.mean3, this.stdev3, this.low4, this.high4,				
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
		long experimentBagGeneratingSeed3 = randomizedSeed.nextLong();
		long experimentBagGeneratingSeed4 = randomizedSeed.nextLong();
		Random alphaRandomizedSeed = new Random(88888888L);
		long alphaSeed = alphaRandomizedSeed.nextLong();
		double mean1 = 48; double stdev1 = 20;
		double low2 = 108; double high2 = 599;
		double mean3 = 689; double stdev3 = 30;
		double low4 = 779; double high4 = 2536;		
		MixtureDistributedBag bmnd = new MixtureDistributedBag(1000,
				experimentBagGeneratingSeed1, mean1, stdev1,
				experimentBagGeneratingSeed2, low2, high2,
				experimentBagGeneratingSeed3, mean3, stdev3,
				experimentBagGeneratingSeed4, low4, high4,
				0.205161119, 0.389336084, 0.344549703,
				/*0.257061824, 0.286694108, 0.395576244,*/
				/*0.217061824, 0.376694108, 0.345576244,*/
				/*0.216216735, 0.37844383, 0.344641207,*/  
				alphaSeed,4,3,3);
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
