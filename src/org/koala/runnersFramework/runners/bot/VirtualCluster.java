package org.koala.runnersFramework.runners.bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class VirtualCluster extends DAS4Cluster {

	private static final long serialVersionUID = 1L;
	public int noThreads;
	public double samplingEnergyCost = 0.0;
	public double samplingTime = 0.0; // in seconds
	public long samplingStartTime = 0;
	
	public boolean paused = false;
	
	double energyPerSec;
	double tasksPerSec;
	
	double energyPerBag;
	double makespan; // in seconds
	
	double avgPower;
	double medianPower;
	double maxPower;
	
	double avgEstimatedEnergyPerSec;
	double medianEstimatedEnergyPerSec;
	double maxEstimatedEnergyPerSec;
	
	double avgEnergyPerBag;
	double medianEnergyPerBag;
	double maxEnergyPerBag;
	boolean samplingTimeSet;

	public VirtualCluster(DAS4Cluster cl, int noThreads) {
		//long timeUnit,
		//double costUnit, int maxNodes, String speedFactor
		super(cl.hostname, cl.alias + "-" + noThreads, cl.timeUnit, cl.costUnit, cl.maxNodes, cl.speedFactor);
		//System.out.println("start time " + noThreads);
		samplingStartTime = System.currentTimeMillis();
		this.bookedNodes = cl.bookedNodes;
		this.noThreads = noThreads;
		this.samplingTimeSet = false;
	//	this.startTime = cl.startTime;

	}

	public void computeSamplingEnergyCost() {
		samplingEnergyCost = 0;
		double sum = 0;
		int nr = 0;
		DAS4Node node = null;
		ArrayList<Integer> power = null;
		ArrayList<Long> timestamp = null;
		//System.out.println("sampling energy cost");
		
		for (int i = 0; i < bookedNodes.size(); i++) {
			sum = 0;
			nr = 0;
			node = bookedNodes.get(i);
			power = node.power.get(noThreads);
			timestamp = node.timestamp.get(noThreads);
			if (power.size() > 1) {
				for (int j = 1; j < power.size(); j++) {
					//System.out.print(power.get(j)  + " power ");
					if (power.get(j).intValue() == 0) {
						j++;
					} else {
						 sum += (timestamp.get(j) - timestamp.get(j - 1))/1000.0;
						 nr++;
						//System.out.println("time difference " );
						samplingEnergyCost += (timestamp.get(j) - timestamp.get(j - 1))/1000.0
								* (power.get(j-1) + power.get(j))/2.0;
					}
					
				}
				/*if (node instanceof DAS4DoubleNode) {
					samplingEnergyCost /= 2.0;
				}*/
			} else if (power.size() == 2) {
				samplingEnergyCost = power.get(0) * samplingTime;
			}
			
		
		}
		maxPower = (double) Collections.max(power);
		
		ArrayList<Integer> validPw = new ArrayList<Integer>();
		
		avgPower = 0;
		int count = 0;
		for (int i = 0; i < power.size(); i++) {
			if (power.get(i) != 0) {
				validPw.add(power.get(i));
				avgPower += power.get(i);
				count ++;
				
			}
		}
		avgPower = avgPower / (double) count;
		
		
	
		
		//medianPower = validPw.get(validPw.size()/2);
	
		
		medianPower = select(validPw, validPw.size()/2);
		
		//System.out.println("these two should be equal " + qmedianPower + " = " + medianPower);
		avgEstimatedEnergyPerSec = avgPower;
		maxEstimatedEnergyPerSec = maxPower;
		medianEstimatedEnergyPerSec = medianPower;
		
		// average en meas interval 2.5
		double averageEnergyMeasuringInterval = sum/(double)nr;
		//System.out.println(averageEnergyMeasuringInterval + " avg meas interv");
		
	}
	
	private static int partition(ArrayList<Integer> arr, int left, int right, int pivot) {
		Integer pivotVal = arr.get(pivot);
		Collections.swap(arr, pivot, right);
		int storeIndex = left;
		for (int i = left; i < right; i++) {
			if (arr.get(i).compareTo(pivotVal) < 0) {
				Collections.swap(arr, i, storeIndex);
				storeIndex++;
			}
		}
		Collections.swap(arr, right, storeIndex);
		return storeIndex;
	}
	
	private static Integer select(ArrayList<Integer> arr, int n) {
		int left = 0;
		int right = arr.size() - 1;
		Random rand = new Random();
		while (right >= left) {
			int pivotIndex = partition(arr, left, right, rand.nextInt(right - left + 1) + left);
			if (pivotIndex == n) {
				return arr.get(pivotIndex);
			} else if (pivotIndex < n) {
				left = pivotIndex + 1;
			} else {
				right = pivotIndex - 1;
			}
		}
		return null;
	}
}
