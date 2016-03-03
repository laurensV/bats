package org.koala.runnersFramework.runners.bot.util;

import ibis.ipl.IbisIdentifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.koala.runnersFramework.runners.bot.Cluster;
import org.koala.runnersFramework.runners.bot.Const;

public class SimulatedSpotCluster extends SimulatedCluster{
	
	//cost from SimulatedCluster represents biddingPrice;
//	Price spotPrice;
	int costOnDemand;
	ArrayList<Price> priceList;
	double scallingCoef;
	int currentPriceIndex;
	boolean terminated;
	Strategy strategy;
	int startIndex;
	int eps = 1;
	// Tp*cp;
	int productForMostProfitable; 

	public SimulatedSpotCluster(double speedFactor, int costFactor,
			String name, int maxNodes, int startIndex, String file, Strategy str, int costOnDemand) {
		super(speedFactor, costFactor, name, maxNodes);
		
		priceList = new ArrayList<Price>();
		getPriceList(file);
		
		scallingCoef = 0;
		currentPriceIndex = startIndex-1;
		this.startIndex = startIndex;
		terminated = false;
		this.strategy = str;
		this.costOnDemand = costOnDemand;
		this.cost = costOnDemand;
	}
	
	private void getPriceList(String file) {
		try {
			BufferedReader pricesFile = new BufferedReader(new FileReader(file));
			String line = pricesFile.readLine();
			while (line!=null) {
				String[] str = line.split(" ");
				String date = str[1].replace("T", ":");
				Price p = new Price(Double.parseDouble(str[0]), date);
				priceList.add(p);
				line = pricesFile.readLine();
			}
			pricesFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		Collections.sort(priceList);
		
	}
	
	public void setBiddingPrice(SimulatedCluster c) {
		if (strategy.equals(Strategy.Current)){
			cost = priceList.get(currentPriceIndex).getIntPrice();
			if (cost > costOnDemand)
				cost = costOnDemand;
		}
		else cost = costOnDemand;
	}
	
	@Override
	public int getCost(){
		if (strategy.equals(Strategy.OnDemand) || priceList.get(currentPriceIndex).getIntPrice() > cost)
			return cost;
		else return priceList.get(currentPriceIndex).getIntPrice();
	}
	
	/*@Override
	public void computeProfitability(SimulatedCluster cheapest) {
		profitability = (cheapest.Ti * ((SimulatedSpotCluster)cheapest).costOnDemand) / (Ti * costOnDemand);
		
	}
	*/
}
