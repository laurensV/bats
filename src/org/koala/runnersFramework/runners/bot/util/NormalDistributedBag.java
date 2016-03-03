package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class NormalDistributedBag extends Bag {

	double mean;
	double stdev;
	

	public NormalDistributedBag(int size, long seed, double mean, double stdev, 
			double speedFactor, int costFactor, int cost) {
		this.mean = mean;
		this.stdev = stdev;
		this.seed= seed;
		this.size= size;
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.cost = cost;
		x = new ArrayList<Double>();
		generateNormalBoT(size, seed, mean, stdev, speedFactor);		
	}


	public NormalDistributedBag(int size, double mean, double stdev, 
			double speedFactor, int costFactor, int cost) {
		this.size = size;
		this.mean = mean;
		this.stdev = stdev;			
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.cost = cost;	
		x = new ArrayList<Double>();
	}


	private void generateNormalBoT (int size, long seed, double mean, double stdev, double speedFactor) {
		
		long et;

		Random random = new Random(seed);
			
		int negative = 0;
		
		for(int i=0 ; i<size ; i++) {
			et = (long) (random.nextGaussian()*stdev+mean);	//in seconds, for accuracy
			while(et<0) {
				negative ++;
				et = (long) (random.nextGaussian()*stdev+mean);	//in seconds, for accuracy
			}
			
			x.add(new Double(et));		
			
		}			
		
		//System.out.println("negative count: " + negative);
	}

	public NormalDistributedBag copy() {
		NormalDistributedBag tmp = new NormalDistributedBag(this.size, this.mean, this.stdev,
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
		/*	
		Random randomizeSeed = new Random(99999999L);
		Random sampleSeeder = new Random(111111111L);
		long sampleSeed = sampleSeeder.nextLong();
		int sampleSize = 30; 
		System.out.println("ti1\tstdev1\tBminPlus10\tATUs\tCost\tfs0\tfs1\tti_ag\tstdev_ag\tq0.5\tM[0.5]\tqsr\tM[sr]\t"+
				"BMakespanMinMinus10\tATUs\tCost\tfs0\tfs1\tti_ag\tstdev_ag\tq0.5\tM[0.5]\tqsr\tM[sr]");
		
		for (int i=0; i < 100; i++) {	

			NormalDistributedBag ndb = new NormalDistributedBag(1000, 
					randomizeSeed.nextLong(),
					15*60, Math.sqrt(5)*60,
					4, 3, 3);
			ndb.sample(sampleSeed, sampleSize);
			long Bmin, BminPlus10, BminPlus20, 
				BmakespanMin, BmakespanMinMinus10, BmakespanMinMinus20;
			Bmin=ndb.getMinimumBudget();		
			BminPlus10 = (long)(Bmin*1.1);			
			BmakespanMin=ndb.getMinimumMakespanBudget();
			BmakespanMinMinus20= (long)(BmakespanMin*0.8);
			//System.out.println("Bmin=" + Bmin +";BmakespanMin=" +BmakespanMin);
			ndb.computeMakespanWithSuccessRate(BminPlus10, "", 0.9);			
			ndb.computeMakespanWithSuccessRate(BmakespanMinMinus20, "", 0.9);
			System.out.println();
		}
		*/
		Random randomizeSeed = new Random(99999999L);
		Random sampleSeeder = new Random(111111111L);
		long sampleSeed = sampleSeeder.nextLong();
		int sampleSize = 30; 
		
		boolean printXLS = true;
		boolean printNodeStats = false;
		if(printXLS)
			System.out.println("realAvg\trealStDev\tti1\tstdev1"+
					"\tBMakespanMinMinus10" + 
					"\tATUs\tCost\tfs0\tfs1\tti_ag_est\tstdev_ag_est\tti_ag_real\tstdev_ag_real" +
					"\tp_mu_1sided\tqp_mu\tqp_sigma\tqp_m" +
					"\tM[a,ti1,b,ti2]" +
					"\tM[p_m,p_mu,p_sigma]" +
					"\tM[ti_ag,stdev_ag]");
				
		for (int i=0; i < 100; i++) {	
				NormalDistributedBag ndb = new NormalDistributedBag(1000, 
				randomizeSeed.nextLong(),
				15*60, 5*60,
				4, 3, 3);
		
				ndb.printXLS = printXLS;
				ndb.printNodeStats = printNodeStats;
				
				ndb.getExpectation();
				ndb.getVariance();
				if(ndb.printXLS) 
					System.out.print(ndb.realExpectation/60 + "\t" + 
							         Math.sqrt(ndb.realVariance)/60 + "\t");
				
				ndb.sample(sampleSeed, sampleSize);
				
				long Bmin, BminPlus10, BminPlus20, 
					BmakespanMin, BmakespanMinMinus10, BmakespanMinMinus20;
				Bmin=ndb.getMinimumBudget();		
				BminPlus10 = (long)(Bmin*1.1);			
				BmakespanMin=ndb.getMinimumMakespanBudget();
				BmakespanMinMinus10= (long)(BmakespanMin*0.9);
				//System.out.println("Bmin=" + Bmin +";BmakespanMin=" +BmakespanMin);
				//ArrayList<Integer> config = ltb.computeMakespanWithSuccessRate(BminPlus10, "", 0.9);
				
				ArrayList<Integer> config = ndb.computeMakespanWithCI(BminPlus10, 
						0.95, 0.99, 0.9);
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
    	/*should always use the theta formula*/
    	/*if(theta < meanX) estimate=meanX;
    	else {*/
    		estimate = meanXExecution + 
    		Math.sqrt(varXsqExecution/(2*Math.PI))*
    		Math.exp(-(theta-meanXExecution)*(theta-meanXExecution)/(2*varXsqExecution))/
    		(1-Phi((theta-meanXExecution)/Math.sqrt(varXsqExecution)));
    	//}
    		//System.out.println("Predict ND: Elapsed time: " + theta + "; predicted: " + estimate);
    	return estimate;
	}


	@Override
	public double getMostPopularRT(double elapsedET) {
		if(useHisto) return super.getMostPopularRT(elapsedET);
		return meanXExecution;
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
