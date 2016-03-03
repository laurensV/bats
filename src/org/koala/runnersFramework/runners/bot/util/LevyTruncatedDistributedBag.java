package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class LevyTruncatedDistributedBag extends Bag {

	double t0;
	double xmax;
	
	double scale;
	double location=0;
    double erfcXmax;
	private double scaleEstimate; 
	double erfcXmaxExecution;
	private double scaleEstimateExecution; 
	
	public LevyTruncatedDistributedBag(int size, long seed, double t0, double xmax, 
			double speedFactor, int costFactor, int cost) {
		this.t0 = t0;
		this.xmax= xmax;
		this.seed= seed;
		this.size= size;
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.cost = cost;
		x = new ArrayList<Double>();		
		generateLevyTruncatedBoT(size, seed, t0, xmax, speedFactor);		
	}


	public LevyTruncatedDistributedBag(int size, long seed, double t0, double xmax, 
			double scaleX, double translateX,
			double speedFactor, int costFactor, int cost) {
		this.t0 = t0;
		this.xmax= xmax;
		this.seed= seed;
		this.size= size;
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.cost = cost;
		x = new ArrayList<Double>();
		generateLevyTruncatedBoT(size, seed, t0, xmax, scaleX, translateX, speedFactor);		
	}

	public LevyTruncatedDistributedBag(int size, double t0, double xmax, 
			double speedFactor, int costFactor, int cost) {
		this.t0 = t0;	
		this.size = size;
		this.xmax = xmax;				
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.cost = cost;	
		x = new ArrayList<Double>();
	}
	
	private void generateLevyTruncatedBoT (long size, long seed, double t0, double xmax, double speedFactor) {
		Random u = new Random(seed);

		long et;


		//double alpha = 0.5;
		//double beta = 1;
		double nu = 2*t0*t0*xmax;
				
		//double delta = 0;

		//double traceMean = 177.17;


		double v;

		double X;

		int negativeETCount = 0;
		for(int i=0 ; i<size ; i++) {
			v = u.nextGaussian();
			X = nu/(v*v);
			et = (long) X;
			while(et>xmax) {
				negativeETCount ++;
				v = u.nextGaussian();
				X = nu/(v*v);
				et = (long) X;
			}

			x.add(new Double(et));
		
		}
		
		scale = nu;
		
		erfcXmax = erfc(Math.sqrt(scale/(2*xmax)));
		
		//System.out.println("Negative ETs: " + negativeETCount);
	}


	private void generateLevyTruncatedBoT (long size, long seed, double t0, double xmax, 
			double scaleX, double translateX,
			double speedFactor) {
		Random u = new Random(seed);

		long et;


		//double alpha = 0.5;
		//double beta = 1;
		double nu = 2*t0*t0*xmax;
		//double delta = 0;

		//double traceMean = 177.17;


		double v;

		double X;

		int negativeETCount = 0;
		for(int i=0 ; i<size ; i++) {
			v = u.nextGaussian();
			X = nu/(v*v);
			et = (long) X;
			while(et>xmax) {
				negativeETCount ++;
				v = u.nextGaussian();
				X = nu/(v*v);
				et = (long) X;
			}

			et = (long) (scaleX*X+translateX);
			
			x.add(new Double(et));				

		}
	
		scale = scaleX*nu;
		location = translateX;
		
		erfcXmax = erfc(Math.sqrt(scale/(2*xmax)));
		
		//System.out.println("Negative ETs: " + negativeETCount);
	}

	public LevyTruncatedDistributedBag copy() {
		LevyTruncatedDistributedBag tmp = new LevyTruncatedDistributedBag(this.size, 
				this.t0, this.xmax,
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
	
	public void sample(long sampleSeed, int sampleSize) {
		super.sample(sampleSeed, sampleSize);
		
		this.scaleEstimate=(this.estimateXmax*meanX-3*meanX*meanX-3*varXsq)/(meanX-estimateXmax);
		this.scaleEstimateExecution = this.scaleEstimate;
		erfcXmaxExecution = erfc(estimateXmax);
		
		if(printXLS || printSampleSummary)
			System.out.print("\t" + estimateXmax + "\t" + scaleEstimate + "\t");
	}
	
	public void resetDistributionParameterEstimates() {
		super.resetDistributionParameterEstimates();
		this.scaleEstimateExecution = this.scaleEstimate;
		erfcXmaxExecution = erfc(estimateXmax);
	}
	
	public static void main(String args[]) {
			
		Random randomizeSeed = new Random(99999999L);
		Random sampleSeeder = new Random(111111111L);
		long sampleSeed = sampleSeeder.nextLong();
		int sampleSize = 30; 
		
		boolean printXLS = true;
		boolean printNodeStats = false;
		if(printXLS)
			/*System.out.println("ti1\tstdev1\trealAvg\tBminPlus10\tATUs\tCost\tfs0\tfs1\tti_ag\tstdev_ag\tq0.5\tM[0.5]\tqsr\tM[sr]\t"+
				"BMakespanMinMinus10\tATUs\tCost\tfs0\tfs1\tti_ag\tstdev_ag\tq0.5\tM[0.5]\tqsr\tM[sr]");
			*/
			System.out.println("realAvg\trealStDev\tti1\tstdev1"+
			"\tBMakespanMinMinus10" + 
			"\tATUs\tCost\tfs0\tfs1\tti_ag\tstdev_ag" +
			"\tp_mu_1sided\tqp_mu\tqp_sigma\tqp_m" +
			"\tM[a,ti1,b,ti2]" +
			"\tM[p_m,p_mu,p_sigma]" +
			"\tM[ti_ag,stdev_ag]");
		
		for (int i=0; i < 100; i++) {	

			LevyTruncatedDistributedBag ltb = new LevyTruncatedDistributedBag(1000, 
					randomizeSeed.nextLong(),
					0.366, 2700,
					4, 3, 3);
			ltb.printXLS = printXLS;
			ltb.printNodeStats = printNodeStats;
			
			ltb.getExpectation();
			ltb.getVariance();
			if(ltb.printXLS) 
				System.out.print(ltb.realExpectation/60 + "\t" + Math.sqrt(ltb.realVariance)/60 + "\t");
			
			ltb.sample(sampleSeed, sampleSize);
			
			long Bmin, BminPlus10, BminPlus20, 
				BmakespanMin, BmakespanMinMinus10, BmakespanMinMinus20;
			Bmin=ltb.getMinimumBudget();		
			BminPlus10 = (long)(Bmin*1.1);			
			BmakespanMin=ltb.getMinimumMakespanBudget();
			BmakespanMinMinus10= (long)(BmakespanMin*0.9);
			//System.out.println("Bmin=" + Bmin +";BmakespanMin=" +BmakespanMin);
			//ArrayList<Integer> config = ltb.computeMakespanWithSuccessRate(BminPlus10, "", 0.9);
			
			ArrayList<Integer> config = ltb.computeMakespanWithCI(BmakespanMinMinus10, 
					0.9, 0.99, 0.9);
			if(printNodeStats)
				System.out.println("\nExecution");
			/* execution empties the bag, calling computeMakespan afterwards fails
			 * ltb.execute(randomizeSeed.nextLong(), 2, config.get(0).intValue(), config.get(1).intValue());
			 */
					
			if(printXLS)
				System.out.println();
		}
	}


	@Override
	public double estimateExecutionTime(double theta) {
		if(useHisto) return super.estimateExecutionTime(theta);
		
		double estimate = 0;
    	
    	/*bebe's formula*/
    	//System.out.println("Predict SD-LT: Elapsed time: " + theta);
    	/*should use a different formula for theta<meanX*/
    	   	
    	/*	   	
    	if(!emergencyOnly) System.out.println("erfc(b)=" + erfcXmax);
    	if(!emergencyOnly)  {
    		System.out.print("erfc(" + theta +  ")=");
    		System.out.println(erfc(Math.sqrt(scale/(2*theta))));
    	}*/
    	
    	
    	/*
    	realEstimate = (Math.sqrt(2*xmax*scale)*Math.exp(-scale/(2*xmax))-Math.sqrt(2*theta*scale)*Math.exp(-scale/(2*theta)))/
    				(Math.sqrt(Math.PI)*(erfcXmax-erfc(Math.sqrt(scale/(2*theta))))) 
    				- scale;
    	*/
		this.scaleEstimateExecution=(this.estimateXmaxExecution*meanXExecution-3*meanXExecution*meanXExecution-3*varXsqExecution)
		/(meanXExecution-estimateXmaxExecution);
		
		estimate = (Math.sqrt(2*estimateXmaxExecution*scaleEstimateExecution)*Math.exp(-scaleEstimateExecution/(2*estimateXmaxExecution))
				   -Math.sqrt(2*theta*scaleEstimateExecution)*Math.exp(-scaleEstimateExecution/(2*theta)))/
				(Math.sqrt(Math.PI)*(erfcXmaxExecution-erfc(Math.sqrt(scaleEstimateExecution/(2*theta))))) 
				- scaleEstimateExecution;

    	return estimate;
	}


	@Override
	public double getMostPopularRT(double elapsedET) {
		if(useHisto) return super.getMostPopularRT(elapsedET);
		
		this.scaleEstimateExecution=(this.estimateXmaxExecution*meanXExecution-3*meanXExecution*meanXExecution-3*varXsqExecution)
		/(meanXExecution-estimateXmaxExecution);
		double mode = meanX + scaleEstimateExecution/3;
		return mode;
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
		
		if(rt > estimateXmaxExecution) {
			estimateXmaxExecution = rt;
		}
		erfcXmaxExecution = erfc(estimateXmaxExecution);
	}


	public Bag createSimilarBag(long similarBagGeneratingSeed) {
		LevyTruncatedDistributedBag tmp = new LevyTruncatedDistributedBag(this.size, similarBagGeneratingSeed,
				this.t0, this.xmax,
				this.speedFactor, this.costFactor, this.cost);
		return tmp;
	}
}
