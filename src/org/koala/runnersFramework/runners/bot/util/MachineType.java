package org.koala.runnersFramework.runners.bot.util;

public class MachineType {
	
	String name;
	double costUnit;
	/*expressed in minutes*/
	int atu;
	int Mmax;
	
	public MachineType(String name, double cost, int i, int Mmax) {		
		this.name = name;
		this.costUnit = cost;
		this.atu = i;
		this.Mmax = Mmax; 
	}	
}
