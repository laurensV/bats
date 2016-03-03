package org.koala.runnersFramework.runners.bot.util;

public class SampleStats {

	Bag bag;
	double realE;
	double realVariance;
	double realStDev;
	int sampleSize;
	
	long RealBmin, RealBminPlus10 ,  RealBminPlus20 , 
	RealBmakespanMin , RealBmakespanMinMinus10 , RealBmakespanMinMinus20 ;
	
	int RealMBmin , RealMBminPlus10 ,  RealMBminPlus20 , 
	RealMBmakespanMin , RealMBmakespanMinMinus10 , RealMBmakespanMinMinus20 ;
	
	Double perBagE =0.0, perBagStDev =0.0, 
	perBagBmin =0.0, 
	perBagBminPlus10 =0.0,  
	 perBagBminPlus20 =0.0, 
	 perBagBmakespanMin =0.0, 
	 perBagBmakespanMinMinus10 =0.0, 
	 perBagBmakespanMinMinus20 =0.0,
	 perBagMBmin =0.0, 
	 perBagMBminPlus10 =0.0,  
	 perBagMBminPlus20 =0.0, 
	 perBagMBmakespanMin =0.0,  
	 perBagMBmakespanMinMinus10 =0.0,  
	 perBagMBmakespanMinMinus20 =0.0;
	
	public SampleStats(Bag b, int sSize) {
		bag = b;
		realE = bag.getExpectation();
		realVariance = bag.getVariance();
		realStDev  = bag.realStDev;
		sampleSize = sSize;
		bag.size =  bag.x.size() - sampleSize;
		
		RealBmin = bag.getRealMinimumBudget();				
		RealBminPlus10  = (long)Math.ceil(RealBmin *1.1);			
		RealBminPlus20  = (long)Math.ceil(RealBmin *1.2);
		RealBmakespanMin  =  bag.getRealMinimumMakespanBudget();
		RealBmakespanMinMinus10  = (long)Math.ceil(RealBmakespanMin *0.9);
		RealBmakespanMinMinus20  = (long)Math.ceil(RealBmakespanMin *0.8);
		
		RealMBmin  =  bag.computeRealMakespan(RealBmin ).get(0);
		RealMBminPlus10  =  bag.computeRealMakespan(RealBminPlus10 ).get(0);
		RealMBminPlus20  =  bag.computeRealMakespan(RealBminPlus20 ).get(0);
		RealMBmakespanMin  =  bag.computeRealMakespan(RealBmakespanMin ).get(0);
		RealMBmakespanMinMinus10  =  bag.computeRealMakespan(RealBmakespanMinMinus10 ).get(0);
		RealMBmakespanMinMinus20  =  bag.computeRealMakespan(RealBmakespanMinMinus20 ).get(0);
		
		bag.size =  bag.x.size();
	}
	
