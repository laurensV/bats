package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.Random;

public class MixtureDistributedTruncatedBag extends Bag {

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
	private long seed5;
	private double high5;
	private double low5;
	private double delta;
	
	public MixtureDistributedTruncatedBag(int size, 
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
		generateMixtureDistributedTruncatedBoT(size, seed1, mean1, stdev1, 
											seed2, low2, high2, 
											seed3, mean3, stdev3,
											seed4, low4, high4,
											alpha, beta, gamma,
												  whichDSeed, 
												  speedFactor);		
	}
	
	public MixtureDistributedTruncatedBag(int size,  
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
	
	
	public MixtureDistributedTruncatedBag(int size,
			long seed1, double mean1, double stdev1,
			long seed2, double low2, double high2,
			long seed3, double mean3, double stdev3,
			long seed4, double low4, double high4,
			long seed5, double low5, double high5,
			double alpha, double beta, double gamma, double delta, long whichDSeed,
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
		
		/*
		double lowZ1 = -1; 
		double highZ1 = 2; 
		
		double lowZ3 = -3; 
		double highZ3 = 3;
		
		*/
			
		double scaleCauchy1 = 15;
		double scaleCauchy3 = 16;
		
		double lowC1 = (19-mean1)/scaleCauchy1;
		double highC1 = (low2-mean1)/scaleCauchy1;
		
		double lowC3 = (high2-mean3)/scaleCauchy3;
		double highC3 = (low4-mean3)/scaleCauchy3;
		
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
	
	public MixtureDistributedTruncatedBag copy() {
		MixtureDistributedTruncatedBag tmp = new MixtureDistributedTruncatedBag(this.size, 
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
		
		MixtureDistributedTruncatedBag bmnd = new MixtureDistributedTruncatedBag(1000,
				experimentBagGeneratingSeed1, mean1, stdev1,
				experimentBagGeneratingSeed2, low2, high2,
				experimentBagGeneratingSeed3, mean3, stdev3,
				experimentBagGeneratingSeed4, low4, high4,
				experimentBagGeneratingSeed5, low5, high5,
				0.274111675, 0.222335025, 0.452791878, 0.026395939,  
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
		if(useHisto) return super.getMostPopularRT(theta);
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void updateDistributionParameterEstimates(double rt) {
		
		
	}
	
}
