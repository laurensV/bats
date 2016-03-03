package org.koala.runnersFramework.runners.bot;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryRequest;
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryResult;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.SpotPlacement;
import com.amazonaws.services.ec2.model.SpotPrice;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

public class EC2ClusterAmazonAPISpot extends EC2ClusterAmazonAPI implements Runnable{
	
	/* Bid price for spot instance */
	double bidPrice;
	double maxSpotPrice;
	
	/* A request can be one-time or persistent */
	String requestType;
	int nextIbisId = 1;
	
	// last queried price
	double lastCurrentPrice;
	// time of last query
	long lastQueryTime = 0;
	// interval of querying
	final int interval = 900000;
	

	/* List of instanceIds started for the requests */
	ArrayList<String> ibisId;
	HashMap<Integer, String> idReqMap;
	HashMap<Integer, String> idAZoneMap;
	HashMap<String, Long> reqTimeMap;
	
	long startTime;

    private static final long SLEEP_CYCLE = 5000;
	
	public EC2ClusterAmazonAPISpot(String hostname, int port,
			String alias, long timeUnit, double costUnit, int maxNodes,
			String speedFactor, String AMI, String instanceType,
			String keyPairName, String keyPairPath, String accessKey,
			String secretKey) {
		super(hostname, port, alias, timeUnit, costUnit, maxNodes, speedFactor, AMI,
				instanceType, keyPairName, keyPairPath, accessKey, secretKey);
		
		/* Setup an arraylist to collect all of the request ids we want to watch */
		ibisId = new ArrayList<String>();
		ibisServer = "";
		idReqMap = new HashMap<Integer, String>();
		idAZoneMap = new HashMap<Integer, String>();
		reqTimeMap = new HashMap<String, Long>();
		this.instanceType = instanceType;
		bidPrice = costUnit/1000;
	}
	
