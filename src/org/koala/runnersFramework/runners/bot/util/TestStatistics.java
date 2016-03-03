package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class TestStatistics {

	public static int noExperiments = 1;
	public static int noExecutions = 10;
	private static int noSampleExperiments;
	private boolean printPerBag = false;
	
	public void testTailPhase() {
		
		noExperiments = 1; //30;
		noExecutions = 1; //10;

		Random randomizedSeed = new Random(99999999L);
		Random sampleSeeder = new Random(111111111L);		
		long sampleSeed = sampleSeeder.nextLong();		
		Random executionSeeder = new Random(888888888L);
		long experimentBagGeneratingSeed;
		long executionSeed;

		double p_mu = 0.9, p_sigma = 0.9, p_m = 0.9;

		int sampleSize = 30; 

		double desiredStDev = Math.sqrt(5);
		double desiredVar = desiredStDev * desiredStDev; 

		boolean useHisto = true;
		
		for(int j=0; j < noExperiments; j++) {
			int replicationFixesNDBCounter = 0;
			int migrationFixesNDBCounter = 0;
			int migrationEmergencyOnlyFixesNDBCounter = 0;
			
			int replicationFixesMDBCounter = 0;
			int migrationFixesMDBCounter = 0;
			int migrationEmergencyOnlyFixesMDBCounter = 0;
			
			int replicationFixesLTBCounter = 0;
			int migrationFixesLTBCounter = 0;
			int migrationEmergencyOnlyFixesLTBCounter = 0;
			
			int replicationFixesLTBSameVarCounter = 0;
			int migrationFixesLTBSameVarCounter = 0;
			int migrationEmergencyOnlyFixesLTBSameVarCounter = 0;
			
			int violateNDBCounter = 0;
			int violateMDBCounter = 0;
			int violateLTBCounter = 0;
			int violateLTBSameVarCounter = 0;
			
			experimentBagGeneratingSeed = randomizedSeed.nextLong();
			
			for (int i=0; i < noExecutions; i++) {	
				
				double replicationFixes, migrationFixes, migrationEmergencyOnlyFixes;
				executionSeed = executionSeeder.nextLong();
				
				
				NormalDistributedBag ndb = new NormalDistributedBag(1000, 
						experimentBagGeneratingSeed,
						15*60, desiredStDev*60,
						3, 4, 3);
						//4, 3, 3);
				
				ndb.useHisto = useHisto;
				
				ndb.getExpectation();
				ndb.getVariance();

				System.out.println("NDB");
				
				ndb.sample(sampleSeed, sampleSize);
				long BminNDB, BminPlus10NDB , BminPlus20NDB, 
				BmakespanMinNDB, BmakespanMinMinus10NDB, BmakespanMinMinus20NDB/**/;

				BminNDB=ndb.getMinimumBudget();				
				BminPlus10NDB = (long)Math.ceil((BminNDB*1.1));
				BminPlus20NDB = (long)Math.ceil((BminNDB*1.2));		
								
				BmakespanMinNDB = ndb.getMinimumMakespanBudget();
				BmakespanMinMinus10NDB = (long)Math.ceil(BmakespanMinNDB*0.9);
				BmakespanMinMinus20NDB = (long)Math.ceil(BmakespanMinNDB*0.8);
				
				//	System.out.print("B=" + BminPlus10);
				ArrayList<Integer> configNDB = ndb.computeMakespanWithCI(BminPlus10NDB, 
						p_mu, p_sigma, p_m);
				/*computeMakespanWithSuccessRate(BminPlus10NDB, "", 0.9);*/
				
				double violateNDB;
							
				System.out.println("\nNDB, replication-only");

				violateNDB = ndb.executeTail(executionSeed, 
						configNDB.get(0).intValue(), 
						configNDB.get(1).intValue(), 
						configNDB.get(2).intValue(),
						0);
				replicationFixes = violateNDB;
				/*System.out.println(violateNDB);
				System.out.println(ndb.overMnodes);*/

				System.out.println("NDB, replication-migration-emergency-only");
				ndb.emergencyOnly = true;
				violateNDB = ndb.executeTail(executionSeed, 
						configNDB.get(0).intValue(), 
						configNDB.get(1).intValue(), 
						configNDB.get(2).intValue(),
						sampleSize);
				migrationEmergencyOnlyFixes = violateNDB;
				/*System.out.println(violateNDB);
				System.out.println(ndb.overMnodes);*/

				
				System.out.println("NDB, replication-migration-emergency-moreTasks");
				ndb.emergencyOnly = false;
				violateNDB = ndb.executeTail(executionSeed, 
						configNDB.get(0).intValue(), 
						configNDB.get(1).intValue(), 
						configNDB.get(2).intValue(),
						sampleSize);
				migrationFixes = violateNDB;
				/*System.out.println(violateNDB);
				System.out.println(ndb.overMnodes);*/

				System.out.println("NDB, no tail optimization");

				violateNDB = ndb.execute(executionSeed, 
						configNDB.get(0).intValue(), 
						configNDB.get(1).intValue(), 
						configNDB.get(2).intValue());

				/*System.out.println(violateNDB);
				System.out.println(ndb.overMnodes);*/

				if(violateNDB > 0) {
					violateNDBCounter ++;
					if(replicationFixes <= 0) {
						replicationFixesNDBCounter ++;
					}
					if(migrationEmergencyOnlyFixes <= 0) {
						migrationEmergencyOnlyFixesNDBCounter ++;
					}
					if(migrationFixes <= 0) {
						migrationFixesNDBCounter ++;
					}
				}

				double mean1 = 48; double stdev1 = 29;
				double low2 = 106; double high2 = 607;
				double mean3 = 689; double stdev3 = 27;
				double low4 = 771; double high4 = 892;
				double low5 = 1649; double high5 = 2553;
				
				Random MDSeeder = new Random(experimentBagGeneratingSeed);
				long experimentBagGeneratingSeed0 = MDSeeder.nextLong();
				long experimentBagGeneratingSeed1 = MDSeeder.nextLong();
				long experimentBagGeneratingSeed2 = MDSeeder.nextLong();
				long experimentBagGeneratingSeed3 = MDSeeder.nextLong();
				long experimentBagGeneratingSeed4 = MDSeeder.nextLong();
				long experimentBagGeneratingSeed5 = MDSeeder.nextLong();
				
				DACHMixtureDistributedTruncatedBag mdb = new DACHMixtureDistributedTruncatedBag(1000,
						experimentBagGeneratingSeed1, mean1, stdev1,
						experimentBagGeneratingSeed2, low2, high2,
						experimentBagGeneratingSeed3, mean3, stdev3,
						experimentBagGeneratingSeed4, low4, high4,
						experimentBagGeneratingSeed5, low5, high5,
						0.274111675, 0.222335025, 0.452791878, 0.026395939,  
						experimentBagGeneratingSeed0,
						3,4,3);
						//4, 3, 3);
				
				mdb.useHisto = useHisto;
				
				mdb.getExpectation();
				mdb.getVariance();

				System.out.println("MDB");
				
				mdb.sample(sampleSeed, sampleSize);
				long BminMDB, BminPlus10MDB , BminPlus20MDB, 
				BmakespanMinMDB, BmakespanMinMinus10MDB, BmakespanMinMinus20MDB/**/;

				BminMDB=mdb.getMinimumBudget();				
				BminPlus10MDB = (long)Math.ceil((BminMDB*1.1));
				BminPlus20MDB = (long)Math.ceil((BminMDB*1.2));		
								
				BmakespanMinMDB = mdb.getMinimumMakespanBudget();
				BmakespanMinMinus10MDB = (long)Math.ceil(BmakespanMinMDB*0.9);
				BmakespanMinMinus20MDB = (long)Math.ceil(BmakespanMinMDB*0.8);
				
				//	System.out.print("B=" + BminPlus10);
				ArrayList<Integer> configMDB = mdb.computeMakespanWithCI(BminPlus10MDB, 
						p_mu, p_sigma, p_m);
				/*computeMakespanWithSuccessRate(BminPlus10NDB, "", 0.9);*/

				double violateMDB;
							
				System.out.println("\nMDB, replication-only");

				violateMDB = mdb.executeTail(executionSeed, 
						configMDB.get(0).intValue(), 
						configMDB.get(1).intValue(), 
						configMDB.get(2).intValue(),
						0);
				replicationFixes = violateMDB;
				/*System.out.println(violateMDB);
				System.out.println(mdb.overMnodes);*/

				System.out.println("MDB, replication-migration-emergency-only");
				mdb.emergencyOnly = true;
				violateMDB = mdb.executeTail(executionSeed, 
						configMDB.get(0).intValue(), 
						configMDB.get(1).intValue(), 
						configMDB.get(2).intValue(),
						sampleSize);
				migrationEmergencyOnlyFixes = violateMDB;
				/*System.out.println(violateMDB);
				System.out.println(mdb.overMnodes);*/

				
				System.out.println("MDB, replication-migration-emergency-moreTasks");
				mdb.emergencyOnly = false;
				violateMDB = mdb.executeTail(executionSeed, 
						configMDB.get(0).intValue(), 
						configMDB.get(1).intValue(), 
						configMDB.get(2).intValue(),
						sampleSize);
				migrationFixes = violateMDB;
				/*System.out.println(violateMDB);
				System.out.println(mdb.overMnodes);*/

				System.out.println("MDB, no tail optimization");

				violateMDB = mdb.execute(executionSeed, 
						configMDB.get(0).intValue(), 
						configMDB.get(1).intValue(), 
						configMDB.get(2).intValue());

				/*System.out.println(violateMDB);
				System.out.println(mdb.overMnodes);*/

				if(violateMDB > 0) {
					violateMDBCounter ++;
					if(replicationFixes <= 0) {
						replicationFixesMDBCounter ++;
					}
					if(migrationEmergencyOnlyFixes <= 0) {
						migrationEmergencyOnlyFixesMDBCounter ++;
					}
					if(migrationFixes <= 0) {
						migrationFixesMDBCounter ++;
					}
				}

				LevyTruncatedDistributedBag ltb = new LevyTruncatedDistributedBag(1000, 
						experimentBagGeneratingSeed,
						0.366, 2700,
						3, 4, 3);
						//4, 3, 3);
				
				ltb.useHisto = useHisto;
				
				double ltbExpectation = ltb.getExpectation();
				double ltbVariance = ltb.getVariance();

				System.out.println("LTB");
				ltb.sample(sampleSeed, sampleSize);

				long BminLTB, BminPlus10LTB , BminPlus20LTB, 
				BmakespanMinLTB, BmakespanMinMinus10LTB, BmakespanMinMinus20LTB/**/;

				BminLTB=ltb.getMinimumBudget();			
				BminPlus10LTB = (long)Math.ceil((BminLTB*1.1));
				BminPlus20LTB = (long)Math.ceil((BminLTB*1.2));			

				BmakespanMinLTB = ltb.getMinimumMakespanBudget();
				BmakespanMinMinus10LTB = (long)Math.ceil(BmakespanMinLTB*0.9);
				BmakespanMinMinus20LTB = (long)Math.ceil(BmakespanMinLTB*0.8);
				
				//	System.out.print("B=" + BminPlus10);
				ArrayList<Integer> configLTB = ltb.computeMakespanWithCI(BminPlus10LTB,
						p_mu, p_sigma, p_m);
				/*computeMakespanWithSuccessRate(BminPlus10LTB, "", 0.9);*/
				
				
				double violateLTB;
								
				System.out.println("\nLTB, replication-only");

				violateLTB = ltb.executeTail(executionSeed, 
						configLTB.get(0).intValue(), 
						configLTB.get(1).intValue(), 
						configLTB.get(2).intValue(),
						0);
				replicationFixes = violateLTB;
								
				/*System.out.println(violateLTB);
				System.out.println(ltb.overMnodes);*/

				System.out.println("LTB, replication-migration-emergency-only");
				ltb.emergencyOnly = true;
				violateLTB = ltb.executeTail(executionSeed, 
						configLTB.get(0).intValue(), 
						configLTB.get(1).intValue(), 
						configLTB.get(2).intValue(),
						sampleSize);
				migrationEmergencyOnlyFixes = violateLTB;
				
				/*System.out.println(violateLTB);
				System.out.println(ltb.overMnodes);*/

				System.out.println("LTB, replication-migration-emergency-moreTasks");
				ltb.emergencyOnly = false;
				violateLTB = ltb.executeTail(executionSeed, 
						configLTB.get(0).intValue(), 
						configLTB.get(1).intValue(), 
						configLTB.get(2).intValue(),
						sampleSize);
				migrationFixes = violateLTB;
				
				/*System.out.println(violateLTB);
				System.out.println(ltb.overMnodes);*/
				
				System.out.println("LTB, no tail optimization");

				violateLTB = ltb.execute(executionSeed, 
						configLTB.get(0).intValue(), 
						configLTB.get(1).intValue(), 
						configLTB.get(2).intValue());
								
				/*System.out.println(violateLTB);
				System.out.println(ltb.overMnodes);*/

				if(violateLTB > 0) {
					violateLTBCounter ++;
					if(replicationFixes <= 0) {
						replicationFixesLTBCounter ++;
					}
					if(migrationEmergencyOnlyFixes <= 0) {
						migrationEmergencyOnlyFixesLTBCounter ++;
					}
					if(migrationFixes <= 0) {
						migrationFixesLTBCounter ++;
					}
				}

				LevyTruncatedDistributedBag ltbSameVar = 
					new LevyTruncatedDistributedBag(1000,							
							experimentBagGeneratingSeed,
							0.366, 2700,
							Math.sqrt(desiredVar*3600/ltbVariance),
							ltbExpectation*(1-Math.sqrt(desiredVar*3600/ltbVariance)),
							3, 4, 3);
							//4, 3, 3);
				
				ltbSameVar.useHisto = useHisto;
				
				ltbSameVar.getExpectation();
				ltbSameVar.getVariance();

				System.out.println("LTBSameVar");
				ltbSameVar.sample(sampleSeed, sampleSize);

				long BminLTBSameVar, BminPlus10LTBSameVar , BminPlus20LTBSameVar, 
				BmakespanMinLTBSameVar, BmakespanMinMinus10LTBSameVar, BmakespanMinMinus20LTBSameVar/**/;

				BminLTBSameVar=ltbSameVar.getMinimumBudget();	
				BminPlus10LTBSameVar = (long)Math.ceil(BminLTBSameVar*1.1);
				BminPlus20LTBSameVar = (long)Math.ceil(BminLTBSameVar*1.2);			

				BmakespanMinLTBSameVar = ltbSameVar.getMinimumMakespanBudget();
				BmakespanMinMinus10LTBSameVar = (long)Math.ceil(BmakespanMinLTBSameVar*0.9);
				BmakespanMinMinus20LTBSameVar = (long)Math.ceil(BmakespanMinLTBSameVar*0.8);
				
				//	System.out.print("B=" + BminPlus10);
				ArrayList<Integer> configLTBSameVar = ltbSameVar.computeMakespanWithCI(BminPlus10LTBSameVar, 
						p_mu, p_sigma, p_m);
				/*computeMakespanWithSuccessRate(BminPlus10LTBSameVar, "", 0.9);*/
				
				
				double violateLTBSameVar; 
				
				System.out.println("\nLTBSameVar, replication-only");

				violateLTBSameVar = ltbSameVar.executeTail(executionSeed, 
						configLTBSameVar.get(0).intValue(),
						configLTBSameVar.get(1).intValue(), 
						configLTBSameVar.get(2).intValue(),
						0);
				replicationFixes = violateLTBSameVar;				
				/*System.out.println(violateLTBSameVar);
				System.out.println(ltbSameVar.overMnodes);*/

				System.out.println("LTBSameVar, replication-migration-emergency-only");
				ltbSameVar.emergencyOnly = true;
				violateLTBSameVar = ltbSameVar.executeTail(executionSeed, 
						configLTBSameVar.get(0).intValue(),
						configLTBSameVar.get(1).intValue(), 
						configLTBSameVar.get(2).intValue(),
						sampleSize);
				migrationEmergencyOnlyFixes = violateLTBSameVar;				
				/*System.out.println(violateLTBSameVar);
				System.out.println(ltbSameVar.overMnodes);*/
				
				System.out.println("LTBSameVar, replication-migration-emergency-moreTasks");
				ltbSameVar.emergencyOnly = false;
				violateLTBSameVar = ltbSameVar.executeTail(executionSeed, 
						configLTBSameVar.get(0).intValue(),
						configLTBSameVar.get(1).intValue(), 
						configLTBSameVar.get(2).intValue(),
						sampleSize);
				migrationFixes = violateLTBSameVar; 				
				/*System.out.println(violateLTBSameVar);
				System.out.println(ltbSameVar.overMnodes);*/

				System.out.println("LTBSameVar, no tail optimization");

				violateLTBSameVar = ltbSameVar.execute(executionSeed, 
						configLTBSameVar.get(0).intValue(),
						configLTBSameVar.get(1).intValue(), 
						configLTBSameVar.get(2).intValue());
				
				/*System.out.println(violateLTBSameVar);
				System.out.println(ltbSameVar.overMnodes);*/
				
				if(violateLTBSameVar > 0) {
					violateLTBSameVarCounter ++;
					if(replicationFixes <= 0) {
						replicationFixesLTBSameVarCounter ++;
					}
					if(migrationEmergencyOnlyFixes <= 0) {
						migrationEmergencyOnlyFixesLTBSameVarCounter ++;
					}
					if(migrationFixes <= 0) {
						migrationFixesLTBSameVarCounter ++;
					}
				}
			
			}
			
			System.out.println("Violations NDB: " + (double)violateNDBCounter/(double)noExecutions);
			System.out.println("Violations MDB: " + (double)violateMDBCounter/(double)noExecutions);
			System.out.println("Violations LTB: " + (double)violateLTBCounter/(double)noExecutions);
			System.out.println("Violations LTB Same Var: " + (double)violateLTBSameVarCounter/(double)noExecutions);
			
			System.out.println("Replication fixes NDB: " + (double)replicationFixesNDBCounter/(double)violateNDBCounter);			
			System.out.println("Emergency only Migration fixes NDB: " + (double)migrationEmergencyOnlyFixesNDBCounter/(double)violateNDBCounter);
			System.out.println("Migration fixes NDB: " + (double)migrationFixesNDBCounter/(double)violateNDBCounter);
			
			System.out.println("Replication fixes MDB: " + (double)replicationFixesMDBCounter/(double)violateMDBCounter);
			System.out.println("Emergency only Migration fixes MDB: " + (double)migrationEmergencyOnlyFixesMDBCounter/(double)violateMDBCounter);
			System.out.println("Migration fixes MDB: " + (double)migrationFixesMDBCounter/(double)violateMDBCounter);
			
			System.out.println("Replication fixes LTB: " + (double)replicationFixesLTBCounter/(double)violateLTBCounter);
			System.out.println("Emergency only Migration fixes LTB: " + (double)migrationEmergencyOnlyFixesLTBCounter/(double)violateLTBCounter);
			System.out.println("Migration fixes LTB: " + (double)migrationFixesLTBCounter/(double)violateLTBCounter);
			
			System.out.println("Replication fixes LTBSameVar: " + (double)replicationFixesLTBSameVarCounter/(double)violateLTBSameVarCounter);
			System.out.println("Emergency only  Migration fixes LTBSameVar: " + (double)migrationEmergencyOnlyFixesLTBSameVarCounter/(double)violateLTBSameVarCounter);
			System.out.println("Migration fixes LTBSameVar: " + (double)migrationFixesLTBSameVarCounter/(double)violateLTBSameVarCounter);
		}
	}

	public void testTailPhaseMultipleClusters() {
		
		noExperiments = 30;
		noExecutions = 30;

		SimulatedCluster clusterA, clusterB, clusterC;
		HashMap<String,SimulatedCluster> clusters;
				
		Random randomizedSeed = new Random(99999999L);
		Random sampleSeeder = new Random(111111111L);		
		long sampleSeed = sampleSeeder.nextLong();		
		Random executionSeeder = new Random(888888888L);
		long experimentBagGeneratingSeed;
		long executionSeed;

		double p_mu = 0.9, p_sigma = 0.9, p_m = 0.9;

		int sampleSize = 30; 

		double desiredStDev = Math.sqrt(5);
		double desiredVar = desiredStDev * desiredStDev; 

		boolean useHisto = true;
		
		HashMap<String,ArrayList<Double>> avgPercentageNDB = new HashMap<String,ArrayList<Double>>();
		avgPercentageNDB.put("ER", new ArrayList<Double>());
		avgPercentageNDB.put("ERM", new ArrayList<Double>());
		avgPercentageNDB.put("PR", new ArrayList<Double>());
		avgPercentageNDB.put("RR", new ArrayList<Double>());
		avgPercentageNDB.put("NOTO", new ArrayList<Double>());
		
		HashMap<String,ArrayList<Double>> avgPercentageMDB = new HashMap<String,ArrayList<Double>>();
		avgPercentageMDB.put("ER", new ArrayList<Double>());
		avgPercentageMDB.put("ERM", new ArrayList<Double>());
		avgPercentageMDB.put("PR", new ArrayList<Double>());
		avgPercentageMDB.put("RR", new ArrayList<Double>());
		avgPercentageMDB.put("NOTO", new ArrayList<Double>());
		
		HashMap<String,ArrayList<Double>> avgPercentageLTB = new HashMap<String,ArrayList<Double>>();
		avgPercentageLTB.put("ER", new ArrayList<Double>());
		avgPercentageLTB.put("ERM", new ArrayList<Double>());
		avgPercentageLTB.put("PR", new ArrayList<Double>());
		avgPercentageLTB.put("RR", new ArrayList<Double>());	
		avgPercentageLTB.put("NOTO", new ArrayList<Double>());
		
		HashMap<String,ArrayList<Double>> avgPercentageLTBSameVar = new HashMap<String,ArrayList<Double>>();
		avgPercentageLTBSameVar.put("ER", new ArrayList<Double>());
		avgPercentageLTBSameVar.put("ERM", new ArrayList<Double>());
		avgPercentageLTBSameVar.put("PR", new ArrayList<Double>());
		avgPercentageLTBSameVar.put("RR", new ArrayList<Double>());
		avgPercentageLTBSameVar.put("NOTO", new ArrayList<Double>());
		
		for(int j=0; j < noExperiments; j++) {
			
			int replicationFixesNDBCounter = 0;
			int migrationFixesNDBCounter = 0;
			int perfectReplicationFixesNDBCounter = 0;
			int randomReplicationFixesNDBCounter = 0;
			HashMap<String,ArrayList<Double>> overMNDB = new HashMap<String,ArrayList<Double>>();
			overMNDB.put("ER", new ArrayList<Double>());
			overMNDB.put("ERM", new ArrayList<Double>());
			overMNDB.put("PR", new ArrayList<Double>());
			overMNDB.put("RR", new ArrayList<Double>());
			overMNDB.put("NOTO", new ArrayList<Double>());
			SimulatedSchedule configNDB = null;
			
			int replicationFixesMDBCounter = 0;
			int migrationFixesMDBCounter = 0;
			int perfectReplicationFixesMDBCounter = 0;
			int randomReplicationFixesMDBCounter = 0;
			HashMap<String,ArrayList<Double>> overMMDB = new HashMap<String,ArrayList<Double>>();
			overMMDB.put("ER", new ArrayList<Double>());
			overMMDB.put("ERM", new ArrayList<Double>());
			overMMDB.put("PR", new ArrayList<Double>());
			overMMDB.put("RR", new ArrayList<Double>());
			overMMDB.put("NOTO", new ArrayList<Double>());
			SimulatedSchedule configMDB = null;
			
			int replicationFixesLTBCounter = 0;
			int migrationFixesLTBCounter = 0;
			int perfectReplicationFixesLTBCounter = 0;
			int randomReplicationFixesLTBCounter = 0;
			HashMap<String,ArrayList<Double>> overMLTB = new HashMap<String,ArrayList<Double>>();
			overMLTB.put("ER", new ArrayList<Double>());
			overMLTB.put("ERM", new ArrayList<Double>());
			overMLTB.put("PR", new ArrayList<Double>());
			overMLTB.put("RR", new ArrayList<Double>());	
			overMLTB.put("NOTO", new ArrayList<Double>());
			SimulatedSchedule configLTB = null;
			
			int replicationFixesLTBSameVarCounter = 0;
			int migrationFixesLTBSameVarCounter = 0;
			int perfectReplicationFixesLTBSameVarCounter = 0;
			int randomReplicationFixesLTBSameVarCounter = 0;
			HashMap<String,ArrayList<Double>> overMLTBSameVar = new HashMap<String,ArrayList<Double>>();
			overMLTBSameVar.put("ER", new ArrayList<Double>());
			overMLTBSameVar.put("ERM", new ArrayList<Double>());
			overMLTBSameVar.put("PR", new ArrayList<Double>());
			overMLTBSameVar.put("RR", new ArrayList<Double>());
			overMLTBSameVar.put("NOTO", new ArrayList<Double>());
			SimulatedSchedule configLTBSameVar = null;
			
			int violateNDBCounter = 0;
			int violateMDBCounter = 0;
			int violateLTBCounter = 0;
			int violateLTBSameVarCounter = 0;
			
			experimentBagGeneratingSeed = randomizedSeed.nextLong();
			
			System.out.println("----------->Experiment " + j + "<-----------");
			
			for (int i=0; i < noExecutions; i++) {	
				
				System.out.println("---------->>Execution " + j + "." + i + "<<--------");
				
				double replicationFixes, migrationFixes, perfectReplicationFixes, randomReplicationFixes;
				executionSeed = executionSeeder.nextLong();
				
				
				NormalDistributedBag ndb = new NormalDistributedBag(1000, 
						experimentBagGeneratingSeed,
						15*60, desiredStDev*60,
						3, 4, 3);
						//4, 3, 3);
		
				
				ndb.useHisto = useHisto;
				
				ndb.getExpectation();
				ndb.getVariance();

				System.out.println("NDB");
				
				clusterA = new SimulatedCluster(1,1,"A",24);
				clusterB = new SimulatedCluster(3,4,"B",24);
				clusterC = new SimulatedCluster(2,1,"C",24);
				clusters = new HashMap<String,SimulatedCluster>();
				clusters.put("A", clusterA);
				clusters.put("B", clusterB);
				clusters.put("C", clusterC);
				ndb.setClusters(clusters, sampleSeed, sampleSize);
				
				ndb.sampleMultipleClusters(sampleSeed, sampleSize);
								
				long BminNDB, BminPlus10NDB , BminPlus20NDB, 
				BmakespanMinNDB, BmakespanMinMinus10NDB, BmakespanMinMinus20NDB/**/;

				BminNDB=ndb.getMinimumBudgetMultipleClusters();				
				BminPlus10NDB = (long)Math.ceil((BminNDB*1.1));
				BminPlus20NDB = (long)Math.ceil((BminNDB*1.2));		
								
				BmakespanMinNDB = ndb.getMinimumMakespanBudgetMultipleClusters();
				BmakespanMinMinus10NDB = (long)Math.ceil(BmakespanMinNDB*0.9);
				BmakespanMinMinus20NDB = (long)Math.ceil(BmakespanMinNDB*0.8);
				
				//	System.out.print("B=" + BminPlus10);
				configNDB = ndb.computeMakespanEstimateMultipleClusters(BminPlus10NDB);
				System.out.println(configNDB.toString());
				/*computeMakespanWithSuccessRate(BminPlus10NDB, "", 0.9);*/
				
				double violateNDB;
				
				ndb.knowledge = Bag.ESTIMATES;
				
				System.out.println("\nNDB, estimates-replication-only-largestExpectedRt");

				violateNDB = ndb.executeTailMultipleClusters(executionSeed, 
						configNDB, 
						0);
				replicationFixes = violateNDB;				
				/*System.out.println(violateNDB);
				System.out.println(ndb.overMnodes);*/
								
				System.out.println("\nNDB, estimates-replication-migration-largestExpectedRt");
				ndb.emergencyOnly = false;
				violateNDB = ndb.executeTailMultipleClusters(executionSeed, 
						configNDB, 
						sampleSize);
				migrationFixes = violateNDB;
				/*System.out.println(violateNDB);
				System.out.println(ndb.overMnodes);*/

				ndb.knowledge = Bag.PERFECT;				
				System.out.println("\nNDB, perfect-replication-only-largestExpectedRt");

				violateNDB = ndb.executeTailMultipleClusters(executionSeed, 
						configNDB, 
						0);
				perfectReplicationFixes = violateNDB;
				/*System.out.println(violateNDB);
				System.out.println(ndb.overMnodes);*/
					
				ndb.knowledge = Bag.RANDOM;
				System.out.println("\nNDB, random-replication-only-largestExpectedRt");
				
				violateNDB = ndb.executeTailMultipleClusters(executionSeed, 
						configNDB, 
						0);
				randomReplicationFixes = violateNDB;
				/*System.out.println(violateNDB);
				System.out.println(ndb.overMnodes);*/

				
				System.out.println("\nNDB, no tail optimization");

				violateNDB = ndb.executeMultipleClusters(executionSeed, 
						configNDB);

				/*System.out.println(violateNDB);
				System.out.println(ndb.overMnodes);*/

				if(violateNDB > 0) {
					violateNDBCounter ++;
					if(replicationFixes <= 0) {
						replicationFixesNDBCounter ++;
					}
					if(migrationFixes <= 0) {
						migrationFixesNDBCounter ++;
					}
					if(perfectReplicationFixes <= 0) {
						perfectReplicationFixesNDBCounter ++;
					}
					if(randomReplicationFixes <= 0) {
						randomReplicationFixesNDBCounter ++;
					}
				}

				overMNDB.get("ER").add(replicationFixes);
				overMNDB.get("ERM").add(migrationFixes);
				overMNDB.get("PR").add(perfectReplicationFixes);
				overMNDB.get("RR").add(randomReplicationFixes);
				overMNDB.get("NOTO").add(violateNDB);
				
				
				double mean1 = 48; double stdev1 = 29;
				double low2 = 106; double high2 = 607;
				double mean3 = 689; double stdev3 = 27;
				double low4 = 771; double high4 = 892;
				double low5 = 1649; double high5 = 2553;
				
				Random MDSeeder = new Random(experimentBagGeneratingSeed);
				long experimentBagGeneratingSeed0 = MDSeeder.nextLong();
				long experimentBagGeneratingSeed1 = MDSeeder.nextLong();
				long experimentBagGeneratingSeed2 = MDSeeder.nextLong();
				long experimentBagGeneratingSeed3 = MDSeeder.nextLong();
				long experimentBagGeneratingSeed4 = MDSeeder.nextLong();
				long experimentBagGeneratingSeed5 = MDSeeder.nextLong();
				
				DACHMixtureDistributedTruncatedBag mdb = new DACHMixtureDistributedTruncatedBag(1000,
						experimentBagGeneratingSeed1, mean1, stdev1,
						experimentBagGeneratingSeed2, low2, high2,
						experimentBagGeneratingSeed3, mean3, stdev3,
						experimentBagGeneratingSeed4, low4, high4,
						experimentBagGeneratingSeed5, low5, high5,
						0.274111675, 0.222335025, 0.452791878, 0.026395939,  
						experimentBagGeneratingSeed0,
						3,4,3);
						//4, 3, 3);
				
				mdb.useHisto = useHisto;				
				
				mdb.getExpectation();
				mdb.getVariance();

				System.out.println("MDB");
				
				clusterA = new SimulatedCluster(1,1,"A",24);
				clusterB = new SimulatedCluster(3,4,"B",24);
				clusterC = new SimulatedCluster(2,1,"C",24);
				clusters = new HashMap<String,SimulatedCluster>();
				clusters.put("A", clusterA);
				clusters.put("B", clusterB);
				clusters.put("C", clusterC);
				mdb.setClusters(clusters, sampleSeed, sampleSize);
								
				mdb.sampleMultipleClusters(sampleSeed, sampleSize);
				
				long BminMDB, BminPlus10MDB , BminPlus20MDB, 
				BmakespanMinMDB, BmakespanMinMinus10MDB, BmakespanMinMinus20MDB/**/;

				BminMDB=mdb.getMinimumBudgetMultipleClusters();				
				BminPlus10MDB = (long)Math.ceil((BminMDB*1.1));
				BminPlus20MDB = (long)Math.ceil((BminMDB*1.2));		
								
				BmakespanMinMDB = mdb.getMinimumMakespanBudgetMultipleClusters();
				BmakespanMinMinus10MDB = (long)Math.ceil(BmakespanMinMDB*0.9);
				BmakespanMinMinus20MDB = (long)Math.ceil(BmakespanMinMDB*0.8);
				
				//	System.out.print("B=" + BminPlus10);
				configMDB = mdb.computeMakespanEstimateMultipleClusters(BminPlus10MDB);
				System.out.println(configMDB.toString());
				/*computeMakespanWithSuccessRate(BminPlus10NDB, "", 0.9);*/

				/*
				if(j == 1) {
					mdb.printDebug = true;
				}
				*/
				
				double violateMDB;
							
				mdb.knowledge = Bag.ESTIMATES;
				System.out.println("\nMDB, estimates-replication-only-largestExpectedRt");

				violateMDB = mdb.executeTailMultipleClusters(executionSeed, 
						configMDB,
						0);
				replicationFixes = violateMDB;
				/*System.out.println(violateMDB);
				System.out.println(mdb.overMnodes);*/
		
				
				System.out.println("\nMDB, estimates-replication-migration-largestExpectedRt");
				mdb.emergencyOnly = false;
				violateMDB = mdb.executeTailMultipleClusters(executionSeed, 
						configMDB,
						sampleSize);
				migrationFixes = violateMDB;
				/*System.out.println(violateMDB);
				System.out.println(mdb.overMnodes);*/

				
				mdb.knowledge = Bag.PERFECT;
				System.out.println("\nMDB, perfect-replication-only-largestExpectedRt");

				violateMDB = mdb.executeTailMultipleClusters(executionSeed, 
						configMDB,
						0);
				perfectReplicationFixes = violateMDB;
				/*System.out.println(violateMDB);
				System.out.println(mdb.overMnodes);*/
		
				mdb.knowledge = Bag.RANDOM;
				System.out.println("\nMDB, random-replication-only-largestExpectedRt");

				violateMDB = mdb.executeTailMultipleClusters(executionSeed, 
						configMDB,
						0);
				randomReplicationFixes = violateMDB;
				/*System.out.println(violateMDB);
				System.out.println(mdb.overMnodes);*/
				
				System.out.println("\nMDB, no tail optimization");

				violateMDB = mdb.executeMultipleClusters(executionSeed, 
						configMDB);

				/*System.out.println(violateMDB);
				System.out.println(mdb.overMnodes);*/

				if(violateMDB > 0) {
					violateMDBCounter ++;
					if(replicationFixes <= 0) {
						replicationFixesMDBCounter ++;
					}					
					if(migrationFixes <= 0) {
						migrationFixesMDBCounter ++;
					}
					if(perfectReplicationFixes <= 0) {
						perfectReplicationFixesMDBCounter ++;
					}
					if(randomReplicationFixes <= 0) {
						randomReplicationFixesMDBCounter ++;
					}
				}

				overMMDB.get("ER").add(replicationFixes);
				overMMDB.get("ERM").add(migrationFixes);
				overMMDB.get("PR").add(perfectReplicationFixes);
				overMMDB.get("RR").add(randomReplicationFixes);
				overMMDB.get("NOTO").add(violateMDB);
				
				
				LevyTruncatedDistributedBag ltb = new LevyTruncatedDistributedBag(1000, 
						experimentBagGeneratingSeed,
						0.366, 2700,
						3, 4, 3);
						//4, 3, 3);
				
				ltb.useHisto = useHisto;
				
				ltb.printTailMap = false;
				
				double ltbExpectation = ltb.getExpectation();
				double ltbVariance = ltb.getVariance();

				System.out.println("LTB");

				clusterA = new SimulatedCluster(1,1,"A",24);
				clusterB = new SimulatedCluster(3,4,"B",24);
				clusterC = new SimulatedCluster(2,1,"C",24);
				clusters = new HashMap<String,SimulatedCluster>();
				clusters.put("A", clusterA);
				clusters.put("B", clusterB);
				clusters.put("C", clusterC);
				ltb.setClusters(clusters, sampleSeed, sampleSize);
				
				ltb.sampleMultipleClusters(sampleSeed, sampleSize);

				long BminLTB, BminPlus10LTB , BminPlus20LTB, 
				BmakespanMinLTB, BmakespanMinMinus10LTB, BmakespanMinMinus20LTB/**/;

				BminLTB=ltb.getMinimumBudgetMultipleClusters();			
				BminPlus10LTB = (long)Math.ceil((BminLTB*1.1));
				BminPlus20LTB = (long)Math.ceil((BminLTB*1.2));			

				BmakespanMinLTB = ltb.getMinimumMakespanBudgetMultipleClusters();
				BmakespanMinMinus10LTB = (long)Math.ceil(BmakespanMinLTB*0.9);
				BmakespanMinMinus20LTB = (long)Math.ceil(BmakespanMinLTB*0.8);
				
				//	System.out.print("B=" + BminPlus10);
				configLTB = ltb.computeMakespanEstimateMultipleClusters(BminPlus10LTB);
				System.out.println(configLTB.toString());
				/*computeMakespanWithSuccessRate(BminPlus10LTB, "", 0.9);*/
				
				
				double violateLTB;
								
				ltb.knowledge = Bag.ESTIMATES;
				System.out.println("\nLTB, estimates-replication-only-largestExpectedRt");

				violateLTB = ltb.executeTailMultipleClusters(executionSeed, 
						configLTB,
						0);
				replicationFixes = violateLTB;
								
				/*System.out.println(violateLTB);
				System.out.println(ltb.overMnodes);*/


				System.out.println("\nLTB, estimates-replication-migration-largestExpectedRt");
				ltb.emergencyOnly = false;
				violateLTB = ltb.executeTailMultipleClusters(executionSeed, 
						configLTB,
						sampleSize);
				migrationFixes = violateLTB;
				
				/*System.out.println(violateLTB);
				System.out.println(ltb.overMnodes);*/
				
				ltb.knowledge = Bag.PERFECT;
				System.out.println("\nLTB, perfect-replication-only-largestExpectedRt");

				violateLTB = ltb.executeTailMultipleClusters(executionSeed, 
						configLTB,
						0);
				perfectReplicationFixes = violateLTB;
								
				/*System.out.println(violateLTB);
				System.out.println(ltb.overMnodes);*/

				
				ltb.knowledge = Bag.RANDOM;
				System.out.println("\nLTB, random-replication-only-largestExpectedRt");

				violateLTB = ltb.executeTailMultipleClusters(executionSeed, 
						configLTB,
						0);
				randomReplicationFixes = violateLTB;
								
				/*System.out.println(violateLTB);
				System.out.println(ltb.overMnodes);*/

				
				
				System.out.println("\nLTB, no tail optimization");

				violateLTB = ltb.executeMultipleClusters(executionSeed, 
						configLTB);
								
				/*System.out.println(violateLTB);
				System.out.println(ltb.overMnodes);*/

				if(violateLTB > 0) {
					violateLTBCounter ++;
					if(replicationFixes <= 0) {
						replicationFixesLTBCounter ++;
					}					
					if(migrationFixes <= 0) {
						migrationFixesLTBCounter ++;
					}
					if(perfectReplicationFixes <= 0) {
						perfectReplicationFixesLTBCounter ++;
					}
					if(randomReplicationFixes <= 0) {
						randomReplicationFixesLTBCounter ++;
					}
				}
				
				overMLTB.get("ER").add(replicationFixes);
				overMLTB.get("ERM").add(migrationFixes);
				overMLTB.get("PR").add(perfectReplicationFixes);
				overMLTB.get("RR").add(randomReplicationFixes);
				overMLTB.get("NOTO").add(violateLTB);
				
				
				LevyTruncatedDistributedBag ltbSameVar = 
					new LevyTruncatedDistributedBag(1000,							
							experimentBagGeneratingSeed,
							0.366, 2700,
							Math.sqrt(desiredVar*3600/ltbVariance),
							ltbExpectation*(1-Math.sqrt(desiredVar*3600/ltbVariance)),
							3, 4, 3);
							//4, 3, 3);
				
				ltbSameVar.useHisto = useHisto;
				
				ltbSameVar.getExpectation();
				ltbSameVar.getVariance();

				System.out.println("LTBSameVar");

				clusterA = new SimulatedCluster(1,1,"A",24);
				clusterB = new SimulatedCluster(3,4,"B",24);
				clusterC = new SimulatedCluster(2,1,"C",24);
				clusters = new HashMap<String,SimulatedCluster>();
				clusters.put("A", clusterA);
				clusters.put("B", clusterB);
				clusters.put("C", clusterC);
				ltbSameVar.setClusters(clusters, sampleSeed, sampleSize);
				
				ltbSameVar.sampleMultipleClusters(sampleSeed, sampleSize);

				long BminLTBSameVar, BminPlus10LTBSameVar , BminPlus20LTBSameVar, 
				BmakespanMinLTBSameVar, BmakespanMinMinus10LTBSameVar, BmakespanMinMinus20LTBSameVar/**/;

				BminLTBSameVar=ltbSameVar.getMinimumBudgetMultipleClusters();	
				BminPlus10LTBSameVar = (long)Math.ceil(BminLTBSameVar*1.1);
				BminPlus20LTBSameVar = (long)Math.ceil(BminLTBSameVar*1.2);			

				BmakespanMinLTBSameVar = ltbSameVar.getMinimumMakespanBudgetMultipleClusters();
				BmakespanMinMinus10LTBSameVar = (long)Math.ceil(BmakespanMinLTBSameVar*0.9);
				BmakespanMinMinus20LTBSameVar = (long)Math.ceil(BmakespanMinLTBSameVar*0.8);
				
				//	System.out.print("B=" + BminPlus10);
				configLTBSameVar = ltbSameVar.computeMakespanEstimateMultipleClusters(BminPlus10LTBSameVar);
				System.out.println(configLTBSameVar.toString());
				/*computeMakespanWithSuccessRate(BminPlus10LTBSameVar, "", 0.9);*/
				
				
				double violateLTBSameVar; 
				
				ltbSameVar.knowledge = Bag.ESTIMATES;
				System.out.println("\nLTBSameVar, estimates-replication-only-largestExpectedRt");

				violateLTBSameVar = ltbSameVar.executeTailMultipleClusters(executionSeed, 
						configLTBSameVar,
						0);
				replicationFixes = violateLTBSameVar;				
				/*System.out.println(violateLTBSameVar);
				System.out.println(ltbSameVar.overMnodes);*/
						
				System.out.println("\nLTBSameVar, estimates-replication-migration-largestExpectedRt");
				ltbSameVar.emergencyOnly = false;
				violateLTBSameVar = ltbSameVar.executeTailMultipleClusters(executionSeed, 
						configLTBSameVar,
						sampleSize);
				migrationFixes = violateLTBSameVar; 				
				/*System.out.println(violateLTBSameVar);
				System.out.println(ltbSameVar.overMnodes);*/

				ltbSameVar.knowledge = Bag.PERFECT;
				System.out.println("\nLTBSameVar, perfect-replication-only-largestExpectedRt");

				violateLTBSameVar = ltbSameVar.executeTailMultipleClusters(executionSeed, 
						configLTBSameVar,
						0);
				perfectReplicationFixes = violateLTBSameVar;				
				/*System.out.println(violateLTBSameVar);
				System.out.println(ltbSameVar.overMnodes);*/
				
				ltbSameVar.knowledge = Bag.RANDOM;
				System.out.println("\nLTBSameVar, random-replication-only-largestExpectedRt");

				violateLTBSameVar = ltbSameVar.executeTailMultipleClusters(executionSeed, 
						configLTBSameVar,
						0);
				randomReplicationFixes = violateLTBSameVar;				
				/*System.out.println(violateLTBSameVar);
				System.out.println(ltbSameVar.overMnodes);*/
				
				
				System.out.println("\nLTBSameVar, no tail optimization");

				violateLTBSameVar = ltbSameVar.executeMultipleClusters(executionSeed, 
						configLTBSameVar);
				
				/*System.out.println(violateLTBSameVar);
				System.out.println(ltbSameVar.overMnodes);*/
				
				if(violateLTBSameVar > 0) {
					violateLTBSameVarCounter ++;
					if(replicationFixes <= 0) {
						replicationFixesLTBSameVarCounter ++;
					}					
					if(migrationFixes <= 0) {
						migrationFixesLTBSameVarCounter ++;
					}
					if(perfectReplicationFixes <= 0) {
						perfectReplicationFixesLTBSameVarCounter ++;
					}
					if(randomReplicationFixes <= 0) {
						randomReplicationFixesLTBSameVarCounter ++;
					}
				}
				
				overMLTBSameVar.get("ER").add(replicationFixes);
				overMLTBSameVar.get("ERM").add(migrationFixes);
				overMLTBSameVar.get("PR").add(perfectReplicationFixes);
				overMLTBSameVar.get("RR").add(randomReplicationFixes);
				overMLTBSameVar.get("NOTO").add(violateLTBSameVar);
				
			}
			
			double perNDBER = 0.0 , perNDBERM = 0.0, perNDBPR = 0.0, perNDBRR = 0.0, perNDBNOTO = 0.0, makespanNDBNOTO = 0.0,
					estimatedMakespanNDB;
			double perMDBER = 0.0 , perMDBERM = 0.0, perMDBPR = 0.0, perMDBRR = 0.0, perMDBNOTO =0.0, makespanMDBNOTO = 0.0,
					estimatedMakespanMDB;
			double perLTBER = 0.0 , perLTBERM = 0.0, perLTBPR = 0.0, perLTBRR = 0.0, perLTBNOTO =0.0, makespanLTBNOTO = 0.0,
					estimatedMakespanLTB;
			double perLTBSameVarER = 0.0 , perLTBSameVarERM = 0.0, perLTBSameVarPR = 0.0, perLTBSameVarRR = 0.0, 
					perLTBSameVarNOTO = 0.0, 
					makespanLTBSameVarNOTO = 0.0,
							estimatedMakespanLTBSameVar;
			
			if(printPerBag) {
			System.out.println("NDB \t\t\t\t\t\t MDB \t\t\t\t\t\t LTB \t\t\t\t\t\t LTBSameVar");
			System.out.println("ER \t ERM \t PR \t RR \t NOTO \t\t\t ER \t ERM \t PR \t RR \t NOTO " +
					"\t\t\t ER \t ERM \t PR \t RR \t NOTO \t\t\t ER \t ERM \t PR \t RR \t NOTO");		
			}
			
			for(int k = 0; k < noExecutions; k++) {
				if(printPerBag ) {
				System.out.println(overMNDB.get("ER").get(k) + "\t" +
								   overMNDB.get("ERM").get(k) + "\t" +
								   overMNDB.get("PR").get(k) + "\t" +
								   overMNDB.get("RR").get(k) + "\t" +
								   overMNDB.get("NOTO").get(k) + "\t\t\t" + 
								   
								   overMMDB.get("ER").get(k) + "\t" +
								   overMMDB.get("ERM").get(k) + "\t" +
								   overMMDB.get("PR").get(k) + "\t" +
								   overMMDB.get("RR").get(k) + "\t" +
								   overMMDB.get("NOTO").get(k) + "\t\t\t" +
								   
								   overMLTB.get("ER").get(k) + "\t" +
								   overMLTB.get("ERM").get(k) + "\t" +
								   overMLTB.get("PR").get(k) + "\t" +
								   overMLTB.get("RR").get(k) + "\t" +
								   overMLTB.get("NOTO").get(k) + "\t\t\t" +
								   
								   overMLTBSameVar.get("ER").get(k) + "\t" +
								   overMLTBSameVar.get("ERM").get(k) + "\t" +
								   overMLTBSameVar.get("PR").get(k) + "\t" +
								   overMLTBSameVar.get("RR").get(k) + "\t" +
								   overMLTBSameVar.get("NOTO").get(k));
				}
				
				estimatedMakespanNDB = configNDB.atus*60;
				makespanNDBNOTO = estimatedMakespanNDB + overMNDB.get("NOTO").get(k);
				perNDBER += (estimatedMakespanNDB + overMNDB.get("ER").get(k))/estimatedMakespanNDB*100;
				perNDBERM += (estimatedMakespanNDB + overMNDB.get("ERM").get(k))/estimatedMakespanNDB*100;
				perNDBPR += (estimatedMakespanNDB + overMNDB.get("PR").get(k))/estimatedMakespanNDB*100; 
				perNDBRR += (estimatedMakespanNDB + overMNDB.get("RR").get(k))/estimatedMakespanNDB*100;
				perNDBNOTO += makespanNDBNOTO / estimatedMakespanNDB * 100;
				
				estimatedMakespanMDB = configMDB.atus*60;
				makespanMDBNOTO = estimatedMakespanMDB + overMMDB.get("NOTO").get(k);
				perMDBER += (estimatedMakespanMDB + overMMDB.get("ER").get(k))/estimatedMakespanMDB*100;
				perMDBERM += (estimatedMakespanMDB + overMMDB.get("ERM").get(k))/estimatedMakespanMDB*100;
				perMDBPR += (estimatedMakespanMDB + overMMDB.get("PR").get(k))/estimatedMakespanMDB*100; 
				perMDBRR += (estimatedMakespanMDB + overMMDB.get("RR").get(k))/estimatedMakespanMDB*100;
				perMDBNOTO += makespanMDBNOTO / estimatedMakespanMDB * 100;
				
				estimatedMakespanLTB = configLTB.atus*60;
				makespanLTBNOTO = estimatedMakespanLTB + overMLTB.get("NOTO").get(k);
				perLTBER += (estimatedMakespanLTB + overMLTB.get("ER").get(k))/estimatedMakespanLTB*100;
				perLTBERM += (estimatedMakespanLTB + overMLTB.get("ERM").get(k))/estimatedMakespanLTB*100;
				perLTBPR += (estimatedMakespanLTB + overMLTB.get("PR").get(k))/estimatedMakespanLTB*100; 
				perLTBRR += (estimatedMakespanLTB + overMLTB.get("RR").get(k))/estimatedMakespanLTB*100;
				perLTBNOTO += makespanLTBNOTO / estimatedMakespanLTB * 100;
				
				estimatedMakespanLTBSameVar = configLTBSameVar.atus*60;
				makespanLTBSameVarNOTO = estimatedMakespanLTBSameVar + overMLTBSameVar.get("NOTO").get(k);
				perLTBSameVarER += (estimatedMakespanLTBSameVar + overMLTBSameVar.get("ER").get(k))/estimatedMakespanLTBSameVar*100;
				perLTBSameVarERM += (estimatedMakespanLTBSameVar + overMLTBSameVar.get("ERM").get(k))/estimatedMakespanLTBSameVar*100;
				perLTBSameVarPR += (estimatedMakespanLTBSameVar + overMLTBSameVar.get("PR").get(k))/estimatedMakespanLTBSameVar*100; 
				perLTBSameVarRR += (estimatedMakespanLTBSameVar + overMLTBSameVar.get("RR").get(k))/estimatedMakespanLTBSameVar*100;
				perLTBSameVarNOTO += makespanLTBSameVarNOTO / estimatedMakespanLTBSameVar * 100;				
				
			}
			
			avgPercentageNDB.get("ER").add(perNDBER/noExecutions);
			avgPercentageNDB.get("ERM").add(perNDBERM/noExecutions);
			avgPercentageNDB.get("PR").add(perNDBPR/noExecutions);
			avgPercentageNDB.get("RR").add(perNDBRR/noExecutions);
			avgPercentageNDB.get("NOTO").add(perNDBNOTO/noExecutions);
			
			avgPercentageMDB.get("ER").add(perMDBER/noExecutions);
			avgPercentageMDB.get("ERM").add(perMDBERM/noExecutions);
			avgPercentageMDB.get("PR").add(perMDBPR/noExecutions);
			avgPercentageMDB.get("RR").add(perMDBRR/noExecutions);
			avgPercentageMDB.get("NOTO").add(perMDBNOTO/noExecutions);
			
			avgPercentageLTB.get("ER").add(perLTBER/noExecutions);
			avgPercentageLTB.get("ERM").add(perLTBERM/noExecutions);
			avgPercentageLTB.get("PR").add(perLTBPR/noExecutions);
			avgPercentageLTB.get("RR").add(perLTBRR/noExecutions);
			avgPercentageLTB.get("NOTO").add(perLTBNOTO/noExecutions);
			
			avgPercentageLTBSameVar.get("ER").add(perLTBSameVarER/noExecutions);
			avgPercentageLTBSameVar.get("ERM").add(perLTBSameVarERM/noExecutions);
			avgPercentageLTBSameVar.get("PR").add(perLTBSameVarPR/noExecutions);
			avgPercentageLTBSameVar.get("RR").add(perLTBSameVarRR/noExecutions);
			avgPercentageLTBSameVar.get("NOTO").add(perLTBSameVarNOTO/noExecutions);
			
			if(printPerBag) {
			System.out.println("Violations NDB: " + (double)violateNDBCounter/(double)noExecutions);
			System.out.println("Violations MDB: " + (double)violateMDBCounter/(double)noExecutions);
			System.out.println("Violations LTB: " + (double)violateLTBCounter/(double)noExecutions);
			System.out.println("Violations LTB Same Var: " + (double)violateLTBSameVarCounter/(double)noExecutions);
			
			System.out.println("TO Replication fixes NDB: " + (double)replicationFixesNDBCounter/(double)violateNDBCounter);			
			System.out.println("TO Migration fixes NDB: " + (double)migrationFixesNDBCounter/(double)violateNDBCounter);
			System.out.println("Perfect Replication fixes NDB: " + (double)perfectReplicationFixesNDBCounter/(double)violateNDBCounter);
			System.out.println("Random Replication fixes NDB: " + (double)randomReplicationFixesNDBCounter/(double)violateNDBCounter);
			
			System.out.println("TO Replication fixes MDB: " + (double)replicationFixesMDBCounter/(double)violateMDBCounter);	
			System.out.println("TO Migration fixes MDB: " + (double)migrationFixesMDBCounter/(double)violateMDBCounter);
			System.out.println("Perfect Replication fixes MDB: " + (double)perfectReplicationFixesMDBCounter/(double)violateMDBCounter);
			System.out.println("Random Replication fixes MDB: " + (double)randomReplicationFixesMDBCounter/(double)violateMDBCounter);
			
			System.out.println("TO Replication fixes LTB: " + (double)replicationFixesLTBCounter/(double)violateLTBCounter);			
			System.out.println("TO Migration fixes LTB: " + (double)migrationFixesLTBCounter/(double)violateLTBCounter);
			System.out.println("Perfect Replication LTB: " + (double)perfectReplicationFixesLTBCounter/(double)violateLTBCounter);
			System.out.println("Random Replication LTB: " + (double)randomReplicationFixesLTBCounter/(double)violateLTBCounter);
			
			System.out.println("TO Replication fixes LTBSameVar: " + (double)replicationFixesLTBSameVarCounter/(double)violateLTBSameVarCounter);			
			System.out.println("TO Migration fixes LTBSameVar: " + (double)migrationFixesLTBSameVarCounter/(double)violateLTBSameVarCounter);
			System.out.println("Perfect Replication LTBSameVar: " + (double)perfectReplicationFixesLTBSameVarCounter/(double)violateLTBSameVarCounter);
			System.out.println("Random Replication LTBSameVar: " + (double)randomReplicationFixesLTBSameVarCounter/(double)violateLTBSameVarCounter);
			}
		}
		
		System.out.println("Aggregated results over all bags and all executions of each bag.\n" +
				"Results are presented as averages over the average percentage of each bag," +
				"where for each strategy the percentage is computed out of the estimated makespan");
		System.out.println("NDB \t\t\t\t\t\t MDB \t\t\t\t\t\t LTB \t\t\t\t\t\t LTBSameVar");
		System.out.println("ER \t ERM \t PR \t RR \t NOTO%EstM \t\t\t ER \t ERM \t PR \t RR \t NOTO%EstM " +
				"\t\t\t ER \t ERM \t PR \t RR \t NOTO%EstM \t\t\t ER \t ERM \t PR \t RR \t NOTO%EstM");		
		
		for(int k = 0; k < noExperiments; k++) {
			
			System.out.println(avgPercentageNDB.get("ER").get(k) + "\t" +
					avgPercentageNDB.get("ERM").get(k) + "\t" +
					avgPercentageNDB.get("PR").get(k) + "\t" +
					avgPercentageNDB.get("RR").get(k) + "\t" +
					avgPercentageNDB.get("NOTO").get(k) + "\t\t\t" + 
							   
							   avgPercentageMDB.get("ER").get(k) + "\t" +
							   avgPercentageMDB.get("ERM").get(k) + "\t" +
							   avgPercentageMDB.get("PR").get(k) + "\t" +
							   avgPercentageMDB.get("RR").get(k) + "\t" +
							   avgPercentageMDB.get("NOTO").get(k) + "\t\t\t" +
							   
							   avgPercentageLTB.get("ER").get(k) + "\t" +
							   avgPercentageLTB.get("ERM").get(k) + "\t" +
							   avgPercentageLTB.get("PR").get(k) + "\t" +
							   avgPercentageLTB.get("RR").get(k) + "\t" +
							   avgPercentageLTB.get("NOTO").get(k) + "\t\t\t" +
							   
							   avgPercentageLTBSameVar.get("ER").get(k) + "\t" +
							   avgPercentageLTBSameVar.get("ERM").get(k) + "\t" +
							   avgPercentageLTBSameVar.get("PR").get(k) + "\t" +
							   avgPercentageLTBSameVar.get("RR").get(k) + "\t" +
							   avgPercentageLTBSameVar.get("NOTO").get(k));
		}
	}
	
	public void testSamplePhaseMultipleClusters(String budgetType) {
		noExperiments = 1;
		int noSampleExperiments = 30;
		noExecutions = 30;

		boolean printXLS = false;
		boolean printSampleSummary = false;
		boolean printNodeStats = true;
		
		
		SimulatedCluster clusterA, clusterB, clusterC;
		HashMap<String,SimulatedCluster> clusters;
				
		Random randomizedSeed = new Random(99999999L);
		Random sampleSeeder = new Random(111111111L);
		Random executionSeeder = new Random(888888888L);
		
		long sampleSeed, executionSeed;
		
		
		long experimentBagGeneratingSeed;
		

		double p_mu = 0.9, p_sigma = 0.9, p_M = 0.9;

		int sampleSize = 30; 
		
		/*
		String Bmin = "Bmin"; 
		String BminPlus10 = "BminPlus10";
		String BminPlus20 = "BminPlus20";
		String BmakespanMin = "BmakespanMin"; 
		String BmakespanMinMinus10 = "BmakespanMinMinus10"; 
		String BmakespanMinMinus20 = "BmakespanMinMinus20";
		String budgetType = BminPlus10;
		*/	
		
		double desiredStDev = Math.sqrt(5);
		double desiredVar = desiredStDev * desiredStDev;
		
		boolean printInfo = false;
				
		experimentBagGeneratingSeed = randomizedSeed.nextLong();
		
		NormalDistributedBag ndb = new NormalDistributedBag(1000, 
				experimentBagGeneratingSeed,
				15*60, desiredStDev*60,
				4, 3, 3);
		ndb.printXLS = printXLS;
		ndb.printSampleSummary = printSampleSummary;
		ndb.printNodeStats = printNodeStats;
		
		clusterA = new SimulatedCluster(1,1,"A",24);
		clusterB = new SimulatedCluster(3,4,"B",24);
		clusterC = new SimulatedCluster(2,1,"C",24);
		clusters = new HashMap<String,SimulatedCluster>();
		clusters.put("A", clusterA);
		clusters.put("B", clusterB);
		clusters.put("C", clusterC);
		
		ndb.clusters = clusters;
		
		SampleStatsMultipleClusters ndbSS = new SampleStatsMultipleClusters(ndb, sampleSize, p_mu, p_sigma, p_M);
		ndbSS.selectBudget(budgetType);
		System.out.println(ndbSS.scheduleRealParameters.machinesPerCluster);
		
		
		
		for(int j=0; j < noSampleExperiments; j++) {		
			
			sampleSeed = sampleSeeder.nextLong();	
			
			System.out.println("----------->Sample Experiment " + j + "<-----------");
			System.out.println("Sample seed: " + sampleSeed);
					

			NormalDistributedBag tmpNDB = (NormalDistributedBag) ndb.copyMC();								
			tmpNDB.sampleMultipleClusters(sampleSeed, sampleSize);						
			ndbSS.addSampleStats(tmpNDB);
			
			long budgetSampleParameters = tmpNDB.selectedBudget(budgetType);
					
			SimulatedSchedule simSched = tmpNDB.computeMakespanEstimateMultipleClusters(budgetSampleParameters);
			System.out.println(simSched);
			
			ndbSS.addScheduleStats(simSched, tmpNDB);
			
			SimulatedSchedule simSchedDeltaB = tmpNDB.computeExtraBudgetMakespanEstimateMultipleClusters(budgetSampleParameters, 
					budgetType);
			System.out.println(simSchedDeltaB);
			
			ndbSS.addExtraBudgetScheduleStats(simSchedDeltaB, tmpNDB);
			
			if(printSampleSummary) {
				System.out.println();
			}
			
			
			for (int i=0; i < noExecutions; i++) {	
				
				System.out.println("---------->>Execution " + j + "." + i + "<<--------");
				
				executionSeed = executionSeeder.nextLong();
				
				
				SimulatedExecution simExec = tmpNDB.executeMultipleClustersMoreDetails(executionSeed, simSched);
				ndbSS.addExecutionStats(j, simExec);
				
				SimulatedExecution simExecExtraB = tmpNDB.executeMultipleClustersMoreDetails(executionSeed, simSchedDeltaB);
				ndbSS.addExtraBudgetExecutionStats(j, simExecExtraB);
						
			}			
		}		
		
		ndbSS.printFormatted("NDB", true);
		
		
	}
	
	public void testSamplePhaseMultipleClusters(String budgetType, Bag bag) {		
		
		boolean printXLS = false;
		boolean printSampleSummary = false;
		boolean printNodeStats = true;
		
		
		SimulatedCluster clusterA, clusterB, clusterC;
		HashMap<String,SimulatedCluster> clusters;
				
		Random sampleSeeder = new Random(111111111L);
		Random executionSeeder = new Random(888888888L);
		
		long sampleSeed, executionSeed;
		
		double p_mu = 0.9, p_sigma = 0.9, p_M = 0.9;

		int sampleSize = bag.sampleSize;
								
		boolean printInfo = false;
				
		
				
		bag.printXLS = printXLS;
		bag.printSampleSummary = printSampleSummary;
		bag.printNodeStats = printNodeStats;
		
		clusterA = new SimulatedCluster(1,1,"A",24);
		clusterB = new SimulatedCluster(3,4,"B",24);
		clusterC = new SimulatedCluster(2,1,"C",24);
		clusters = new HashMap<String,SimulatedCluster>();
		clusters.put("A", clusterA);
		clusters.put("B", clusterB);
		clusters.put("C", clusterC);
		
		bag.clusters = clusters;
		
		SampleStatsMultipleClusters bagSS = new SampleStatsMultipleClusters(bag, sampleSize, p_mu, p_sigma, p_M);
		bagSS.selectBudget(budgetType);
		System.out.println("Schedule based on real parameters: \t" + bagSS.scheduleRealParameters.machinesPerCluster);
		System.out.println("Schedule based on real parameters + deltaB: \t" + 
					bagSS.scheduleRealParametersExtraBudget.machinesPerCluster + 
					"\t initialDeltaN=" + bagSS.scheduleRealParametersExtraBudget.deltaN +
					"\t deltaNExtraB=" + bagSS.scheduleRealParametersExtraBudget.deltaNExtraB);
		
		
		for(int j=0; j < noSampleExperiments; j++) {		
			
			sampleSeed = sampleSeeder.nextLong();	
			
			System.out.println("----------->Sample Experiment " + j + "<-----------");
			System.out.println("Sample seed: " + sampleSeed);
					

			Bag tmpBag = bag.copyMC();								
			tmpBag.sampleMultipleClusters(sampleSeed, sampleSize);						
			bagSS.addSampleStats(tmpBag);
			
			long budgetSampleParameters = tmpBag.selectedBudget(budgetType);
					
			SimulatedSchedule simSched = tmpBag.computeMakespanEstimateMultipleClusters(budgetSampleParameters);
			System.out.println(simSched);
			
			bagSS.addScheduleStats(simSched, tmpBag);
			
			SimulatedSchedule simSchedDeltaB = tmpBag.computeExtraBudgetMakespanEstimateMultipleClusters(budgetSampleParameters, 
					budgetType);
			System.out.println(simSchedDeltaB);
			
			bagSS.addExtraBudgetScheduleStats(simSchedDeltaB, tmpBag);
			
			if(printSampleSummary) {
				System.out.println();
			}
			
			for (int i=0; i < noExecutions; i++) {	
				
				System.out.println("---------->>Execution " + j + "." + i + "<<--------");
				
				executionSeed = executionSeeder.nextLong();
				
				
				SimulatedExecution simExec = tmpBag.executeMultipleClustersMoreDetails(executionSeed, simSched);
				bagSS.addExecutionStats(j, simExec);

				SimulatedExecution simExecExtraB = tmpBag.executeMultipleClustersMoreDetails(executionSeed, simSchedDeltaB);
				bagSS.addExtraBudgetExecutionStats(j, simExecExtraB);
						
			}			
		}		
		
		bagSS.printFormatted("", true);
				
	}

	public void test1() {
		
		Random randomizedSeed = new Random(99999999L);
		Random sampleSeeder = new Random(111111111L);		
		long sampleSeed = sampleSeeder.nextLong();		
		Random executionSeeder = new Random(888888888L);
		
		
		int sampleSize = 30; 
		boolean printXLS = true;
		boolean printNodeStats = true;
		
		double p_mu=0.9, p_sigma=0.9, p_m=0.9;
		
		for(int j=0; j < noExperiments; j++) {
			
			long experimentBagGeneratingSeed = randomizedSeed.nextLong();
			int successfulPrediction = 0;
			
			for (int i=0; i < noExecutions; i++) {	

				LevyTruncatedDistributedBag ltb = new LevyTruncatedDistributedBag(1000, 
						experimentBagGeneratingSeed,
						0.366, 2700,
						4, 3, 3);
				ltb.printXLS = printXLS;
				ltb.printNodeStats = printNodeStats;
				
				ltb.sample(sampleSeed, sampleSize);
						
				
				long Bmin, BminPlus10, BminPlus20, 
				BmakespanMin, BmakespanMinMinus10, BmakespanMinMinus20;
				
				Bmin=ltb.getMinimumBudget();		
				BminPlus10 = (long)Math.ceil((Bmin*1.1));			
				
			//	System.out.print("B=" + BminPlus10);
				ArrayList<Integer> config = ltb.computeMakespanWithCI(BminPlus10, p_mu, p_sigma, p_m);
				/*ltb.computeMakespanWithSuccessRate(BminPlus10, "", 0.9);*/
				double violate = ltb.execute(executionSeeder.nextLong(), 
						config.get(0).intValue(), 
						config.get(1).intValue(), 
						config.get(2).intValue());
				
			//	System.out.print("\t" + config.get(0) + "\t" + config.get(1) + "\t" + (config.get(2) == 0 ? "false" : "true") + "\t" + violate);
			
				
				
				if(((config.get(config.size()-1).intValue()==1) && (violate > 0)) || 
				   ((config.get(config.size()-1).intValue()==0) && (violate <= 0))	) {
					successfulPrediction ++;
				} 
				
				// System.out.println();
			}
			
			System.out.println((double)successfulPrediction/noExecutions);
		}
	}
	
	
	public void test2() {
		Random randomizedSeed = new Random(99999999L);
		Random sampleSeeder = new Random(111111111L);				
		Random executionSeeder = new Random(888888888L);
		
		
		int sampleSize = 30; 
		boolean printXLS = false;
		boolean printNodeStats = false;
		
		double p_mu=0.9, p_sigma=0.9, p_m = 0.9;
		
		for(int j=0; j < noExperiments; j++) {
			
			long experimentBagGeneratingSeed = randomizedSeed.nextLong();
			int successfulPrediction = 0;
			
			for (int i=0; i < noExecutions; i++) {	

				LevyTruncatedDistributedBag ltb = new LevyTruncatedDistributedBag(1000, 
						experimentBagGeneratingSeed,
						0.366, 2700,
						4, 3, 3);
				ltb.printXLS = printXLS;
				ltb.printNodeStats = printNodeStats;
				
				long sampleSeed = sampleSeeder.nextLong();
				ltb.sample(sampleSeed, sampleSize);
						
				
				long Bmin, BminPlus10, BminPlus20, 
				BmakespanMin, BmakespanMinMinus10, BmakespanMinMinus20;
				
				Bmin=ltb.getMinimumBudget();		
				BminPlus10 = (long)Math.ceil((Bmin*1.1));	
				BmakespanMin = ltb.getMinimumMakespanBudget();
				
			//	System.out.print("B=" + BminPlus10);
				ArrayList<Integer> config = ltb.computeMakespanWithCI(BminPlus10, p_mu, p_sigma, p_m);
				/*computeMakespanWithSuccessRate(BmakespanMin, "", 0.9);*/
				double violate = ltb.execute(executionSeeder.nextLong(), 
						config.get(0).intValue(), 
						config.get(1).intValue(), 
						config.get(2).intValue());
				
			//	System.out.print("\t" + config.get(0) + "\t" + config.get(1) + "\t" + (config.get(2) == 0 ? "false" : "true") + "\t" + violate);
			
				
				
				if(((config.get(config.size()-1)==1) && (violate > 0)) || 
				   ((config.get(config.size()-1)==0) && (violate <= 0))	) {
					successfulPrediction ++;
				} 
				
				// System.out.println();
			}
			
			System.out.println((double)successfulPrediction/noExecutions);
		}
	}
	
	public void test1AllD() {
		
		noExperiments = 10;
		noExecutions = 100;
		
		Random randomizedSeed = new Random(99999999L);
		Random sampleSeeder = new Random(111111111L);		
		long sampleSeed = sampleSeeder.nextLong();		
		Random executionSeeder = new Random(888888888L);
		
		
		int sampleSize = 30; 
		boolean printXLS = false;
		boolean printSampleSummary = false;
		boolean printNodeStats = false;
		
		double p_mu = 0.9, p_sigma = 0.9, p_m = 0.9;
		
		for(int j=0; j < noExperiments; j++) {
		
			ArrayList<Double> histoLTB  = new ArrayList<Double>();
			ArrayList<Double> histoNDB  = new ArrayList<Double>();
			ArrayList<Double> histoSDB  = new ArrayList<Double>();
			
			ArrayList<Double> histoLTBSmallVar  = new ArrayList<Double>();
			ArrayList<Double> histoLTBSameVar  = new ArrayList<Double>();
			
			
			long experimentBagGeneratingSeed = randomizedSeed.nextLong();
			
			/*true positive prediction*/
			int tpPredictionLTB = 0;
			int tpPredictionNDB = 0;
			int tpPredictionSDB = 0;
			
			int tpPredictionLTBSmallVar = 0;
			int tpPredictionLTBSameVar = 0;
			
			/*true negative prediction*/
			int tnPredictionLTB = 0;
			int tnPredictionNDB = 0;
			int tnPredictionSDB = 0;
			
			int tnPredictionLTBSmallVar = 0;
			int tnPredictionLTBSameVar = 0;
							
			/*fake positive prediction == prediction to meet schedule, but it doesn't*/
			int fpPredictionLTB = 0;
			int fpPredictionNDB = 0;
			int fpPredictionSDB = 0;
			
			int fpPredictionLTBSmallVar = 0;
			int fpPredictionLTBSameVar = 0;
			
			/*fake negative prediction == prediction not to meet schedule, but it does*/
			int fnPredictionLTB = 0;
			int fnPredictionNDB = 0;
			int fnPredictionSDB = 0;
			
			int fnPredictionLTBSmallVar = 0;
			int fnPredictionLTBSameVar = 0;
			
			/*average number of machines going over the predicted Makespan*/			
			double avgOverMnodesLTB = 0;
			double avgOverMnodesNDB = 0;
			double avgOverMnodesSDB = 0;
			
			double avgOverMnodesLTBSmallVar = 0;
			double avgOverMnodesLTBSameVar = 0;
					
			for (int i=0; i < noExecutions; i++) {	

				if(i==0) {
					printSampleSummary = true;
				} else {
					printSampleSummary = false;
				}
				
				long executionSeed = executionSeeder.nextLong();
				
				double desiredStDev = 5;
				
				NormalDistributedBag ndb = new NormalDistributedBag(1000, 
						experimentBagGeneratingSeed,
						15*60, desiredStDev*60,
						4, 3, 3);
				ndb.printXLS = printXLS;
				ndb.printSampleSummary = printSampleSummary;
				ndb.printNodeStats = printNodeStats;
				
				ndb.getExpectation();
				ndb.getVariance();
				
				ndb.sample(sampleSeed, sampleSize);
				
				long BminNDB, BminPlus10NDB /*, BminPlus20, 
				BmakespanMin, BmakespanMinMinus10, BmakespanMinMinus20*/;
				
				BminNDB=ndb.getMinimumBudget();				
				BminPlus10NDB = (long)Math.ceil((BminNDB*1.1));			
				
			//	System.out.print("B=" + BminPlus10);
				ArrayList<Integer> configNDB = ndb.computeMakespanWithCI(BminPlus10NDB, 
						p_mu, p_sigma, p_m);
				/*computeMakespanWithSuccessRate(BminPlus10NDB, "", 0.9);*/
				double violateNDB = ndb.execute(executionSeed, 
						configNDB.get(0).intValue(), 
						configNDB.get(1).intValue(), 
						configNDB.get(2).intValue());
				
			//	System.out.print("\t" + config.get(0) + "\t" + config.get(1) + "\t" + (config.get(2) == 0 ? "false" : "true") + "\t" + violate);
				
				if((configNDB.get(configNDB.size()-1)==1) && (violateNDB > 0))
					tnPredictionNDB ++;
				if((configNDB.get(configNDB.size()-1)==0) && (violateNDB <= 0)) 
					tpPredictionNDB ++;
				 
				if((configNDB.get(configNDB.size()-1)==1) && (violateNDB <= 0))
					fnPredictionNDB ++;
				if((configNDB.get(configNDB.size()-1)==0) && (violateNDB > 0)) 
					fpPredictionNDB ++;
								
				avgOverMnodesNDB += (double)ndb.overMnodes/(configNDB.get(1).intValue() + configNDB.get(2).intValue());
				
				System.out.println(" " + (configNDB.get(configNDB.size()-1)==0) + " " + (violateNDB <= 0));
				
				histoNDB.add(violateNDB);
				
				if(printSampleSummary) {
					System.out.println();
				}
				
				StableDistributedBag sdb = new StableDistributedBag(1000, 
						experimentBagGeneratingSeed,
						15*60, desiredStDev*60, 1.5, 0,
						4, 3, 3);
				sdb.printXLS = printXLS;
				sdb.printSampleSummary = printSampleSummary;
				sdb.printNodeStats = printNodeStats;
				
				sdb.getExpectation();
				sdb.getVariance();
				
				sdb.sample(sampleSeed, sampleSize);
				
				long BminSDB, BminPlus10SDB /*, BminPlus20, 
				BmakespanMin, BmakespanMinMinus10, BmakespanMinMinus20*/;
				
				BminSDB=sdb.getMinimumBudget();				
				BminPlus10SDB = (long)Math.ceil((BminSDB*1.1));			
				
			//	System.out.print("B=" + BminPlus10);
				ArrayList<Integer> configSDB = sdb.computeMakespanWithCI(BminPlus10SDB, 
						p_mu, p_sigma, p_m);
				 /*computeMakespanWithSuccessRate(BminPlus10SDB, "", 0.9);*/
				double violateSDB = sdb.execute(executionSeed, 
						configSDB.get(0).intValue(), 
						configSDB.get(1).intValue(), 
						configSDB.get(2).intValue());
				
			//	System.out.print("\t" + config.get(0) + "\t" + config.get(1) + "\t" + (config.get(2) == 0 ? "false" : "true") + "\t" + violate);
				
				if((configSDB.get(configSDB.size()-1)==1) && (violateSDB > 0))
					tnPredictionSDB ++;
				if((configSDB.get(configSDB.size()-1)==0) && (violateSDB <= 0))
					tpPredictionSDB ++;
				
				if((configSDB.get(configSDB.size()-1)==1) && (violateSDB <= 0))
					fnPredictionSDB ++;
				if((configSDB.get(configSDB.size()-1)==0) && (violateSDB > 0))
					fpPredictionSDB ++;
			
				avgOverMnodesSDB += (double)sdb.overMnodes/(configSDB.get(1).intValue() + configSDB.get(2).intValue());
				
				histoSDB.add(violateSDB);
				
				if(printSampleSummary) {
					System.out.println();
				}
				
				LevyTruncatedDistributedBag ltb = new LevyTruncatedDistributedBag(1000, 
						experimentBagGeneratingSeed,
						0.366, 2700,
						4, 3, 3);
				ltb.printXLS = printXLS;
				ltb.printSampleSummary = printSampleSummary;
				ltb.printNodeStats = printNodeStats;
				
				double ltbExpectation = ltb.getExpectation();
				double ltbVariance = ltb.getVariance();
				
				ltb.sample(sampleSeed, sampleSize);
				
				long BminLTB, BminPlus10LTB /*, BminPlus20, 
				BmakespanMin, BmakespanMinMinus10, BmakespanMinMinus20*/;
				
				BminLTB=ltb.getMinimumBudget();				
				BminPlus10LTB = (long)Math.ceil((BminLTB*1.1));			
				
			//	System.out.print("B=" + BminPlus10);
				ArrayList<Integer> configLTB = ltb.computeMakespanWithCI(BminPlus10LTB,
						p_mu, p_sigma, p_m);
				 /*computeMakespanWithSuccessRate(BminPlus10LTB, "", 0.9);*/
				double violateLTB = ltb.execute(executionSeed, 
						configLTB.get(0).intValue(), 
						configLTB.get(1).intValue(), 
						configLTB.get(2).intValue());
				
			//	System.out.print("\t" + config.get(0) + "\t" + config.get(1) + "\t" + (config.get(2) == 0 ? "false" : "true") + "\t" + violate);
				
				if((configLTB.get(configLTB.size()-1)==1) && (violateLTB > 0)) 
					tnPredictionLTB ++;
				if((configLTB.get(configLTB.size()-1)==0) && (violateLTB <= 0))
					tpPredictionLTB ++;
				if((configLTB.get(configLTB.size()-1)==1) && (violateLTB <= 0)) 
					fnPredictionLTB ++;
				if((configLTB.get(configLTB.size()-1)==0) && (violateLTB > 0))
					fpPredictionLTB ++;
				
				avgOverMnodesLTB += (double)ltb.overMnodes/(configLTB.get(1).intValue() + configLTB.get(2).intValue());
				
				histoLTB.add(violateLTB);
				
				if(printSampleSummary) {
					System.out.println();
				}
				
				double desiredVar = desiredStDev * desiredStDev; 
				
				LevyTruncatedDistributedBag ltbSameVar = 
					new LevyTruncatedDistributedBag(1000,							
						experimentBagGeneratingSeed,
						0.366, 2700,
						Math.sqrt(desiredVar*3600/ltbVariance),
						ltbExpectation*(1-Math.sqrt(desiredVar*3600/ltbVariance)),
						4, 3, 3);
				ltbSameVar.printXLS = printXLS;
				ltbSameVar.printSampleSummary = printSampleSummary;
				ltbSameVar.printNodeStats = printNodeStats;
				
				ltbSameVar.getExpectation();
				ltbSameVar.getVariance();
				
				ltbSameVar.sample(sampleSeed, sampleSize);
				
				long BminLTBSameVar, BminPlus10LTBSameVar /*, BminPlus20, 
				BmakespanMin, BmakespanMinMinus10, BmakespanMinMinus20*/;
				
				BminLTBSameVar=ltbSameVar.getMinimumBudget();				
				BminPlus10LTBSameVar = (long)Math.ceil((BminLTBSameVar*1.1));			
				
			//	System.out.print("B=" + BminPlus10);
				ArrayList<Integer> configLTBSameVar = ltbSameVar.computeMakespanWithCI(BminPlus10LTBSameVar, 
						p_mu, p_sigma, p_m);
				 /*computeMakespanWithSuccessRate(BminPlus10LTBSameVar, "", 0.9);*/
				double violateLTBSameVar = ltbSameVar.execute(executionSeed, 
						configLTBSameVar.get(0).intValue(),
						configLTBSameVar.get(1).intValue(), 
						configLTBSameVar.get(2).intValue());
				
			//	System.out.print("\t" + config.get(0) + "\t" + config.get(1) + "\t" + (config.get(2) == 0 ? "false" : "true") + "\t" + violate);
				
				if((configLTBSameVar.get(configLTBSameVar.size()-1)==1) && (violateLTBSameVar > 0))
					tnPredictionLTBSameVar ++;
				if((configLTBSameVar.get(configLTBSameVar.size()-1)==0) && (violateLTBSameVar <= 0))
					tpPredictionLTBSameVar ++;
				if((configLTBSameVar.get(configLTBSameVar.size()-1)==1) && (violateLTBSameVar <= 0))
					fnPredictionLTBSameVar ++;
				if((configLTBSameVar.get(configLTBSameVar.size()-1)==0) && (violateLTBSameVar > 0))
					fpPredictionLTBSameVar ++;
				
				avgOverMnodesLTBSameVar += (double)ltbSameVar.overMnodes/(configLTBSameVar.get(1).intValue() + configLTBSameVar.get(2).intValue());
				
				histoLTBSameVar.add(violateLTBSameVar);
				
				if(printSampleSummary) {
					System.out.println();
				} 
								
				LevyTruncatedDistributedBag ltbSmallVar = 
					new LevyTruncatedDistributedBag(1000,							
						experimentBagGeneratingSeed,
						0.366, 2700,
						Math.sqrt(5*3600/ltbVariance),ltbExpectation*(1-Math.sqrt(5*3600/ltbVariance)),
						4, 3, 3);
				ltbSmallVar.printXLS = printXLS;
				ltbSmallVar.printSampleSummary = printSampleSummary;
				ltbSmallVar.printNodeStats = printNodeStats;
				
				ltbSmallVar.getExpectation();
				ltbSmallVar.getVariance();
				
				ltbSmallVar.sample(sampleSeed, sampleSize);
				
				long BminLTBSmallVar, BminPlus10LTBSmallVar /*, BminPlus20, 
				BmakespanMin, BmakespanMinMinus10, BmakespanMinMinus20*/;
				
				BminLTBSmallVar=ltbSmallVar.getMinimumBudget();				
				BminPlus10LTBSmallVar = (long)Math.ceil((BminLTBSmallVar*1.1));			
				
			//	System.out.print("B=" + BminPlus10);
				ArrayList<Integer> configLTBSmallVar = ltbSmallVar.computeMakespanWithCI(BminPlus10LTBSmallVar, 
						p_mu, p_sigma, p_m);
				 /*computeMakespanWithSuccessRate(BminPlus10LTBSmallVar, "", 0.9);*/
				double violateLTBSmallVar = ltbSmallVar.execute(executionSeed, 
						configLTBSmallVar.get(0).intValue(),
						configLTBSmallVar.get(1).intValue(), 
						configLTBSmallVar.get(2).intValue());
				
								
			//	System.out.print("\t" + config.get(0) + "\t" + config.get(1) + "\t" + (config.get(2) == 0 ? "false" : "true") + "\t" + violate);
				
				if((configLTBSmallVar.get(configLTBSmallVar.size()-1)==1) && (violateLTBSmallVar > 0))
					tnPredictionLTBSmallVar ++;
				if((configLTBSmallVar.get(configLTBSmallVar.size()-1)==0) && (violateLTBSmallVar <= 0))
					tpPredictionLTBSmallVar ++;
				if((configLTBSmallVar.get(configLTBSmallVar.size()-1)==1) && (violateLTBSmallVar <= 0))
					fnPredictionLTBSmallVar ++;
				if((configLTBSmallVar.get(configLTBSmallVar.size()-1)==0) && (violateLTBSmallVar > 0))
					fpPredictionLTBSmallVar ++;
				
				avgOverMnodesLTBSmallVar += (double)ltbSmallVar.overMnodes/(configLTBSmallVar.get(1).intValue() + configLTBSmallVar.get(2).intValue());
				
				histoLTBSmallVar.add(violateLTBSmallVar);
				
				if(printSampleSummary) {
					System.out.println();
				}
				// System.out.println();
			}
			
			System.out.println((double)tpPredictionNDB/noExecutions + "\t" + 
							   (double)tnPredictionNDB/noExecutions + "\t" +
							   (double)fpPredictionNDB/noExecutions + "\t" +
							   (double)fnPredictionNDB/noExecutions);
			
			System.out.println((double)tpPredictionSDB/noExecutions + "\t" +
							   (double)tnPredictionSDB/noExecutions + "\t" +
							   (double)fpPredictionSDB/noExecutions + "\t" +
							   (double)fnPredictionSDB/noExecutions);
			
			System.out.println((double)tpPredictionLTB/noExecutions + "\t" +
					   		   (double)tnPredictionLTB/noExecutions + "\t" +
					   		   (double)fpPredictionLTB/noExecutions + "\t" +
					   		   (double)fnPredictionLTB/noExecutions);
			
			System.out.println((double)tpPredictionLTBSameVar/noExecutions + "\t" +
							   (double)tnPredictionLTBSameVar/noExecutions + "\t" +
							   (double)fpPredictionLTBSameVar/noExecutions + "\t" +
							   (double)fnPredictionLTBSameVar/noExecutions);
			
			System.out.println((double)tpPredictionLTBSmallVar/noExecutions+ "\t" +
							   (double)tnPredictionLTBSmallVar/noExecutions+ "\t" +
							   (double)fpPredictionLTBSmallVar/noExecutions+ "\t" +
							   (double)fnPredictionLTBSmallVar/noExecutions);			
	
	
			Collections.sort(histoNDB);
			int[] categNDB = categorizeHistogram(histoNDB);
			
			for(int k = 0; k<categNDB.length; k++) {
				System.out.print(categNDB[k] + "\t");
			}
			
			System.out.print("avgOverMnodes: " + (double)avgOverMnodesNDB/100);
			
			Collections.sort(histoSDB);
			int[] categSDB = categorizeHistogram(histoSDB);
			
			System.out.println();
			
			for(int k = 0; k<categSDB.length; k++) {
				System.out.print(categSDB[k] + "\t");
			}
			
			System.out.print("avgOverMnodes: " + (double)avgOverMnodesSDB/100);
			
			Collections.sort(histoLTB);
			int[] categLTB = categorizeHistogram(histoLTB); 
			
			System.out.println();
			
			for(int k = 0; k<categLTB.length; k++) {
				System.out.print(categLTB[k] + "\t");
			}
			
			System.out.print("avgOverMnodes: " + (double)avgOverMnodesLTB/100);
			
			Collections.sort(histoLTBSameVar);
			int[] categLTBSameVar = categorizeHistogram(histoLTBSameVar); 
			
			System.out.println();
			
			
			for(int k = 0; k<categLTBSameVar.length; k++) {
				System.out.print(categLTBSameVar[k] + "\t");
			}
			
			System.out.print("avgOverMnodes: " + (double)avgOverMnodesLTBSameVar/100);
			
			Collections.sort(histoLTBSmallVar);
			int[] categLTBSmallVar = categorizeHistogram(histoLTBSmallVar); 
			
			System.out.println();
			
			for(int k = 0; k<categLTBSmallVar.length; k++) {
				System.out.print(categLTBSmallVar[k] + "\t");
			}
			
			System.out.print("avgOverMnodes: " + (double)avgOverMnodesLTBSmallVar/100);
			
			System.out.println("");
			System.out.println("Experiment " + j + " done");
	
		}
	}
		

	public void testSamplePhaseAllD() {
		noExperiments = 10;
		noExecutions = 100;
		
		Random randomizedSeed = new Random(99999999L);
		Random sampleSeeder = new Random(111111111L);		
				
		double desiredStDev = Math.sqrt(5);
		double desiredVar = desiredStDev * desiredStDev;
		
		int sampleSize = 30; 
		boolean printXLS = false;
		boolean printSampleSummary = false;
		boolean printNodeStats = false;
		
		
		System.out.println("realE \t" + 
				"sdtdevPerBagE \t" +
				"realStDev \t" +
				"stdevPerBagStDev \t" +
				"RealMBmin \t" +
				"stdevPerBagMBmin \t" +
				"RealMBminPlus10 \t" +
				"stdevPerBagMBminPlus10 \t" +
				"RealMBminPlus20 \t" +
				"stdevPerBagMBminPlus20 \t" +
				"RealMBmakespanMin \t" +
				"stdevPerBagMBmakespanMin \t" +
				"RealMBmakespanMinMinus10  \t" +
				"stdevPerBagMBmakespanMinMinus10 \t" +
				"RealMBmakespanMinMinus20 \t" +
				"stdevPerBagMBmakespanMinMinus20 \t" +
				"RealBmin  \t" +
				"stdevPerBagBmin \t" +							   
				"RealBminPlus10 \t" +
				"stdevPerBagBminPlus10 \t" +
				"RealBminPlus20  \t" +
				"stdevPerBagBminPlus20 \t" +
				"RealBmakespanMin \t" +
				"stdevPerBagBmakespanMin \t" +
				"RealBmakespanMinMinus10 \t" +
				"stdevPerBagBmakespanMinMinus10 \t" +
				"RealBmakespanMinMinus20  \t" +
				"stdevPerBagBmakespanMinMinus20" 
		);
		
		for(int j=0; j < noExperiments; j++) {
								
			long experimentBagGeneratingSeed = randomizedSeed.nextLong();
						
			NormalDistributedBag ndb = new NormalDistributedBag(1000, 
					experimentBagGeneratingSeed,
					15*60, desiredStDev*60,
					4, 3, 3);
			ndb.printXLS = printXLS;
			ndb.printSampleSummary = printSampleSummary;
			ndb.printNodeStats = printNodeStats;
			
			SampleStats ndbSS = new SampleStats(ndb, sampleSize);
			
			StableDistributedBag sdb = new StableDistributedBag(1000, 
					experimentBagGeneratingSeed,
					15*60, desiredStDev*60, 1.5, 0,
					4, 3, 3);
			sdb.printXLS = printXLS;
			sdb.printSampleSummary = printSampleSummary;
			sdb.printNodeStats = printNodeStats;
			
			SampleStats sdbSS = new SampleStats(sdb, sampleSize);
			
			LevyTruncatedDistributedBag ltb = new LevyTruncatedDistributedBag(1000, 
					experimentBagGeneratingSeed,
					0.366, 2700,
					4, 3, 3);
			ltb.printXLS = printXLS;
			ltb.printSampleSummary = printSampleSummary;
			ltb.printNodeStats = printNodeStats;
			
			SampleStats ltbSS = new SampleStats(ltb, sampleSize);
			
			
			LevyTruncatedDistributedBag ltbSameVar = 
				new LevyTruncatedDistributedBag(1000,							
					experimentBagGeneratingSeed,
					0.366, 2700,
					Math.sqrt(desiredVar*3600/ltb.realVariance),
					ltb.realExpectation*(1-Math.sqrt(desiredVar*3600/ltb.realVariance)),
					4, 3, 3);
			ltbSameVar.printXLS = printXLS;
			ltbSameVar.printSampleSummary = printSampleSummary;
			ltbSameVar.printNodeStats = printNodeStats;
			
			SampleStats ltbSameVarSS = new SampleStats(ltbSameVar, sampleSize);
			
			
			LevyTruncatedDistributedBag ltbSmallVar = 
				new LevyTruncatedDistributedBag(1000,							
					experimentBagGeneratingSeed,
					0.366, 2700,
					Math.sqrt(5*3600/ltb.realExpectation),
					ltb.realExpectation*(1-Math.sqrt(5*3600/ltb.realVariance)),
					4, 3, 3);
			ltbSmallVar.printXLS = printXLS;
			ltbSmallVar.printSampleSummary = printSampleSummary;
			ltbSmallVar.printNodeStats = printNodeStats;
			
			SampleStats ltbSmallVarSS = new SampleStats(ltbSmallVar, sampleSize);
			
			
			for (int i=0; i < noExecutions; i++) {	
				
				
				long sampleSeed = sampleSeeder.nextLong();		
				
				NormalDistributedBag tmpNDB = ndb.copy();								
				tmpNDB.sample(sampleSeed, sampleSize);						
				ndbSS.addStats(tmpNDB);
				
				if(printSampleSummary) {
					System.out.println();
				}
				
				StableDistributedBag tmpSDB = sdb.copy();								
				tmpSDB.sample(sampleSeed, sampleSize);						
				sdbSS.addStats(tmpSDB);
				
				if(printSampleSummary) {
					System.out.println();
				}
				
				LevyTruncatedDistributedBag tmpLTB = ltb.copy();								
				tmpLTB.sample(sampleSeed, sampleSize);						
				ltbSS.addStats(tmpLTB);
				
				if(printSampleSummary) {
					System.out.println();
				}
				
				LevyTruncatedDistributedBag tmpLTBSameVar = ltbSameVar.copy();								
				tmpLTBSameVar.sample(sampleSeed, sampleSize);						
				ltbSameVarSS.addStats(tmpLTBSameVar); 
				
				if(printSampleSummary) {
					System.out.println();
				} 
					
				LevyTruncatedDistributedBag tmpLTBSmallVar = ltbSmallVar.copy();								
				tmpLTBSmallVar.sample(sampleSeed, sampleSize);						
				ltbSmallVarSS.addStats(tmpLTBSmallVar); 
				
				if(printSampleSummary) {
					System.out.println();
				}
				
			}
			
			ndbSS.printStats(noExecutions);
			sdbSS.printStats(noExecutions);
			ltbSS.printStats(noExecutions);
			ltbSameVarSS.printStats(noExecutions);
			ltbSmallVarSS.printStats(noExecutions);
			
			System.out.println("Experiment " + j + " done");
	
		}
	}
	

	private int[]  categorizeHistogram(ArrayList<Double> histo) {
		
		int[] categ = new int[11];
		
		int i = 0;
		while(histo.get(i) * 100 < -50) {
			categ[0] ++;
			i++;
			if(i>=histo.size()) return categ;
		}			
		while((-50 <= histo.get(i) * 100) && ( histo.get(i) * 100 < -20) ) {
			categ[1] ++;
			i++;
			if(i>=histo.size()) return categ;
		}
		while((-20 <= histo.get(i) * 100) && ( histo.get(i) * 100 < -10) ) {
			categ[2] ++;
			i++;
			if(i>=histo.size()) return categ;
		}
		while((-10 <= histo.get(i) * 100) && ( histo.get(i) * 100 < -5) ) {
			categ[3] ++;
			i++;
			if(i>=histo.size()) return categ;
		}
		while((-5 <= histo.get(i) * 100) && ( histo.get(i) * 100 < -1) ) {
			categ[4] ++;
			i++;
			if(i>=histo.size()) return categ;
		}
		while((-1 <= histo.get(i) * 100) && ( histo.get(i) * 100 <= 1) ) {
			categ[5] ++;
			i++;
			if(i>=histo.size()) return categ;
		}
		while((1 < histo.get(i) * 100) && ( histo.get(i) * 100 < 5) ) {
			categ[6] ++;
			i++;
			if(i>=histo.size()) return categ;
		}
		while((5 <= histo.get(i) * 100) && ( histo.get(i) * 100 < 10) ) {
			categ[7] ++;
			i++;
			if(i>=histo.size()) return categ;
		}
		while((10 <= histo.get(i) * 100) && ( histo.get(i) * 100 < 20) ) {
			categ[8] ++;
			i++;
			if(i>=histo.size()) return categ;
		}
		while((20 <= histo.get(i) * 100) && ( histo.get(i) * 100 <= 50) ) {
			categ[9] ++;
			i++;
			if(i>=histo.size()) return categ;
		}
		while(50 <= histo.get(i) * 100) {
			categ[10] ++;
			i++;
			if(i>=histo.size()) return categ;
		}
		
		return categ;
	}
	
	public void printBags() {
		
		noExperiments = 1;
		
		
		Random randomizedSeed = new Random(99999999L);
				
		
		for(int j=0; j < noExperiments; j++) {

			long experimentBagGeneratingSeed = randomizedSeed.nextLong();


			NormalDistributedBag ndb = new NormalDistributedBag(1000, 
					experimentBagGeneratingSeed,
					15*60, Math.sqrt(5)*60,
					4, 3, 3);

			

			StableDistributedBag sdb = new StableDistributedBag(1000, 
					experimentBagGeneratingSeed,
					15*60, Math.sqrt(5)*60, 1.5, 0,
					4, 3, 3);



			LevyTruncatedDistributedBag ltb = new LevyTruncatedDistributedBag(1000, 
					experimentBagGeneratingSeed,
					0.366, 2700,
					4, 3, 3);



			for(int i=0; i<1000; i++) {
				System.out.println(ndb.x.get(i) + "\t" + sdb.x.get(i) + "\t" + ltb.x.get(i));
			}
			
			// System.out.println();
		}
	}

	void printLTBSameVar() {
		
		double desiredStDev = Math.sqrt(5);
		double desiredVar = desiredStDev * desiredStDev;
		long experimentBagGeneratingSeed = 8888888L;
		LevyTruncatedDistributedBag ltb = new LevyTruncatedDistributedBag(1000, 
				experimentBagGeneratingSeed,
				0.366, 2700,
				3, 4, 3);
				//4, 3, 3);

		double ltbExpectation = ltb.getExpectation();
		double ltbVariance = ltb.getVariance();
		
		LevyTruncatedDistributedBag ltbSameVar = 
			new LevyTruncatedDistributedBag(1000,							
					experimentBagGeneratingSeed,
					0.366, 2700,
					Math.sqrt(desiredVar*3600/ltbVariance),
					ltbExpectation*(1-Math.sqrt(desiredVar*3600/ltbVariance)),
					3, 4, 3);
		
		ltbSameVar.print();
	}
	
	public static void main(String args[]) {
		TestStatistics ts = new TestStatistics();
		//ts.printLTBSameVar();
		/* Chapter 5
		ts.testTailPhaseMultipleClusters();
		*/
		/*Chapter 4 
		 * 
		 */
		//ts.testSamplePhaseMultipleClusters(args[0]);
		
		noSampleExperiments = 30 ;
		noExecutions = 30;
		
		int sampleSize = 30;
		
		double desiredStDev = Math.sqrt(5);
		double desiredVar = desiredStDev * desiredStDev;
		long experimentBagGeneratingSeed;
		Random randomizedSeed = new Random(99999999L);
		experimentBagGeneratingSeed = randomizedSeed.nextLong();
		
		String bagType = "LTB";
		String budgetType = "Bmin";
		
		if(args.length > 0) { 
			bagType = args[0];		 
			if(args.length > 1) {
				budgetType = args[1]; 
			}
		}
		
		if(bagType.equals("NDB")) {
			NormalDistributedBag ndb = new NormalDistributedBag(1000, 
					experimentBagGeneratingSeed,
					15*60, desiredStDev*60,
					4, 3, 3);
			ndb.sampleSize = sampleSize;
			ts.testSamplePhaseMultipleClusters(budgetType,ndb);
		} else if (bagType.equals("MDB")) {
			double mean1 = 48; double stdev1 = 29;
			double low2 = 106; double high2 = 607;
			double mean3 = 689; double stdev3 = 27;
			double low4 = 771; double high4 = 892;
			double low5 = 1649; double high5 = 2553;
			
			Random MDSeeder = new Random(experimentBagGeneratingSeed);
			long experimentBagGeneratingSeed0 = MDSeeder.nextLong();
			long experimentBagGeneratingSeed1 = MDSeeder.nextLong();
			long experimentBagGeneratingSeed2 = MDSeeder.nextLong();
			long experimentBagGeneratingSeed3 = MDSeeder.nextLong();
			long experimentBagGeneratingSeed4 = MDSeeder.nextLong();
			long experimentBagGeneratingSeed5 = MDSeeder.nextLong();
			
			DACHMixtureDistributedTruncatedBag mdb = new DACHMixtureDistributedTruncatedBag(1000,
					experimentBagGeneratingSeed1, mean1, stdev1,
					experimentBagGeneratingSeed2, low2, high2,
					experimentBagGeneratingSeed3, mean3, stdev3,
					experimentBagGeneratingSeed4, low4, high4,
					experimentBagGeneratingSeed5, low5, high5,
					0.274111675, 0.222335025, 0.452791878, 0.026395939,  
					experimentBagGeneratingSeed0,
					3,4,3);
			mdb.sampleSize = sampleSize;
			
			ts.testSamplePhaseMultipleClusters(budgetType,mdb);
		} else if (bagType.equals("LTB")) {
			LevyTruncatedDistributedBag ltb = new LevyTruncatedDistributedBag(1000, 
					experimentBagGeneratingSeed,
					0.366, 2700,
					3, 4, 3);
			ltb.sampleSize = sampleSize;
			
			ts.testSamplePhaseMultipleClusters(budgetType,ltb);
		} else if (bagType.equals("SDB")) {
			StableDistributedBag sdb = new StableDistributedBag(1000, 
					experimentBagGeneratingSeed,
					15*60, desiredStDev*60, 1.5, 0,
					4, 3, 3);
			sdb.sampleSize = sampleSize;
			
			ts.testSamplePhaseMultipleClusters(budgetType,sdb);
		}
	}
}
