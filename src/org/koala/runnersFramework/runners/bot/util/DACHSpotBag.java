package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class DACHSpotBag extends SpotBag{
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
	long seed5;
	double high5;
	double low5;
	double delta;
	double lowC3;
	double highC3;
	double scaleCauchy3;
	double lowC1;
	double highC1;	
	double scaleCauchy1;
	
	
	public DACHSpotBag(int size, 
			long seed1, double mean1, double stdev1, 
			long seed2, double low2, double high2, 
			long seed3, double mean3, double stdev3, 
			long seed4, double low4, double high4,
			double alpha, double beta, double gamma, long whichDSeed,
			int speedFactor, int costFactor, int cost, PriceFluctuationsSimulator priceSim, int maxItems) {
		super(size, seed1, 0, mean1, speedFactor, costFactor, cost, priceSim, maxItems);
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
		generateMixtureDistributedTruncatedBoT(size, seed1, mean1, stdev1, 
											seed2, low2, high2, 
											seed3, mean3, stdev3,
											seed4, low4, high4,
											alpha, beta, gamma,
												  whichDSeed, 
												  speedFactor);		
	}
	
	public DACHSpotBag(int size,  
			double mean1, double stdev1, double low2, double high2, 
			double mean3, double stdev3, double low4, double high4,
			int speedFactor, int costFactor, int cost, PriceFluctuationsSimulator priceSim, int maxItems) {
		super(size, (long)0, 0, mean1, speedFactor, costFactor, cost, priceSim, maxItems);
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
	
	
	public DACHSpotBag(int size,
			long seed1, double mean1, double stdev1,
			long seed2, double low2, double high2,
			long seed3, double mean3, double stdev3,
			long seed4, double low4, double high4,
			long seed5, double low5, double high5,
			double alpha, double beta, double gamma, double delta, long whichDSeed,
			int speedFactor, int costFactor, int cost, PriceFluctuationsSimulator priceSim, int maxItems) {
		super(size, seed1, 0, mean1, speedFactor, costFactor, cost, priceSim, maxItems);
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
		this.low5 = low5;
		this.high5 = high5;
		this.seed5 = seed5;
		this.alpha = alpha;		
		this.beta = beta;	
		this.gamma = gamma;	
		this.delta = delta;
		this.size= size;
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.cost = cost;
		x = new ArrayList<Double>();
		generateMixtureDistributedTruncatedBoT(size, 
											seed1, mean1, stdev1, 
											seed2, low2, high2, 
											seed3, mean3, stdev3,
											seed4, low4, high4,
											seed5, low5, high5,
											alpha, beta, gamma, delta,
												  whichDSeed, 
												  speedFactor);		
		
	}

	private void generateMixtureDistributedTruncatedBoT(int size, 
			long seed1,	double mean1, double stdev1, 
			long seed2, double low2, double high2, 
			long seed3, double mean3, double stdev3,
			long seed4, double low4, double high4, 
			long seed5, double low5, double high5, 
			double alpha, double beta, double gamma, double delta, 
			long whichDSeed, double speedFactor) {

		long et;

		Random random11 = new Random(seed1);
		Random random12 = new Random(seed1 + 1);
		Random random2 = new Random(seed2);
		Random random31 = new Random(seed3);
		Random random32 = new Random(seed3 + 1);
		Random random4 = new Random(seed4);
		Random random5 = new Random(seed5);
		Random whichD = new Random(whichDSeed);
			
		int negative = 0;
		int count1 = 0;
		int count2 = 0;
		int count3 = 0;
		int count4 = 0;
		int count5 = 0;
		
		/*
		double Fi1a = umontreal.iro.lecuyer.probdist.NormalDist.cdf(0, 1, -2);
		double Fi1b = umontreal.iro.lecuyer.probdist.NormalDist.cdf(0, 1, 2);
		
		double Fi3a = umontreal.iro.lecuyer.probdist.NormalDist.cdf(0, 1, -2);
		double Fi3b = umontreal.iro.lecuyer.probdist.NormalDist.cdf(0, 1, 2);
		
		double fi1a = umontreal.iro.lecuyer.probdist.NormalDist.density(0, 1, -2);
		double fi1b = umontreal.iro.lecuyer.probdist.NormalDist.density(0, 1, 2);
		
		double fi3a = umontreal.iro.lecuyer.probdist.NormalDist.density(0, 1, -2);
		double fi3b = umontreal.iro.lecuyer.probdist.NormalDist.density(0, 1, 2);
		
		double truncated1 = (fi1a-fi1b)/(Fi1b-Fi1a);
		double truncated3 = (fi3a-fi3b)/(Fi3b-Fi3a);
		
		double truncatedMean1 = mean1 + truncated1*stdev1;
		double truncatedStdev1 = Math.sqrt(stdev1*stdev1*(1-truncated1*truncated1-2*(fi1a+fi1b)/(Fi1b-Fi1a)));
		
		double truncatedMean3 = mean3 + truncated3*stdev3;
		double truncatedStdev3 = Math.sqrt(stdev3*stdev3*(1-truncated3*truncated3-2*(fi3a+fi3b)/(Fi3b-Fi3a)));
		
		System.out.println("truncated1:" + truncated1 + "; truncated3:" + truncated3);
		*/
		
		/* for D1 and D3 normal distributions
		double lowZ1 = -1; 
		double highZ1 = 2; 
		
		double lowZ3 = -3; 
		double highZ3 = 3;
		
		*/
		
		/*for D1 and D3 cauchy distributions*/
		scaleCauchy1 = 15;
		scaleCauchy3 = 16;
		
		lowC1 = (19-mean1)/scaleCauchy1;
		highC1 = (low2-mean1)/scaleCauchy1;
		
		lowC3 = (high2-mean3)/scaleCauchy3;	
		highC3 = (low4-mean3)/scaleCauchy3;
		
		double r, nd;		
		
		for(int i=0 ; i<size ; i++) {
			double p = whichD.nextDouble();
			if(p < alpha) {
				r = random11.nextGaussian();
				nd = random12.nextGaussian();
				while(((r/nd)<lowC1) || ((r/nd)>highC1)) {
					r = random11.nextGaussian();
					nd = random12.nextGaussian();
				}
				//et = (long) (r*stdev1+mean1);	//in seconds, for accuracy
				et = (long) ((r/nd)*scaleCauchy1+mean1);	//in seconds, for accuracy
				count1 ++;
			} else if (p < (alpha + beta)) {
				et = (long) (random2 .nextDouble()*(high2-low2)+low2);	//in seconds, for accuracy
				while(et<=0) {
					negative ++;
					et = (long) (random2.nextDouble()*(high2-low2)+low2);	//in seconds, for accuracy
				}
				count2 ++;
			} else if (p < (alpha + beta + gamma)) {
				/*
				r = random3.nextGaussian();
				while((r<lowZ3) || (r>highZ3)) {
					r = random3.nextGaussian();					
				}
				et = (long) (r*stdev3+mean3);	//in seconds, for accuracy
				*/
				r = random31.nextGaussian();
				nd = random32.nextGaussian();
				while(((r/nd)<lowC3) || ((r/nd)>highC3)) {
					r = random31.nextGaussian();
					nd = random32.nextGaussian();
				}
				et = (long) ((r/nd)*scaleCauchy3+mean3);	//in seconds, for accuracy
				count3 ++;
			} else if (p < (alpha + beta + gamma + delta)) {
				et = (long) (random4.nextDouble()*(high4-low4)+low4);	//in seconds, for accuracy
				while(et<=0) {
					negative ++;
					et = (long) (random4.nextDouble()*(high4-low4)+low4);	//in seconds, for accuracy
				}
				count4 ++;
			} else {
				et = (long) (random5.nextDouble()*(high5-low5)+low5);	//in seconds, for accuracy
				while(et<=0) {
					negative ++;
					et = (long) (random5.nextDouble()*(high5-low5)+low5);	//in seconds, for accuracy
				}
				count5 ++;
			}
			
			x.add(new Double(et));		
			
		}	
		System.out.println("count1=" + count1 + 
						 "; count2=" + count2 +
						 "; count3=" + count3 +
						 "; count4=" + count4 +
						 "; count5=" + count5 );

		
		
	}

	@Override
	public DACHSpotBag copyMC() {
		DACHSpotBag tmp = copy();
		tmp.clusters = (HashMap<String, SimulatedCluster>) clusters.clone();
		return tmp;
	}
	
	private void generateMixtureDistributedTruncatedBoT (int size, 
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
		
		/*
		double Fi1a = umontreal.iro.lecuyer.probdist.NormalDist.cdf(0, 1, -2);
		double Fi1b = umontreal.iro.lecuyer.probdist.NormalDist.cdf(0, 1, 2);
		
		double Fi3a = umontreal.iro.lecuyer.probdist.NormalDist.cdf(0, 1, -2);
		double Fi3b = umontreal.iro.lecuyer.probdist.NormalDist.cdf(0, 1, 2);
		
		double fi1a = umontreal.iro.lecuyer.probdist.NormalDist.density(0, 1, -2);
		double fi1b = umontreal.iro.lecuyer.probdist.NormalDist.density(0, 1, 2);
		
		double fi3a = umontreal.iro.lecuyer.probdist.NormalDist.density(0, 1, -2);
		double fi3b = umontreal.iro.lecuyer.probdist.NormalDist.density(0, 1, 2);
		
		double truncated1 = (fi1a-fi1b)/(Fi1b-Fi1a);
		double truncated3 = (fi3a-fi3b)/(Fi3b-Fi3a);
		
		double truncatedMean1 = mean1 + truncated1*stdev1;
		double truncatedStdev1 = Math.sqrt(stdev1*stdev1*(1-truncated1*truncated1-2*(fi1a+fi1b)/(Fi1b-Fi1a)));
		
		double truncatedMean3 = mean3 + truncated3*stdev3;
		double truncatedStdev3 = Math.sqrt(stdev3*stdev3*(1-truncated3*truncated3-2*(fi3a+fi3b)/(Fi3b-Fi3a)));
		
		System.out.println("truncated1:" + truncated1 + "; truncated3:" + truncated3);
		*/
		
		double lowZ1 = -1; 
		double highZ1 = 1; 
		
		double lowZ3 = -2; 
		double highZ3 = 2;
		
		double r;		
		
		for(int i=0 ; i<size ; i++) {
			double p = whichD.nextDouble();
			if(p < alpha) {
				r = random1.nextGaussian();
				while((r<lowZ1) || (r>highZ1)) {
					r = random1.nextGaussian();					
				}
				et = (long) (r*stdev1+mean1);	//in seconds, for accuracy
				count1 ++;
			} else if (p < (alpha + beta)) {
				et = (long) (random2 .nextDouble()*(high2-low2)+low2);	//in seconds, for accuracy
				while(et<=0) {
					negative ++;
					et = (long) (random2.nextDouble()*(high2-low2)+low2);	//in seconds, for accuracy
				}
				count2 ++;
			} else if (p < (alpha + beta + gamma)) {
				r = random3.nextGaussian();
				while((r<lowZ3) || (r>highZ3)) {
					r = random3.nextGaussian();					
				}
				et = (long) (r*stdev3+mean3);	//in seconds, for accuracy
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
		System.out.println("count1=" + count1 + 
						 "; count2=" + count2 +
						 "; count3=" + count3 +
						 "; count4=" + count4 );
	}
	
	public DACHSpotBag copy() {
		DACHSpotBag tmp = new DACHSpotBag(this.size, 
				this.mean1, this.stdev1, this.low2, this.high2, 
				this.mean3, this.stdev3, this.low4, this.high4,
				(int)this.speedFactor, this.costFactor, this.cost, this.priceSimulator, this.maxItems);
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
		long experimentBagGeneratingSeed5 = randomizedSeed.nextLong();
		
		Random alphaRandomizedSeed = new Random(88888888L);
		long alphaSeed = alphaRandomizedSeed.nextLong();
		/*0.206244704, 0.387433902, 0.34531007,*/
		/*double mean1 = 48; double stdev1 = 15;
		double low2 = 108; double high2 = 599;
		double mean3 = 689; double stdev3 = 17;
		double low4 = 779; double high4 = 2536;		
		*/
		double mean1 = 48; double stdev1 = 29;
		double low2 = 106; double high2 = 607;
		double mean3 = 689; double stdev3 = 27;
		double low4 = 771; double high4 = 892;
		double low5 = 1649; double high5 = 2553;
		
		DACHSpotBag bmnd = new DACHSpotBag(1000,
				experimentBagGeneratingSeed1, mean1, stdev1,
				experimentBagGeneratingSeed2, low2, high2,
				experimentBagGeneratingSeed3, mean3, stdev3,
				experimentBagGeneratingSeed4, low4, high4,
				experimentBagGeneratingSeed5, low5, high5,
				0.274111675, 0.222335025, 0.452791878, 0.026395939,  
				alphaSeed,4,3,3, null, 0);
		bmnd.print();
	}

	@Override
	public double estimateExecutionTime(double theta) {
		if(useHisto) return super.estimateExecutionTime(theta);
		double estimate = 0;
		if(theta < this.low2) {
			estimate = Math.log(Math.sqrt((1+this.low2*this.low2)/(1+theta*theta)))
					 /(Math.atan(this.low2)-Math.atan(theta));
			return estimate;
		}
		if(theta < this.high2) {
			estimate = (this.high2 + theta)/2;
			return estimate;
		}
		if (theta < this.low4) {
			estimate = Math.log(Math.sqrt((1+this.low4*this.low4)/(1+theta*theta)))
			 /(Math.atan(this.low4)-Math.atan(theta));
			return estimate;
		}
		if(theta < this.high4) {
			estimate = (this.high4 + theta)/2;
			return estimate;
		}
		if(theta < this.high5) {
			estimate = (this.high5 + theta)/2;
			return estimate;
		}
		return estimate;
	}

	@Override
	public double getMostPopularRT(double elapsedET) {
		if(useHisto) return super.getMostPopularRT(elapsedET);
		if(this.alpha < this.gamma) {
			return this.mean1;
		} else {
			return this.mean3;
		}
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
