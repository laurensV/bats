package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class SampleStatsMultipleClusters {
	
	Bag bag;
	
	/*expressed in seconds*/
	double realE;
	double realStDev;
	
	double realVariance;
	
	int sampleSize;
	int trueMean, trueStdev;
	double p_mu, p_sigma, p_M;
	boolean printInfo;
	SimulatedSchedule scheduleRealParametersExtraBudget, scheduleRealParameters;
	String budgetType;
	long BRealParametersExtraBudget, BRealParameters;
	/*expressed in minutes*/
	double MRealParametersExtraBudget, MRealParameters;
	
	
	ArrayList<ConfidenceInterval> meanConfInt = new ArrayList<ConfidenceInterval>();
	ArrayList<ConfidenceInterval> stdevConfInt = new ArrayList<ConfidenceInterval>();
		
	ArrayList<SimulatedSchedule> simSchedules = new ArrayList<SimulatedSchedule>();
	ArrayList<PredictionInterval> MPredInt = new ArrayList<PredictionInterval>();
	ArrayList<Long> budgets = new ArrayList<Long>();
	
	ArrayList<SimulatedSchedule> simSchedulesExtraB = new ArrayList<SimulatedSchedule>();
	ArrayList<PredictionInterval> MExtraBPredInt = new ArrayList<PredictionInterval>();
	ArrayList<Long> extraBudgets = new ArrayList<Long>();
	
	HashMap<String, ArrayList<SimulatedExecution>> executionStats = new HashMap<String, ArrayList<SimulatedExecution>>(); 
	HashMap<String, ArrayList<SimulatedExecution>> extraBExecutionStats = new HashMap<String, ArrayList<SimulatedExecution>>();
	
	public SampleStatsMultipleClusters(Bag b, int sSize, double p_mu, double p_sigma, double p_M) {
		bag = b;
		realE = bag.getExpectation();
		realVariance = bag.getVariance();
		realStDev  = bag.realStDev;
		sampleSize = sSize;		
		bag.size =  bag.x.size();
		
		this.p_mu = p_mu;
		this.p_sigma = p_sigma;
		this.p_M = p_M;
		
		trueMean = 0;
		trueStdev = 0;
	}
	
	public void selectBudget(String budgetType) {
		long budget;
		this.budgetType = budgetType;
		budget = bag.selectedBudgetRealParameters(budgetType);
		
		scheduleRealParameters = bag.computeMakespanEstimateRealParametersMultipleClusters(budget);		
		BRealParameters = scheduleRealParameters.budget;
		
		double speed = 0.0;
		for(String clusterName : scheduleRealParameters.machinesPerCluster.keySet()) {
			SimulatedCluster simCluster = bag.clusters.get(clusterName);
			int noMachines = scheduleRealParameters.machinesPerCluster.get(clusterName).intValue();			
			speed += noMachines/(realE/60/simCluster.speedFactor);	
		}
		
		MRealParameters = (bag.size-sampleSize)/speed;
		
		scheduleRealParametersExtraBudget = bag.computeExtraBudgetMakespanEstimateRealParametersMultipleClusters(budget, budgetType);		
		BRealParametersExtraBudget = scheduleRealParametersExtraBudget.budget;
		
		speed = 0.0;
		for(String clusterName : scheduleRealParametersExtraBudget.machinesPerCluster.keySet()) {
			SimulatedCluster simCluster = bag.clusters.get(clusterName);
			int noMachines = scheduleRealParametersExtraBudget.machinesPerCluster.get(clusterName).intValue();			
			speed += noMachines/(realE/60/simCluster.speedFactor);	
		}
		
		MRealParametersExtraBudget = (bag.size-sampleSize)/speed;
		
	}
	
	public void selectBudget(String budgetType, boolean realValuesInvolved) {
		if(realValuesInvolved == true) {
			selectBudget(budgetType);
			return;
		}

		long budget;
		this.budgetType = budgetType;

		double speed = 0.0;


	}
	
	public void addSampleStats(Bag tmp) {
		
		ConfidenceInterval ciMean = tmp.computeCIMean(p_mu);
		ConfidenceInterval ciStDev = tmp.computeCIStdev(p_sigma);
		
		meanConfInt.add(ciMean);
		stdevConfInt.add(ciStDev);
		
		if(ciMean.contains(realE)) {
			trueMean ++;
		}
		if(ciStDev.contains(realStDev)) {
			trueStdev ++;
		}	
	}
	
	public void addScheduleStats(SimulatedSchedule simSched,
			Bag tmpBag) {
		PredictionInterval pi = tmpBag.computePIMakespan(simSched, p_mu, p_sigma, p_M);
		MPredInt.add(pi);
		budgets.add(simSched.budget);	
		simSchedules.add(simSched);
	}

	public void addExtraBudgetScheduleStats(SimulatedSchedule simSched,
			Bag tmpBag) {
		PredictionInterval pi = tmpBag.computePIMakespan(simSched, p_mu, p_sigma, p_M);		
		MExtraBPredInt.add(pi);
		extraBudgets.add(simSched.budget);
		simSchedulesExtraB.add(simSched);
	}
	
	public void addExecutionStats(int j, SimulatedExecution simExec) {
		
		if(!executionStats.containsKey(""+j)) {
			executionStats.put("" + j, new ArrayList<SimulatedExecution>());
		}
		executionStats.get(""+j).add(simExec);
	}
	
	public void addExtraBudgetExecutionStats(int j, SimulatedExecution simExec) {
		
		if(!extraBExecutionStats.containsKey(""+j)) {
			extraBExecutionStats.put("" + j, new ArrayList<SimulatedExecution>());
		}
		extraBExecutionStats.get(""+j).add(simExec);
	}
	
	public void printStats(int noExecutions) {
		System.out.println((double)realE + "\t" + 
				Math.sqrt((double)trueMean/noExecutions) + "\t" +
				(double)realStDev + "\t" +
				Math.sqrt((double)trueStdev/noExecutions) + "\t" );
		 
	}
			
	public void printFormatted(String bagName, boolean verbose) {
		int k = 0;
		System.out.println(bagName + "\t" + realE/60 + "\t" + realStDev/60);
		System.out.println("Schedule based on real parameters \n" +
				"realMakespanEstimate = " + MRealParameters + "\n " +
				"realBEstimate = " + BRealParameters + "\n" +
				"configuration = " + scheduleRealParameters.machinesPerCluster );
		
		System.out.println("Schedule based on real parameters with delta B \n" +
				"realMakespanDeltaBEstimate = " + MRealParametersExtraBudget + "\n " +
				"realBDeltaBEstimate = " + BRealParametersExtraBudget + "\n" +
				"configuration = " + scheduleRealParametersExtraBudget.machinesPerCluster + 
				"\n initialDeltaN=" + scheduleRealParametersExtraBudget.deltaN +
				"\n deltaNExtraB=" + scheduleRealParametersExtraBudget.deltaNExtraB);
		
		if (verbose) {
			k = 0;
			System.out.println("\n Percentage confidence interval contains the true mean: " + (double)trueMean/meanConfInt.size());
			System.out.println("ExpNo" + "\t" + "mu_min" + "\t" + "mu_est" + "\t" + "mu_max");		
			for(ConfidenceInterval ciMean : meanConfInt) {
				k++;
				System.out.println(k + "\t" + ciMean.lowerBound/60 + "\t" + ciMean.estimate/60 + "\t" + ciMean.upperBound/60);
			}

			k=0;
			System.out.println("\n Percentage confidence interval contains the true stdev: " + (double)trueMean/stdevConfInt.size());
			System.out.println("ExpNo" + "\t" + "stdev_min" + "\t" + "stdev_est" + "\t" + "stdev_max");
			for(ConfidenceInterval ciStdev : stdevConfInt) {
				k++;
				System.out.println(k + "\t" + ciStdev.lowerBound/60 + "\t" + ciStdev.estimate/60 + "\t" + ciStdev.upperBound/60);
			}

			k=0;
			System.out.println("\n");
			System.out.println("ExpNo" + "\t" + "B_est" + "\t" + "B_max");
			for(SimulatedSchedule simExec : simSchedules) {
				k++;
				System.out.println(k + "\t" + simExec.budget + "\t" + simExec.budgetMax);			
			}	

			k=0;
			System.out.println("\n");
			System.out.println("ExpNo" + "\t" + "M_est" + "\t" + "M_max");
			for(PredictionInterval piM : MPredInt) {
				k++;
				System.out.println(k + "\t" + piM.estimate + "\t" + piM.upperBound);			
			}

			System.out.println("\n");
			System.out.println("M pdf");
			System.out.println("ExpNo"   + "\t" + "(<-50%)" 
					+ "\t" + "(-50%<=p<-20%)" 
					+ "\t" + "(-20%<=p<-10%)" 
					+ "\t" + "(-10%<=p<-5%)"
					+ "\t" + "(-5%<=p<-1%)"
					+ "\t" + "(-1%<=p<0%)"
					+ "\t" + "(0%<=p<1%)"
					+ "\t" + "(1%<=p<5%)"
					+ "\t" + "(5%<=p<10%)"
					+ "\t" + "(10%<=p<20%)"
					+ "\t" + "(20%<=p<50%)"
					+ "\t" + "(50%<=p)");
			for(k=0; k< MPredInt.size() ; k++) {
				double predictedMakespan = MPredInt.get(k).estimate;
				int[] percentageHistogram = categorizeHistogramMakespan(executionStats.get("" + k), predictedMakespan);
				System.out.print(""+(k+1)+"\t");
				for(int categ=0 ; categ<percentageHistogram.length; categ ++) {
					System.out.print(percentageHistogram[categ] + "\t");
				} 
				System.out.println();			
			}	

			System.out.println("\n");
			System.out.println("B pdf");
			System.out.println("ExpNo"   + "\t" + "(<-50%)" 
					+ "\t" + "(-50%<=p<-20%)" 
					+ "\t" + "(-20%<=p<-10%)" 
					+ "\t" + "(-10%<=p<-5%)"
					+ "\t" + "(-5%<=p<-1%)"
					+ "\t" + "(-1%<=p<=0%)"
					+ "\t" + "(0%<p<1%)"
					+ "\t" + "(1%<=p<5%)"
					+ "\t" + "(5%<=p<10%)"
					+ "\t" + "(10%<=p<20%)"
					+ "\t" + "(20%<=p<50%)"
					+ "\t" + "(50%<=p)");
			for(k=0; k < budgets.size() ; k++) {
				long estimatedBudget = budgets.get(k).longValue();
				int[] percentageHistogram = categorizeHistogramBudget(executionStats.get("" + k), estimatedBudget);
				System.out.print(""+(k+1)+"\t");
				for(int categ=0 ; categ<percentageHistogram.length; categ ++) {
					System.out.print(percentageHistogram[categ] + "\t");
				}
				System.out.println();
			}	

			k=0;
			System.out.println("\n");
			System.out.println("ExpNo" + "\t" + "B + Delta_B est" + "\t" + "B + Delta_B max");
			for(SimulatedSchedule simSched : simSchedulesExtraB) {
				k++;
				System.out.println(k + "\t" + simSched.budget + "\t" + simSched.budgetMax);			
			}			

			k=0;
			System.out.println("\n");
			System.out.println("ExpNo" + "\t" + "M with Delta_B est" + "\t" + "M with Delta_B max");
			for(PredictionInterval piM : MExtraBPredInt) {
				k++;
				System.out.println(k + "\t" + piM.estimate + "\t" + piM.upperBound);			
			}

			System.out.println("\n");
			System.out.println("M with Delta_B pdf");
			System.out.println("ExpNo"   + "\t" + "(<-50%)" 
					+ "\t" + "(-50%<=p<-20%)" 
					+ "\t" + "(-20%<=p<-10%)" 
					+ "\t" + "(-10%<=p<-5%)"
					+ "\t" + "(-5%<=p<-1%)"
					+ "\t" + "(-1%<=p<0%)"
					+ "\t" + "(0%<=p<1%)"
					+ "\t" + "(1%<=p<5%)"
					+ "\t" + "(5%<=p<10%)"
					+ "\t" + "(10%<=p<20%)"
					+ "\t" + "(20%<=p<50%)"
					+ "\t" + "(50%<=p)");
			for(k=0; k< MExtraBPredInt.size() ; k++) {
				double predictedMakespan = MExtraBPredInt.get(k).estimate;
				int[] percentageHistogram = categorizeHistogramMakespan(extraBExecutionStats.get("" + k), predictedMakespan);
				System.out.print(""+(k+1)+"\t");
				for(int categ=0 ; categ<percentageHistogram.length; categ ++) {
					System.out.print(percentageHistogram[categ] + "\t");
				} 
				System.out.println();
			}	

			System.out.println("\n");
			System.out.println("B + Delta_B pdf");
			System.out.println("ExpNo" + "\t" + "(<-50%)" 
					+ "\t" + "(-50%<=p<-20%)" 
					+ "\t" + "(-20%<=p<-10%)" 
					+ "\t" + "(-10%<=p<-5%)"
					+ "\t" + "(-5%<=p<-1%)"
					+ "\t" + "(-1%<=p<=0%)"
					+ "\t" + "(0%<p<1%)"
					+ "\t" + "(1%<=p<5%)"
					+ "\t" + "(5%<=p<10%)"
					+ "\t" + "(10%<=p<20%)"
					+ "\t" + "(20%<=p<50%)"
					+ "\t" + "(50%<=p)");
			for(k=0; k < extraBudgets.size() ; k++) {
				long estimatedBudget = extraBudgets.get(k).longValue();
				int[] percentageHistogram = categorizeHistogramBudget(extraBExecutionStats.get("" + k), estimatedBudget);
				System.out.print(""+(k+1)+"\t");
				for(int categ=0 ; categ<percentageHistogram.length; categ ++) {
					System.out.print(percentageHistogram[categ] + "\t");
				}
				System.out.println();
			}	

			System.out.println("\n");
			System.out.println("ExpNo" + "\t" + "Delta_N \t " +
					"p% of runs with overMNodes <= Delta_N \t " +
					"Delta_N_Extra_B \t " +
					"p% of runs with overMNodes <= Delta_N_Extra_B");
			for(k=0; k < simSchedulesExtraB.size() ; k++) {
				int deltaN = simSchedulesExtraB.get(k).deltaN ;
				int deltaNExtraB = simSchedulesExtraB.get(k).deltaNExtraB;
				int pDeltaN =0, pDeltaNExtraB = 0;
				for(SimulatedExecution simExec : executionStats.get(""+k)) {
					if( ((deltaN <= 0) && (simExec.noMachinesOverM == 0))
							|| ((deltaN > 0) && (simExec.noMachinesOverM > 0)) ) {
						pDeltaN ++;
					}
				}
				for(SimulatedExecution simExec : extraBExecutionStats.get(""+k)) {
					if( ((deltaNExtraB <= 0) && (simExec.noMachinesOverM == 0))
							|| ((deltaNExtraB > 0) && (simExec.noMachinesOverM > 0)) ) {
						pDeltaNExtraB ++;
					}
				}		
				System.out.println((k+1) + "\t" + simSchedulesExtraB.get(k).deltaN + "\t" + 
						(double)pDeltaN/executionStats.get(""+k).size() + "\t" + 
						simSchedulesExtraB.get(k).deltaNExtraB + "\t" +
						(double)pDeltaNExtraB/extraBExecutionStats.get(""+k).size());
			}

			System.out.println("\n");
			System.out.println("M%Mmax pdf");
			System.out.println("ExpNo"   + "\t" + "(<-50%)" 
					+ "\t" + "(-50%<=p<-20%)" 
					+ "\t" + "(-20%<=p<-10%)" 
					+ "\t" + "(-10%<=p<-5%)"
					+ "\t" + "(-5%<=p<-1%)"
					+ "\t" + "(-1%<=p<0%)"
					+ "\t" + "(0%<=p<1%)"
					+ "\t" + "(1%<=p<5%)"
					+ "\t" + "(5%<=p<10%)"
					+ "\t" + "(10%<=p<20%)"
					+ "\t" + "(20%<=p<50%)"
					+ "\t" + "(50%<=p)");
			for(k=0; k< MPredInt.size() ; k++) {
				double predictedMakespan = MPredInt.get(k).upperBound;
				int[] percentageHistogram = categorizeHistogramMakespan(executionStats.get("" + k), predictedMakespan);
				System.out.print(""+(k+1)+"\t");
				for(int categ=0 ; categ<percentageHistogram.length; categ ++) {
					System.out.print(percentageHistogram[categ] + "\t");
				} 
				System.out.println();			
			}

			System.out.println("\n");
			System.out.println("B%Bmax pdf");
			System.out.println("ExpNo"   + "\t" + "(<-50%)" 
					+ "\t" + "(-50%<=p<-20%)" 
					+ "\t" + "(-20%<=p<-10%)" 
					+ "\t" + "(-10%<=p<-5%)"
					+ "\t" + "(-5%<=p<-1%)"
					+ "\t" + "(-1%<=p<=0%)"
					+ "\t" + "(0%<p<1%)"
					+ "\t" + "(1%<=p<5%)"
					+ "\t" + "(5%<=p<10%)"
					+ "\t" + "(10%<=p<20%)"
					+ "\t" + "(20%<=p<50%)"
					+ "\t" + "(50%<=p)");
			for(k=0; k < budgets.size() ; k++) {
				long estimatedBudget = simSchedules.get(k).budgetMax;
				int[] percentageHistogram = categorizeHistogramBudget(executionStats.get("" + k), estimatedBudget);
				System.out.print(""+(k+1)+"\t");
				for(int categ=0 ; categ<percentageHistogram.length; categ ++) {
					System.out.print(percentageHistogram[categ] + "\t");
				}
				System.out.println();
			}

			System.out.println("\n");
			System.out.println("M%Mmax pdf for extraB schedule");
			System.out.println("ExpNo"   + "\t" + "(<-50%)" 
					+ "\t" + "(-50%<=p<-20%)" 
					+ "\t" + "(-20%<=p<-10%)" 
					+ "\t" + "(-10%<=p<-5%)"
					+ "\t" + "(-5%<=p<-1%)"
					+ "\t" + "(-1%<=p<0%)"
					+ "\t" + "(0%<=p<1%)"
					+ "\t" + "(1%<=p<5%)"
					+ "\t" + "(5%<=p<10%)"
					+ "\t" + "(10%<=p<20%)"
					+ "\t" + "(20%<=p<50%)"
					+ "\t" + "(50%<=p)");
			for(k=0; k< MExtraBPredInt.size() ; k++) {
				double predictedMakespan = MExtraBPredInt.get(k).upperBound;
				int[] percentageHistogram = categorizeHistogramMakespan(extraBExecutionStats.get("" + k), predictedMakespan);
				System.out.print(""+(k+1)+"\t");
				for(int categ=0 ; categ<percentageHistogram.length; categ ++) {
					System.out.print(percentageHistogram[categ] + "\t");
				} 
				System.out.println();			
			}

			System.out.println("\n");
			System.out.println("B%Bmax pdf for extraB schedule");
			System.out.println("ExpNo"   + "\t" + "(<-50%)" 
					+ "\t" + "(-50%<=p<-20%)" 
					+ "\t" + "(-20%<=p<-10%)" 
					+ "\t" + "(-10%<=p<-5%)"
					+ "\t" + "(-5%<=p<-1%)"
					+ "\t" + "(-1%<=p<=0%)"
					+ "\t" + "(0%<p<1%)"
					+ "\t" + "(1%<=p<5%)"
					+ "\t" + "(5%<=p<10%)"
					+ "\t" + "(10%<=p<20%)"
					+ "\t" + "(20%<=p<50%)"
					+ "\t" + "(50%<=p)");
			for(k=0; k < simSchedulesExtraB.size() ; k++) {
				long estimatedBudget = simSchedulesExtraB.get(k).budgetMax;
				int[] percentageHistogram = categorizeHistogramBudget(extraBExecutionStats.get("" + k), estimatedBudget);
				System.out.print(""+(k+1)+"\t");
				for(int categ=0 ; categ<percentageHistogram.length; categ ++) {
					System.out.print(percentageHistogram[categ] + "\t");
				}
				System.out.println();
			}
		}
		
	}
	
	private int[]  categorizeHistogramMakespan(ArrayList<SimulatedExecution> executions, double predictedMakespan) {
		
		int[] categories = new int[12];
		
		for(SimulatedExecution simExec : executions) {
			
			double percentage = (simExec.makespanMinutes/predictedMakespan)*100 - 100;
			if(percentage < -50) {
				categories[0] ++;
			} else if((-50 <= percentage) && (percentage < -20)) {
				categories[1] ++;
			} else if((-20 <= percentage) && (percentage < -10)) {
				categories[2] ++;
			} else if((-10 <= percentage) && (percentage < -5)) {
				categories[3] ++;
			} else if((-5 <= percentage) && (percentage < -1)) {
				categories[4] ++;
			} else if((-1 <= percentage) && (percentage <= 0)) {
				categories[5] ++;
			} else if((0 < percentage) && (percentage < 1)) {
				categories[6] ++;
			} else if((1 <= percentage) && (percentage < 5)) {
				categories[7] ++;
			} else if((5 <= percentage) && (percentage < 10)) {
				categories[8] ++;
			} else if((10 <= percentage) && (percentage < 20)) {
				categories[9] ++;
			} else if((20 <= percentage) && (percentage < 50)) {
				categories[10] ++;
			} else if(percentage >= 50) {
				categories[11] ++;
			}			
		}
		return categories;
	}
	
	private int[]  categorizeHistogramBudget(ArrayList<SimulatedExecution> executions, double estimatedBudget) {
		
		int[] categories = new int[12];
		
		for(SimulatedExecution simExec : executions) {
			
			double percentage = (simExec.cost/estimatedBudget)*100 - 100;
			if(percentage < -50) {
				categories[0] ++;
			} else if((-50 <= percentage) && (percentage < -20)) {
				categories[1] ++;
			} else if((-20 <= percentage) && (percentage < -10)) {
				categories[2] ++;
			} else if((-10 <= percentage) && (percentage < -5)) {
				categories[3] ++;
			} else if((-5 <= percentage) && (percentage < -1)) {
				categories[4] ++;
			} else if((-1 <= percentage) && (percentage <= 0)) {
				categories[5] ++;
			} else if((0 < percentage) && (percentage < 1)) {
				categories[6] ++;
			} else if((1 <= percentage) && (percentage < 5)) {
				categories[7] ++;
			} else if((5 <= percentage) && (percentage < 10)) {
				categories[8] ++;
			} else if((10 <= percentage) && (percentage < 20)) {
				categories[9] ++;
			} else if((20 <= percentage) && (percentage < 50)) {
				categories[10] ++;
			} else if(percentage >= 50) {
				categories[11] ++;
			}			
		}
		return categories;
	}
}
