package org.koala.runnersFramework.runners.bot.util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.koala.runnersFramework.runners.bot.ContKnap;
import org.koala.runnersFramework.runners.bot.Item;
import org.koala.runnersFramework.runners.bot.ItemType;
import org.koala.runnersFramework.runners.bot.Knapsack;


public class DifferentDistributionBag {
	
	double mean;
	double variance;
	
	public DifferentDistributionBag(int size, long budget, double mean, double variance) {
	
		generateBoT(size, budget, mean, variance);		
	}

	/**
	 * @param size
	 * @param budget
	 * @param mean
	 * @param variance
	 */
	void generateBoT (long size, long budget, double mean, double variance) {
		
		Random random = new Random(999999999);
		long et;
		long ett1 = 0;
		long ett2 = 0;
		ArrayList<Double> x = new ArrayList<Double>();
		ArrayList<Double> y = new ArrayList<Double>();
		long[] args1 = new long[(int) size];
		long[] args2 = new long[(int) size];
		
		int[] interval1 = new int[32];
		int[] interval2 = new int[32];
		
		int beta = 1;
		
		for(int i=0 ; i<size ; i++) {
			et = (long) (random.nextGaussian()*variance+mean);	//in seconds, for accuracy
			while(et<0) {
				et = (long) (random.nextGaussian()*variance+mean);	//in seconds, for accuracy
			}
			//List<String> args = new ArrayList<String>();
			x.add(new Double(et));
			y.add(new Double(et/beta));
			args1[i] = et;
			args2[i] = et/beta;
			ett2 += args2[i];
			/*if(et > mean)  { 
				args1[i] = 2*et;				
			}	*/		
			ett1 += args1[i];			
			interval1[(int)et/60+1] ++;
			interval2[(int)(et/beta)/60+1] ++;
		}		
		
		double ti1 = ((double)ett1/size)/60;
		double ti2 = ((double)ett2/size)/60;

		System.out.println("mean c1: " + ti1 + "; mean c2: " + ti2);
		
		double sumvarsq = 0;
		double sumvarsqy = 0;
		
		for(Double xi : x) {
			sumvarsq += Math.pow(xi.doubleValue()-((double)ett1/size), 2);
		}
		
		for(Double yi : y) {
			sumvarsqy += Math.pow(yi.doubleValue()-((double)ett2/y.size()), 2);
		}
		
		double varXsq = sumvarsq / x.size();
		double varYsq = sumvarsqy / y.size();
		
		System.out.println("variance c1: " + varXsq + " sec^2, " + Math.sqrt(varXsq)/60 + " min ; variance c2: " 
				+ varYsq + " sec^2, " + Math.sqrt(varYsq)/60 + " min");
		/*
		DistributionPredictor dpSlow = new DistributionPredictor(x);
		dpSlow.estimateX();
		System.out.println("DP mean: " + dpSlow.meanX + "; DP stDev: " + Math.sqrt(dpSlow.varXsq));
		
		DistributionPredictor dpFast = new DistributionPredictor(y);
		dpFast.estimateX();
		System.out.println("DP mean: " + dpFast.meanX + "; DP stDev: " + Math.sqrt(dpFast.varXsq));
		
		*/
		
		int alpha = 3;
		beta = 4;
		
		int c1 = 3;
		int c2 = alpha*c1;
		//int c3 = c1;
		
		ti1=  13.57;
		ti2=  ti1/beta;
		
				
		//double ti3 = ti1;
		
		//size=4; budget=12;
		size=873; budget=540;
		//size=963; budget=(long)Math.ceil(Math.ceil(0.84*1920));
		//size = 1027; budget=1920;/*(long)Math.ceil(1.01*1920);*/
		
		Item[] machines = new Item[65];/*[97];*/
		machines[0]=null;
		
		for(int i=1;i<33;i++) {
			machines[i] = new Item(1/ti1,c1,"1");						
		} 
		for(int i=33;i<65;i++) {
			machines[i] = new Item(1/ti2,c2,"2");			
		}/*
		for(int i=65;i<97;i++) {
			machines[i] = new Item(1/ti3,c3,"3");			
		}*/
		
		
		
		long start = System.nanoTime();
		
		Knapsack offlineMoo = new Knapsack(machines,budget,(long)size,3,32*c1+32*c2/*+32*c3*/,60);
		HashMap<String, Integer> offlineSol = offlineMoo.findSol();
		System.out.println("Estimated Makespan: " + offlineMoo.noATUPlan + "; Estimated Budget: " + offlineMoo.costPlan);
		
		System.out.println("Time(nano): " + (System.nanoTime()-start));
		System.out.println("---------------------------------------------");
	
		long aini = 0;
		if(offlineSol.get("1")!=null)
			aini += offlineSol.get("1").intValue()* Math.floor(offlineMoo.noATUPlan*60/ti1);
		if(offlineSol.get("2")!=null)
			aini += offlineSol.get("2").intValue()* Math.floor(offlineMoo.noATUPlan*60/ti2);
		long deltaN = size - aini;
		System.out.println("Delta N = " + deltaN );
		/*
		ItemType[] clusters = new ItemType[3];
		clusters[0] = new ItemType(3,1/ti1,32,"1");
		clusters[1] = new ItemType(3,1/ti2,32,"2");
		clusters[2] = new ItemType(3,1/ti3,32,"3");
		
		start = System.nanoTime();
		
		ContKnap greedyMoo = new ContKnap(clusters,budget,60,size);
		greedyMoo.findSol();
	
		System.out.println("Time(nano): " + (System.nanoTime()-start));
		System.out.println("---------------------------------------------");
		*/
		
		System.out.println("Mean " + mean + " variance " + variance);
		
		System.out.println("ett: " + ett1 + "vi: " + 1/ti1 + " ;slow: " + (double)(ett1/32) + " ; budget: " + 32*Math.ceil(ett1/(32*15*60))*3);
		System.out.println("ett: " + ett2 + "vi: " + 1/ti2 + ";fast: " + (double)(ett2/32) + "; budget: " + 32*Math.ceil(ett2/(32*15*60))*5);
		
		/*
		System.out.println("Min\tSlow\tFast");
		for(int i=1;i<32;i++) {
			System.out.println(i+"\t"+interval1[i]+"\t"+interval2[i]);
		}
		*/
		long Bmin, BminPlus10, BminPlus20, BmakespanMinMinus10, BmakespanMinMinus20;

		alpha = 3;
		beta = 4;
		c1 = 3;
		
		System.out.println("==================ND==========================");
		
		size = 888;
		Bmin = 513;
		BminPlus10 = (long)(Bmin*1.1);
		budget = Bmin;
		ti1 = 15.16;
		double stDev1 = 2.42;
		
		computeMakespanWithSuccessRate(alpha,beta,c1,ti1,stDev1,size,budget,"estimated(0.5)",0.90);
		
		double quantileTi = StatUtil.getInvCDF(0.52, true);		
		double ti1WithError = ti1 + stDev1*quantileTi;
		System.out.println("ti1 with probability["+0.52+"->"+quantileTi+"]:" + ti1WithError);
		computeMakespanWithSuccessRate(alpha,beta,c1,ti1WithError,stDev1,size,budget,"estimated(>0.5)",0.99);
				
		computeMakespanWithSuccessRate(alpha,beta,c1,15,2.23,size,budget,"theoretical",0.99);
		
		computeMakespanWithSuccessRate(alpha,beta,c1,15.08,2.19,size,budget,"based on real values",0.99);
		
		System.out.println("==================LT==========================");
		
		size = 873;
		Bmin=450;
		BminPlus10 = (long)(Bmin*1.1); //this yields 450->495, however (long)Math.ceil(Bmin*1.1) yields 496 ?!?!?!?
		budget = BminPlus10;
		ti1 = 13.57;
		stDev1 = 9.58;
		
		computeMakespanWithSuccessRate(alpha,beta,c1,ti1,stDev1,size,budget,"estimated(0.5)",0.99999);
		
		quantileTi = StatUtil.getInvCDF(0.7667, true);		
		ti1WithError = ti1 + stDev1*quantileTi;
		
		System.out.println("ti1 with probability["+0.7667+"->"+quantileTi+"]:" + ti1WithError);
		computeMakespanWithSuccessRate(alpha,beta,c1,ti1WithError,2.42,size,budget,"estimated(>0.5)",0.90);
		
		double makespanBminTiWithError = Math.ceil((size*(ti1WithError/beta))/60);
		long BminTiWithError = (long)makespanBminTiWithError*c2;
		System.out.println("MBminTiError= " + makespanBminTiWithError + "; BminTiWithError= " + BminTiWithError);
		computeMakespanWithSuccessRate(alpha,beta,c1,ti1WithError,2.42,size,BminTiWithError,"estimated(>0.5)",0.90);
		
		int Xmax = 45;
		double t0 = 0.366;
		double common = Math.pow(Math.E, -t0*t0)*2*Xmax*t0/(Math.sqrt(Math.PI)*erfc(t0));
		double ex2 = (Xmax-2*t0*t0*Xmax)*common/3 + Math.pow(2*t0*t0*Xmax, 2)/3;
		double ex = common-2*t0*t0*Xmax;
		double var1_theoretic = ex2 - ex*ex; 	
		double stDev1_theoretic = 0;
		if(var1_theoretic > 0) stDev1_theoretic = Math.sqrt(var1_theoretic);
		System.out.println("ti1=ex:" + ex);
		computeMakespanWithSuccessRate(alpha,beta,c1,ex,stDev1_theoretic,size,budget,"theoretical",0.99);
		
		computeMakespanWithSuccessRate(alpha,beta,c1,14.43,11.18,size,budget,"based on real values",0.95);
		
		//generateStableDistributionLevyTruncatedBoT(1000, 0.366, 45*60);
		//System.out.println("ex2=" + ex2 + "; ex=" + ex + "; sigma^2/3= " + Math.pow(2*t0*t0*Xmax, 2)/3);
		/*
		ti1 = 14.43045; 
		ti2 = ti1/beta;
		*/
		/*
		int deltaNLT = 1;
		int percent = 0;
		
		deltaNLT = pg.getDeltaNLT((int)size, offlineLTMoo.noATUPlan, 
				(int)budget, a, b, stDev1);		
		*/
		
		
		/*if(deltaNLT != 1) break;
		percent ++;
		if(percent > 20) break;
		budget = (long)Math.ceil((double)(100 + percent)*Bmin/100.00);
		}*/
		
	}	
	
