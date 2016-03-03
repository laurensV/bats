package org.koala.runnersFramework.runners.bot;

import ibis.ipl.IbisIdentifier;

import java.io.Serializable;
import java.util.TimerTask;

public class WorkerStats {

	/*expressed in milliseconds*/
	private long uptime;
	/*expressed in nanoseconds*/
	private long runtime;
	private long noJobs;
	private String node;
	private boolean done;	
	/*expressed in milliseconds*/
	private long startTime;	
	/*expressed in nanoseconds*/
	private long latestJobStartTime;
	private IbisIdentifier ii;
	private boolean willTerminate;
	int timestamp, oldtimestamp;
	/*expressed in minutes*/
	private double futurePoint=0.0;
	/*expressed in jobs per minute*/
	private double speed;
	/*expressed in minutes*/
	private double offset;
	private boolean isInitWorker;
	/*expressed in minutes*/
	private double lastEstimatedUptime;
	/*expressed in milliseconds*/
	private long timeToDie; 
	int expNoATU;
	public int noATUPlan;
	
	public TimerTask killingMe;
	public long timeLeftATU;
	
	long lastPriceQuery;
		
	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public WorkerStats(String node, long startTime, IbisIdentifier ii) {
		this.node = node;
		this.startTime = startTime;
		this.ii = ii;
		noJobs = 0;
		uptime = 0;		
		runtime = 0;
		done = false;
		willTerminate = false;
		lastPriceQuery = 0;
	}
	
	public boolean isFinished() {
		return done;
	}

	public void addJobStats(long r) {
		noJobs ++;
		runtime += r;
		latestJobStartTime = 0;
	}	
	
	public void workerFinished(long endTime) {
		done = true;
		uptime += endTime - startTime;
		timestamp = 0;
	}
	
	public long getUptime() {
		return done ? uptime : uptime + (System.currentTimeMillis() - startTime);
	}
	
	public long getRuntime() {
		/*TODO similar computation as in getUptime*/
		return runtime;
	}

	public void setRuntime(long runtime) {
		this.runtime = runtime;
	}
	
	public long getNoJobs() {
		return noJobs;
	}

	public void setLatestJobStartTime(long latestJobStartTime) {
		this.latestJobStartTime = latestJobStartTime;
	}

	public long getLatestJobStartTime() {
		return latestJobStartTime;
	}
	
	public void setFuturePoint(double future) {
		futurePoint = future;
	}

	public double getFuturePoint() {
		return futurePoint;
	}
	
	public void printStats() {		
		System.out.println("Node " + node + " stats => uptime(ms): " + uptime + " runtime(nanos): " + runtime + " jobs: " + noJobs);
		double rtms = (double)runtime/1000000;
		
		System.out.println("runtime/uptime: " + (uptime == 0 ? "NaN" : rtms/uptime) + " avg runtime per job(ms): " 
				+ (noJobs == 0 ? "NaN" : rtms/noJobs) + " avg uptime per job(ms): " +
				(noJobs == 0 ? "NaN" : (double)uptime/noJobs));
	}

	public void reacquire(long timeUnit, long currentTimeMillis) {
		done = false;
		uptime = (long)Math.ceil((double)uptime / 60000 / timeUnit) * timeUnit * 60000;
		startTime = currentTimeMillis;
		willTerminate = false;
	}

	public IbisIdentifier getIbisIdentifier() {
		return ii;
	}
	
	public void setIbisIdentifier(IbisIdentifier ii) {
		this.ii = ii;
	}

	public boolean isMarked() {
		return willTerminate ;
	}

	public void markTerminated(TimerTask tt) {
		willTerminate = true;
		oldtimestamp = timestamp;
		timestamp = 0;	
		killingMe = tt;
	}
	
	public void unmarkTerminated() {
		willTerminate = false;
		timestamp = oldtimestamp;
	}

	public double getOffset() {
		return offset;
	}
	
	public void setOffset(double offset) {
		this.offset = offset;
	}

	public boolean isInitialWorker() {
		return isInitWorker;
	}
	
	public void setInitialWorker(boolean maybe) {
		isInitWorker = maybe;
	}

	public double getLastEstimatedUptime() {
		return lastEstimatedUptime;
	}
	
	public void setLastEstimatedUptime(double save) {
	    lastEstimatedUptime = save;
	}

	public void setTimeToDie(long l) {
		timeToDie = l;		
	}
	
	public long getTimeToDie() {
		return timeToDie;		
	}
	
	public long getStartTime() {
		return startTime;
	}

	public void setTimeLeftATU(long timeLeftATU) {
		this.timeLeftATU = timeLeftATU;
	}
	
	public boolean isUpdateNeeded(){
		return true;
	}
	
	public void updateCost(double price){
		
	}
	
	public double getCost(){
		return 0;
	}
	
	public double getLastPrice(){
		return 0;
	}
}
