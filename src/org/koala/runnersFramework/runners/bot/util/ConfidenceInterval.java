package org.koala.runnersFramework.runners.bot.util;

public class ConfidenceInterval {

	/*expressed in seconds*/
	double lowerBound, upperBound;
	double confidenceLevel;
	boolean printInfo = false;
	double estimate;

	public boolean contains(double realE) {
		if((realE<lowerBound) || (realE>upperBound))
			return false;
		return true;		
	}
	
	void print() {
		if(printInfo)
			System.out.println(lowerBound + " " + upperBound);
	}
}
