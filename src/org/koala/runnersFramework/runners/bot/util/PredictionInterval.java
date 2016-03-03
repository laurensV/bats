package org.koala.runnersFramework.runners.bot.util;

public class PredictionInterval {

	/*expressed in minutes*/
	double upperBound;
	double confidenceLevel;
	boolean printInfo = false;
	double estimate;

	public boolean contains(double realM) {
		if(realM>upperBound)
			return false;
		return true;		
	}
	
	void print() {
		if(printInfo)
			System.out.println(upperBound);
	}
}
