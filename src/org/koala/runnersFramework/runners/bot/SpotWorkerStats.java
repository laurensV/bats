package org.koala.runnersFramework.runners.bot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ibis.ipl.IbisIdentifier;

public class SpotWorkerStats extends WorkerStats{

	double cost = 0;
	double lastPrice = 0;

	public SpotWorkerStats(String node, long startTime, IbisIdentifier ii) {
		super(node, startTime, ii);
	}
	
	@Override
	public boolean isUpdateNeeded(){
		return System.currentTimeMillis() - lastPriceQuery >= Const.hourInMilis ? true : false;
	}
	
	@Override
	public void updateCost(double price){
		cost += price;
		lastPrice = price;
	}
	
	@Override
	public double getCost(){
		return cost;
	}
	
	@Override
	public double getLastPrice(){
		return lastPrice;
	}

}
