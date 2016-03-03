package org.koala.runnersFramework.runners.bot;

public final class Const {
	static final int current = 1;
	static final int max = 2;
	static final int avg = 3;
	static final int onDemand = 0;
	
	static final long minuteInNanos = 60000000000L;
	static final int minuteInMilis = 60000;
	static final int timeout = 5 * minuteInMilis;
	static final int hourInMinutes = 60;
	static final int hourInMilis = hourInMinutes * minuteInMilis;
	

	static final String machineType = "Linux/UNIX";
	
	static String getString(int type){
		if (type == current) return "current price";
		else if (type == avg) return "average price";
		else if (type == max) return "max price";
		else return "no type";
	}
	
	static double getMinPrice(String instType){
		if (instType.equals("t1.micro"))
			return 3;
		if (instType.equals("m1.small"))
			return 7;
		if (instType.equals("m1.medium"))
			return 13;
		return 13;
	}
	 
}