	private void generateStableDistributionLevyTruncatedBoT (long size, double t0, double xmax) {
		Random u = new Random(999999999);
					
		long et;

		double speedFactor = 4;
		
		double alpha = 0.5;
		double beta = 1;
		double nu = 2*t0*t0*xmax;
		double delta = 0;
		
		double traceMean = 177.17;
		
		
		double v;
				
		double X;
		
		ArrayList<Integer> tasks = new ArrayList<Integer>();
		
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
				tasks.add(i);
				String argsC1 = "" + et;
				String printC1 = et/60 + "m" + et%60 + "s";
				String argsC2 = "" + et/speedFactor;
				String printC2 = (et/speedFactor)/60 + "m" + (et/speedFactor)%60 + "s";
				System.out.println(i + "\t" + argsC1 + "\t" + argsC2);
			}
		//System.out.println("Negative ETs: " + negativeETCount);
	    
			Random randomSample = new Random(1111111111L);
			for(int i = 0; i < 7; i++) {
				System.out.println(tasks.remove(randomSample.nextInt(tasks.size()))+";");
			}
			
	}

	
	
	public void computeMakespanWithSuccessRate(int alpha, double beta,int c1,			
												 double ti1, double stDev1,			
												 long size, long budget,
												 String type,
												 double successProbability) {
		
		Item[] machines = new Item[65];
		machines[0]=null;
		
		 
		int c2 = alpha*c1;
		double ti2 = ti1/beta;
		double stDev2 = stDev1/beta;
		
		for(int i=1;i<33;i++) {
			machines[i] = new Item(1/ti1,c1,"1");						
		} 
		for(int i=33;i<65;i++) {
			machines[i] = new Item(1/ti2,c2,"2");			
		}
	
		Knapsack offlineLTMoo = new Knapsack(machines,budget,(long)size,3,32*c1+32*c2,60);
		HashMap<String, Integer> offlineLTSol = offlineLTMoo.findSol();
		Playground pg = new Playground(1000,3,60/ti1,alpha,beta);
		int a = offlineLTSol.get("1")!=null ? offlineLTSol.get("1").intValue() : 0;
		int b = offlineLTSol.get("2")!=null ? offlineLTSol.get("2").intValue() : 0;
		System.out.println("Estimated Makespan: " + offlineLTMoo.noATUPlan + 
						 "; Estimated Budget: " + offlineLTMoo.costPlan);
		
		
		System.out.println("fs0 machines:" + a + "; fs1 machines:" + b);
				
		double gamma = (a/ti1+b/ti2);
		double ti_ag = 1/gamma;
		double stDev_ag = Math.sqrt((a*ti1+b*ti2)/gamma + (a*stDev1*stDev1/ti1+b*stDev2*stDev2/ti2)/gamma -
		  					(a+b)*(a+b)/gamma/gamma)/(a+b);
			
		
		System.out.println("T["+type+"]:" + ti_ag + 
						 "; stDev["+type+"]:" + stDev_ag);
		
		
		double quantile = StatUtil.getInvCDF(0.5, true);//0;		
		double makespanBminWithError50 = size*ti_ag + Math.sqrt(size)*stDev_ag*quantile;
		
		double makespanWithError = makespanBminWithError50;
		System.out.println("Makespan(min) with probability["+0.5+"->"+quantile+"]:" 
				+ makespanWithError + "; in ATUs=" +
				Math.ceil(makespanWithError/60));
		
		quantile = StatUtil.getInvCDF(successProbability, true);
		double makespanBminWithError99 = size*ti_ag + Math.sqrt(size)*stDev_ag*quantile;
		
		makespanWithError = makespanBminWithError99;
		System.out.println("Makespan(min) with probability["+successProbability+"->"+quantile+"]:" 
				+ makespanWithError + "; in ATUs=" +
				Math.ceil(makespanWithError/60));
		
		double maxSpeed = 0.0;
		long costMaxSpeed = 0;
		
		maxSpeed += (double) (32 / ti1);
		maxSpeed += (double) (32 / ti2);
		costMaxSpeed += 32 * c1;
		costMaxSpeed += 32 * c2 ;
		
		double makespanMin = Math.ceil((size/maxSpeed)/60);
		double BmakespanMin = makespanMin * costMaxSpeed;
		
		System.out.println("Minimum Makespan=" + makespanMin + " ; budget=" + BmakespanMin);
		
		}
	
	
	 private double erfc(double x) {
	    	return 2*Phi(-Math.sqrt(2)*x);
	    }
	 
	// return phi(x) = standard Gaussian pdf
    private double phi(double x) {
        return Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI);
    }
    
    // return Phi(z) = standard Gaussian cdf using Taylor approximation
	private double Phi(double z) {
        if (z < -8.0) return 0.0;
        if (z >  8.0) return 1.0;
        double sum = 0.0, term = z;
        for (int i = 3; sum + term != sum; i += 2) {
            sum  = sum + term;
            term = term * z * z / i;
        }
        return 0.5 + sum * phi(z);
    }
	
    public static void main (String args[]) {
		
		int size = args.length > 0 ? Integer.parseInt(args[0]) : 100;
		/*args related to distribution time interval are expressed in minutes by the user*/
		long budget = args.length > 1 ? Long.parseLong(args[1]) : 350;
		double mean = args.length > 2 ? Double.parseDouble(args[2])*60 : 15*60;
		double variance = args.length > 3 ? Double.parseDouble(args[3])*60 : Math.sqrt(5)*60;
/*
		double zeta = args.length > 1 ? Double.parseDouble(args[1]) : 1.96;
		double delta = args.length > 2 ? Double.parseDouble(args[2]) : 0.25;
		double zeta_sq = zeta * zeta;
		int noSampleJobs = (int) Math.ceil(size * zeta_sq
				/ (zeta_sq + 2 * (size - 1) * delta * delta));
		
		System.out.println("Sample number is " + noSampleJobs + " totalNumberTasks: " + size);
	*/	
		
		DifferentDistributionBag bag = new DifferentDistributionBag(size, budget, mean, variance);
		//AllBM allBM = new AllBM(bag);		
	}
	
}