	public synchronized boolean areAnyOpen() {
    	DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();

		System.out.println("Checking to determine if Spot Bids have reached the active state...");
		try
		{
        	// Retrieve all of the requests we want to monitor. 
			DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
			List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();

        	// Look through each request and determine which is in the active state.
        	for (SpotInstanceRequest describeResponse : describeResponses) {
        		System.out.println(" " +describeResponse.getSpotInstanceRequestId() + 
        						   " is in the "+describeResponse.getState() + " state.");
        		long endTime = (System.nanoTime() - startTime)/1000000000;
        		
        		System.out.println("Time elapsed since request: " + endTime);
    			System.out.println("Spot price: " + describeResponse.getSpotPrice());
    			System.out.println("Launched availability zone: " + describeResponse.getAvailabilityZoneGroup());
        		if (describeResponse.getState().equals("open"))
        			return true;
        	}
		} catch (AmazonServiceException e) {
			/* Print out the error. */
			System.out.println("Error when calling describeSpotInstances");
            System.out.println("Caught Exception: " + e.getMessage());
            System.out.println("Reponse Status Code: " + e.getStatusCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Request ID: " + e.getRequestId());
            
            /* If we have an exception, ensure we don't break out of the loop. */
			return true;
        }
		return false; 	
    }
	
	public void initClient(){
		this.ec2 = new AmazonEC2Client(new BasicAWSCredentials(accessKey, secretKey));
	}
	
	@Override
	public Process startNodes(String time, int noNodes, String electionName,
			String poolName, String serverAddress) throws AmazonServiceException {
		if (this.ec2 == null){
			initClient();
		}
		
		System.out.println("No nodes" + noNodes);
		
		/* null or "" for the requestType parameter	*/	
		for (int i = 0; i < noNodes; i++){
			RequestSpotInstancesRequest request = new RequestSpotInstancesRequest();
			/* Set the bid price */
			request.setSpotPrice(bidPrice+"");

			/* Set number of instances */
			request.setInstanceCount(1);

			/* Setup the specifications of the launch. This includes the instance type and the AMI. */
			LaunchSpecification launchSpecification = new LaunchSpecification();
			launchSpecification.setImageId(AMI);
			launchSpecification.setInstanceType(instanceType);
			launchSpecification.setKeyName(keyPairName);
			
			launchSpecification.withUserData(Base64.encodeBase64String(createUserData(instanceType, electionName,
					poolName, serverAddress).getBytes()));

			/* Check to see if we need to set the request type. */
			if (requestType != null && !requestType.equals("")) {
				/* Set the type of the bid. */
				request.setType(requestType);
			}

			/* Add the launch specifications to the request. */
			request.setLaunchSpecification(launchSpecification);

			/* Call the RequestSpotInstance API. */
			RequestSpotInstancesResult requestResult = ec2.requestSpotInstances(request);
			/* Register start time for submitting a request. */
			startTime = System.nanoTime();
			List<SpotInstanceRequest> requestResponses = requestResult.getSpotInstanceRequests();    	

			/* Add all of the request ids to the list so we can determine when they hit the 
			 * active state.
			 */
			for (SpotInstanceRequest requestResponse : requestResponses) {
				String reqId = requestResponse.getSpotInstanceRequestId();
				System.out.println("Created Spot Request: " + reqId);
				System.out.println("Mapping IbisId " + nextIbisId + " with Spot Instance Req Id" + reqId);
				idReqMap.put(nextIbisId, reqId);
				reqTimeMap.put(reqId, startTime);
				nextIbisId++;
			}
    	
		}
    	/* monitor the requests
    	new Thread(this).start();*/
		return null;
	}

	private String createUserData(String instanceType, String electionName, String poolName,
			String serverAddress) {
        String script;

        script = "#!/bin/bash\n"
        +		 "export BATS_HOME=/root/ConPaaS-TaskFarmService\n"
        + 		 "export BATS_HOME_LIB=/root/ConPaaS-TaskFarmService/lib\n"
        +		 "export IPL_HOME=$BATS_HOME/ipl-2.2\n"
        +		 "export WORKER_LOG=$BATS_HOME/worker.log\n"
        +		 " 1>> $BATS_HOME/mounting.log 2>> $BATS_HOME/mounting.err.log\n"
        +		 "java "
        +		 "-classpath $BATS_HOME/lib/*:$IPL_HOME/lib/*:$IPL_HOME/external/* "
        +		 "-Dibis.location="+nextIbisId+"@" + hostname  + " "
        + 		 "org.koala.runnersFramework.runners.bot.VMWorker "
        +		 electionName + " " + poolName + " "
        /* FIXME: right now we have a hardcoded Ibis address*/
        + 		((ibisServer.equals("")) ? serverAddress : this.ibisServer) + " " + speedFactor
        + 		" &> $WORKER_LOG\n";

        ibisId.add(nextIbisId+"");
                
        return script;
	}

	@Override
	public void terminateNode(IbisIdentifier from, Ibis myIbis)
			throws IOException {
        
        ArrayList<String> terminationListInst = new ArrayList<String>();
		ArrayList<String> terminationListReq = new ArrayList<String>();
		
		myIbis.registry().signal("die", from);
        String instanceIDWithLocation = from.location().toString();
        String instanceID[] = instanceIDWithLocation.split("@");
        
        terminationListReq.add(idReqMap.get(Integer.parseInt(instanceID[0])));
        /* Create the request for finding out which instance
         * corresponds to the request which will be cancelled
         */
        DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();    	
    	describeRequest.setSpotInstanceRequestIds(terminationListReq);

		try{
			DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
			List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();

        	for (SpotInstanceRequest describeResponse : describeResponses) {
        		/* Add the instance id to the list for termination */
        		terminationListInst.add(describeResponse.getInstanceId());
        	}
		} catch (AmazonServiceException e) {
			System.out.println("Error when calling describeSpotInstances");
            System.out.println("Caught Exception: " + e.getMessage());
            System.out.println("Reponse Status Code: " + e.getStatusCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Request ID: " + e.getRequestId());
        }  

		terminateInstance(terminationListInst);
        cancelRequest(terminationListReq);
	}

	public void cancelRequest(ArrayList<String> terminationList){
		try {
		       // Cancel request.
		       CancelSpotInstanceRequestsRequest cancelRequest = new CancelSpotInstanceRequestsRequest(terminationList);
		       ec2.cancelSpotInstanceRequests(cancelRequest);
		     } catch (AmazonServiceException e) {
		       // Write out any exceptions that may have occurred.
		       System.out.println("Error cancelling instances");
		       System.out.println("Caught Exception: " + e.getMessage());
		       System.out.println("Reponse Status Code: " + e.getStatusCode());
		       System.out.println("Error Code: " + e.getErrorCode());
		       System.out.println("Request ID: " + e.getRequestId());
		     }
	}
	
	public void terminateInstance(List<String> instanceList){
		try {
		       // Terminate instances.
		       TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest(instanceList);
		       ec2.terminateInstances(terminateRequest);
		     } catch (AmazonServiceException e) {
		       // Write out any exceptions that may have occurred.
		       System.out.println("Error terminating instances");
		       System.out.println("Caught Exception: " + e.getMessage());
		       System.out.println("Reponse Status Code: " + e.getStatusCode());
		       System.out.println("Error Code: " + e.getErrorCode());
		       System.out.println("Request ID: " + e.getRequestId());
		     }
	}

	@Override
	public void run() {
		// Monitor spot instance requests
		do
    	{
        	try {
				Thread.sleep(SLEEP_CYCLE);
			} catch (InterruptedException e) {
				System.out.println("Error in thread sleep");
				e.printStackTrace();
			}
    	} while (areAnyOpen());
		
	}
	
	/* Method for getting the price of a machine from this cluster
	 * according to the type of strategy used.
	 */
	@Override
	public double getCost(int type){
		return queryPrice(type, false);
	}
	
	/* Method for updating the price of a machine */
	public double queryPrice(int type, boolean setBidPrice){
		
		double returnPrice;
		
		if (this.ec2 == null){
			initClient();
		}
		/* Price update with the lowest price from all availability zones*/
		if (System.currentTimeMillis() - lastQueryTime > interval || lastQueryTime == 0){
			DescribeSpotPriceHistoryRequest dr = new DescribeSpotPriceHistoryRequest();
			ArrayList<String> inst = new ArrayList<String>();

			inst.add(instanceType);
			dr.setInstanceTypes(inst);
			ArrayList<String> pd = new ArrayList<String>();
			pd.add(Const.machineType);
			dr.setProductDescriptions(pd);
			Calendar c = Calendar.getInstance();
			Date d = c.getTime();
			dr.setStartTime(d);
			DescribeSpotPriceHistoryResult hr = ec2.describeSpotPriceHistory(dr);
			List<SpotPrice> spotPrice = hr.getSpotPriceHistory();

			ArrayList<Double> prices = new ArrayList<Double>();
			for (int i = 0; i < spotPrice.size(); i++){
				prices.add(Double.parseDouble(spotPrice.get(i).getSpotPrice()));
			}
			Collections.sort(prices);
			lastCurrentPrice = prices.get(0)*1000;
			lastQueryTime = System.currentTimeMillis();
		}
		
		/* Set the returned price according to the strategy*/
		if (type == Const.avg)
			returnPrice = (lastCurrentPrice+maxSpotPrice)/2;
		else if(type == Const.onDemand)
			returnPrice = costUnit;
		else if(type == Const.max)
			returnPrice = maxSpotPrice;
		else 
			returnPrice = lastCurrentPrice;
		
		/* Temporary hack
		 * if (Const.getMinPrice(instanceType) > returnPrice)
			returnPrice = Const.getMinPrice(instanceType);
		 */
		if (setBidPrice) bidPrice = returnPrice/1000;
		return returnPrice;
	}
	
	@Override
	public void computeProfitability(Cluster cheapest) {
		profitability = (cheapest.Ti * cheapest.costUnit) / (Ti * costUnit); 
	}
	
	@Override
	public void computeMaxPrice(Cluster cheapest, double profitability){
		this.maxSpotPrice = (cheapest.Ti * cheapest.costUnit) / (profitability * Ti);
		System.out.println("Max spot price for " + this.instanceType + " is: " + this.maxSpotPrice);
	}
	
	/* Find the avaulability zone of a machine */
	public String getAvailabilityZone(IbisIdentifier from){
		String instanceIDWithLocation = from.location().toString();
		String instanceID[] = instanceIDWithLocation.split("@");
		ArrayList<String> listInst = new ArrayList<String>();
		ArrayList<String> listReq = new ArrayList<String>();
		
		int id = Integer.parseInt(instanceID[0]);
		
		if (idAZoneMap.containsKey(id)){
			return idAZoneMap.get(id);
		}
		else {
			 listReq.add(idReqMap.get(id));
			 DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
		     describeRequest.setSpotInstanceRequestIds(listReq);
		     DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
		     List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();

		     for (SpotInstanceRequest describeResponse : describeResponses) {
		    	 // Add the instance id to the list
		    	 listInst.add(describeResponse.getInstanceId());
		    	 String zone = describeResponse.getAvailabilityZoneGroup();
		    	 idAZoneMap.put(id, zone);
		    	 return zone;
		     }
		     
		}
		return "none";
	}
	
	
	/* Method for getting the cost of an instance 
	 * at a specific time in the past
	 */
	@Override
	public double getCostUsingDate(long time, IbisIdentifier from){
		String availabilityZone = getAvailabilityZone(from);
		DescribeSpotPriceHistoryRequest dr = new DescribeSpotPriceHistoryRequest();
		ArrayList<String> inst = new ArrayList<String>();

		inst.add(instanceType);
		dr.setInstanceTypes(inst);
		ArrayList<String> pd = new ArrayList<String>();
		pd.add(Const.machineType);
		dr.setProductDescriptions(pd);
		dr.setAvailabilityZone(availabilityZone);
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(time);
		Date d = c.getTime();
		dr.setStartTime(d);
		DescribeSpotPriceHistoryResult hr = ec2.describeSpotPriceHistory(dr);
		List<SpotPrice> spotPrice = hr.getSpotPriceHistory();

		ArrayList<Double> prices = new ArrayList<Double>();
		for (int i = 0; i < spotPrice.size(); i++){
			prices.add(Double.parseDouble(spotPrice.get(i).getSpotPrice()));
		}
		Collections.sort(prices);
		return prices.get(0)*1000;
	}
	
	@Override
	public void setBidPrice(int type){
		queryPrice(type, true);
	}
	
	/* Method for checking if a specific instance
	 * has been terminated by Amazon
	 */
	@Override
	public boolean terminatedByProvider(IbisIdentifier from){
		String instanceIDWithLocation = from.location().toString();
		String instanceID[] = instanceIDWithLocation.split("@");
		ArrayList<String> listInst = new ArrayList<String>();
		ArrayList<String> listReq = new ArrayList<String>();

		int id = Integer.parseInt(instanceID[0]);

		listReq.add(idReqMap.get(id));
		DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
		describeRequest.setSpotInstanceRequestIds(listReq);
		DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
		List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();

		for (SpotInstanceRequest describeResponse : describeResponses) {
			//if (describeResponse.getInstanceId()==null)
			System.out.println("Instance state is: " + describeResponse.getState());
			if (describeResponse.getState().equals("closed")){
				return true;
			}
		}
		return false;
	}
}



