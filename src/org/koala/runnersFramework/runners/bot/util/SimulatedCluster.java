package org.koala.runnersFramework.runners.bot.util;

public class SimulatedCluster {

	public double speedFactor;
	public int costFactor;
	public String name;
	public int maxNodes;
	public int cost;
	
	/*expressed in minutes*/
	public double Ti;
	public double stdev;
	public double profitability;
	public double profitabilityRealParameters;
	
	ConfidenceInterval ciMean, ciStdev;	
	
	public SimulatedCluster(double speedFactor, int costFactor, String name, int maxNodes) {
		super();
		this.speedFactor = speedFactor;
		this.costFactor = costFactor;
		this.name = name;
		this.maxNodes = maxNodes;
	}

	public void computeProfitability(SimulatedCluster cheapest) {
		profitability = (cheapest.Ti * cheapest.cost) / (Ti * cost);
		
	}

	public void computeProfitabilityRealParameters(SimulatedCluster cheapest, double realE) {
		profitabilityRealParameters = ( realE/cheapest.speedFactor * cheapest.cost) / (realE/speedFactor * cost);	
		
	}
	
	public ConfidenceInterval computeCIMean(double p_mu, int sampleSize) {
		ConfidenceInterval ci = new ConfidenceInterval();
		ci.estimate = Ti;
		ci.confidenceLevel = p_mu;
		
		double tnminus1 = umontreal.iro.lecuyer.probdist.StudentDist.inverseF(sampleSize-1, (1-p_mu)/2);
		double student = Math.abs(tnminus1/Math.sqrt(sampleSize));		
		ci.lowerBound = ci.estimate - student*stdev;
		ci.upperBound = ci.estimate + student*stdev;
		this.ciMean = ci;
		return ci;
	}

	
	public ConfidenceInterval computeCIStdev(double p_sigma, int sampleSize) {
		ConfidenceInterval ci = new ConfidenceInterval();
		ci.estimate = stdev;
		ci.confidenceLevel = p_sigma;
		double chisLow = flanagan.analysis.Stat.chiSquareInverseCDF(sampleSize-1, (1+p_sigma)/2);
		double chisHigh = flanagan.analysis.Stat.chiSquareInverseCDF(sampleSize-1, (1-p_sigma)/2);
		double nchiLow = Math.sqrt((sampleSize-1)/chisLow);
		double nchiHigh = Math.sqrt((sampleSize-1)/chisHigh);
		
		//System.out.println("cL=" + 1/chisLow + "; cH=" + 1/chisHigh);
		
		ci.lowerBound = nchiLow*ci.estimate;
		ci.upperBound = nchiHigh*ci.estimate;
		this.ciStdev = ci;
		return ci;
	}
	
	public int getCost(){
		return cost;
	}
}
