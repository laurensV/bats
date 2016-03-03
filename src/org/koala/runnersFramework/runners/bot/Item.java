package org.koala.runnersFramework.runners.bot;

public class Item implements Comparable<Item>{

	public double profit;
	public int weight; //cost
	public String cluster; //name
	public boolean take;
	public int maxItems;
	
	public Item(double profit, int costUnit, String cluster) {
		super();
		this.profit = profit;
		this.weight = costUnit;
		this.cluster = cluster;
	}

	@Override
	public int compareTo(Item arg0) {
		return this.weight - arg0.weight;
	}	
}