	public void addStats(Bag tmp) {
		
		long Bmin , BminPlus10 ,  BminPlus20 , 
		BmakespanMin , BmakespanMinMinus10 , BmakespanMinMinus20 ;
		
		Bmin =tmp.getMinimumBudget();				
		BminPlus10  = (long)Math.ceil(Bmin *1.1);			
		BminPlus20  = (long)Math.ceil(Bmin *1.2);
		BmakespanMin  = tmp.getMinimumMakespanBudget();
		BmakespanMinMinus10  = (long)Math.ceil(BmakespanMin *0.9);
		BmakespanMinMinus20  = (long)Math.ceil(BmakespanMin *0.8);
						
		int MBmin , MBminPlus10 ,  MBminPlus20 , 
		MBmakespanMin ,  MBmakespanMinMinus10 ,  MBmakespanMinMinus20 ;
		
		 MBmin  = tmp.computeMakespan( Bmin ).get(0);
		 MBminPlus10  = tmp.computeMakespan( BminPlus10 ).get(0);
		 MBminPlus20  = tmp.computeMakespan( BminPlus20 ).get(0);
		 MBmakespanMin  = tmp.computeMakespan( BmakespanMin ).get(0);
		 MBmakespanMinMinus10  = tmp.computeMakespan( BmakespanMinMinus10 ).get(0);
		 MBmakespanMinMinus20  = tmp.computeMakespan( BmakespanMinMinus20 ).get(0);
		
		 perBagE  += Math.pow(tmp.meanX- bag.realExpectation, 2); //in seconds
		 perBagStDev += Math.pow(tmp.stDevX- bag.realStDev, 2); //in seconds
		 perBagBmin  += Math.pow(Bmin -RealBmin ,2);
		 perBagBminPlus10  += Math.pow(BminPlus10 -RealBminPlus10 ,2);
		 perBagBminPlus20  += Math.pow(BminPlus20 -RealBminPlus20 ,2);
		 perBagBmakespanMin  += Math.pow(BmakespanMin -RealBmakespanMin ,2);
		 perBagBmakespanMinMinus10  += Math.pow(BmakespanMinMinus10 -
				 RealBmakespanMinMinus10 ,2);
		 perBagBmakespanMinMinus20  += Math.pow(BmakespanMinMinus20 -
				 RealBmakespanMinMinus20 ,2);
		
		 perBagMBmin  += Math.pow(MBmin  - RealMBmin ,2);
		 perBagMBminPlus10  += Math.pow(MBminPlus10  - RealMBminPlus10 ,2); 
		 perBagMBminPlus20  += Math.pow(MBminPlus20  - RealMBminPlus20 ,2);
	     perBagMBmakespanMin  += Math.pow(MBmakespanMin  - RealMBmakespanMin ,2); 
		 perBagMBmakespanMinMinus10  += Math.pow(MBmakespanMinMinus10  - 
				 RealMBmakespanMinMinus10 ,2);  
		 perBagMBmakespanMinMinus20  += Math.pow(MBmakespanMinMinus20  - 
				 RealMBmakespanMinMinus20 ,2);
	}
		
	
	public void printStats(int noExecutions) {
		System.out.println((double)realE + "\t" + 
				Math.sqrt((double)perBagE/noExecutions) + "\t" +
				(double)realStDev + "\t" +
				Math.sqrt((double)perBagStDev/noExecutions) + "\t" +
				(double)RealMBmin + "\t" +
				Math.sqrt((double)perBagMBmin/noExecutions) + "\t" +
				(double)RealMBminPlus10 + "\t" +
				Math.sqrt((double)perBagMBminPlus10/noExecutions) + "\t" +
				(double)RealMBminPlus20 + "\t" +
				Math.sqrt((double)perBagMBminPlus20/noExecutions) + "\t" +
				(double)RealMBmakespanMin + "\t" +
				Math.sqrt((double)perBagMBmakespanMin/noExecutions) + "\t" +
				(double)RealMBmakespanMinMinus10 + "\t" +
				Math.sqrt((double)perBagMBmakespanMinMinus10/noExecutions) + "\t" +
				(double)RealMBmakespanMinMinus20 + "\t" +
				Math.sqrt((double)perBagMBmakespanMinMinus20/noExecutions) + "\t" +
				(double)RealBmin + "\t" +
				Math.sqrt((double)perBagBmin/noExecutions) + "\t" +							   
				(double)RealBminPlus10 + "\t" +
				Math.sqrt((double)perBagBminPlus10/noExecutions) + "\t" +
				(double)RealBminPlus20 + "\t" +
				Math.sqrt((double)perBagBminPlus20/noExecutions) + "\t" +
				(double)RealBmakespanMin + "\t" +
				Math.sqrt((double)perBagBmakespanMin/noExecutions) + "\t" +
				(double)RealBmakespanMinMinus10 + "\t" +
				Math.sqrt((double)perBagBmakespanMinMinus10/noExecutions) + "\t" +
				(double)RealBmakespanMinMinus20 + "\t" +
				Math.sqrt((double)perBagBmakespanMinMinus20/noExecutions) 
		);
		 
	}
	
	
}
