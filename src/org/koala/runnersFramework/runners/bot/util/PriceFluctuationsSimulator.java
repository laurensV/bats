package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class PriceFluctuationsSimulator {
	HashMap<String, SimulatedSpotCluster> clusters = new HashMap<String, SimulatedSpotCluster>();
	int rateOfChangeDelay = 10;

	// measured in seconds
	double currentElapsedTime = 0;

	public void addCluster(SimulatedSpotCluster cluster) {
		clusters.put(cluster.name, cluster);
	}

	public void setCoef(double makespan) { // makespan in minutes
		// long makespanMS = (long)(makespan * 1000 * 60);
		for (SimulatedSpotCluster c : clusters.values()) {
			ArrayList<Price> priceList = c.priceList;
			
			// difference in miliseconds
			long timeDifference = (priceList.get(priceList.size() - 1).date
					.getTime() - priceList.get(c.startIndex).date.getTime()) / 1000;

			c.scallingCoef = timeDifference / makespan;

		}
	}

	public void computeROC() {
		for (SimulatedSpotCluster c : clusters.values()) {
			for (int i = rateOfChangeDelay; i < c.priceList.size(); i++) {
				c.priceList.get(i).computeROC(
						c.priceList.get(i - rateOfChangeDelay).price);
			}
		}
	}

	public void computeExpMovingAverage() {
		for (SimulatedSpotCluster c : clusters.values()) {
			double exp = 2 / (double) c.priceList.size();
			for (int i = 1; i < c.priceList.size(); i++) {
				c.priceList.get(i).computeExpMovingAverage(
						c.priceList.get(i).expMovingAverage, exp);
			}
		}
	}

	// extraRt expressed in seconds
	public void updateElapsedTime(double extraRt) {
		currentElapsedTime += extraRt;

		for (SimulatedSpotCluster c : clusters.values()) {
			Date currentTime = getCurrentTime(c.name);

			int priceIndex = c.currentPriceIndex;

			ArrayList<Price> priceList = c.priceList;
			
			if (priceIndex+1 < priceList.size() && currentTime.compareTo(priceList.get(priceIndex + 1).date) >= 0) {
				c.currentPriceIndex ++;
			}
		}
	}

	public int getPrice(String type) {
		SimulatedSpotCluster c = clusters.get(type);
		ArrayList<Price> priceList = c.priceList;
		if (c.strategy != Strategy.OnDemand) {
			int priceIndex =  c.currentPriceIndex;
			int price = priceList.get(priceIndex).getIntPrice();
			if (price < c.costOnDemand)
				return price;
			else return c.costOnDemand;
		}
		else return c.costOnDemand;
	}

	public double getTimeUntilNextChange() {
		long nextTime;
		double min = Double.MAX_VALUE;
		for (SimulatedSpotCluster c : clusters.values()) {

			// System.out.println(type);

			Date currentTime = getCurrentTime(c.name);

			// System.out.println("currentTime "+ currentTime);

			int currentPriceIndex = c.currentPriceIndex;

			if ((currentPriceIndex + 1) < c.priceList.size()) {
				nextTime = c.priceList.get(currentPriceIndex + 1).date.getTime(); // milis

				// System.out.println("previousTime " +
				// priceMap.get(type).get(currentPriceIndex).date + " price " +
				// priceMap.get(type).get(currentPriceIndex).price);
				// System.out.println("nextTime " +
				// priceMap.get(type).get(currentPriceIndex+1).date + " price "
				// + priceMap.get(type).get(currentPriceIndex+1).price);
				
				double diff = (nextTime - currentTime.getTime()) / 1000;
				diff /= c.scallingCoef;
				diff *= 60;
				if (diff < min) {
					min = diff;
				}
				//System.out.println(min);
			}
		}
		// transform from miliseconds to seconds
		return min;
	}

	private Date getCurrentTime(String type) {
		SimulatedSpotCluster cluster = clusters.get(type);
		ArrayList<Price> priceList = cluster.priceList;
		// min * s / min
		// System.out.println("Elapsed time " + currentElapsedTime +
		// " coeficient " + scallingCoefMap.get(type));
		double convertedElapsedTime = currentElapsedTime / (double) 60
				* cluster.scallingCoef;// seconds

		// System.out.println("Converted elapsed time" + convertedElapsedTime);
		Calendar c = Calendar.getInstance();
		c.setTime(priceList.get(0).date);
		// add miliseconds
		c.add(Calendar.SECOND, (int) convertedElapsedTime);

		return c.getTime();
	}

	public boolean hasTerminated(String clusterName) {
		return !clusters.containsKey(clusterName);
	}
	/*public void sortLists() {
		for (String type : priceMap.keySet()) {
			ArrayList<Price> al = priceMap.get(type);
			Collections.sort(al);
			priceMap.put(type, al);
		}
	}*/
}
