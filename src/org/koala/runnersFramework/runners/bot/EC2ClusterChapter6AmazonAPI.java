package org.koala.runnersFramework.runners.bot;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;

public class EC2ClusterChapter6AmazonAPI extends EC2ClusterAmazonAPI {


	public EC2ClusterChapter6AmazonAPI(String hostname, int port,
			String alias, long timeUnit, double costUnit, int maxNodes,
			String speedFactor, String AMI, String instanceType,
			String keyPairName, String keyPairPath, String accessKey,
			String secretKey) {
		super(hostname, port, alias, timeUnit, costUnit, maxNodes, speedFactor, AMI,
				instanceType, keyPairName, keyPairPath, accessKey, secretKey);
		
		ibisServer = "";
		
	}

	@Override
	public Process startNodes(String time, int noNodes, String electionName,
			String poolName, String serverAddress) throws AmazonServiceException {
		if (this.ec2 == null){
			initClient();
		}
		
		if(noNodes == 0)
		{
			return null;
		}
		
		/* set the requested VM properties */
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
	    .withInstanceType(this.instanceType)
	    .withImageId(this.AMI)
	    .withMinCount(noNodes)
	    .withMaxCount(noNodes)
	    .withKeyName(this.keyPairName)
	    .withUserData(Base64.encodeBase64String(createUserData(electionName, poolName, serverAddress).getBytes()));
		
		RunInstancesResult result;
		
		try {
			/* request the VM */
			//time before after
			result = ec2.runInstances(runInstancesRequest);
		} catch (AmazonServiceException e)
		{
            Logger.getLogger(EC2ClusterChapter6AmazonAPI.class.getName()).log(Level.SEVERE, null, e);
            throw new RuntimeException("FAILED to start Amazon workers.");
		}
		
		System.out.println("Successfully created " + noNodes + " EC2 instances");
		
		Reservation reservation = result.getReservation();
		
        for (Instance instance : reservation.getInstances()) {
            System.out.println("EC2 instance with InstanceID " + instance.getInstanceId());
        }
		return null;
	}

	private String createUserData(String electionName, String poolName,
			String serverAddress) {
        String script;

        script = "#!/bin/bash\n"
                +		 "export BATS_HOME=/root/ConPaaS-TaskFarmService\n"
                + 		 "export BATS_HOME_LIB=/root/ConPaaS-TaskFarmService/lib\n"
                +		 "export IPL_HOME=$BATS_HOME/ipl-2.2\n"
                +		 "export WORKER_LOG=$BATS_HOME/worker.log\n"
                +		 "export ID=`curl http://169.254.169.254/latest/meta-data/instance-id`\n"
                /*+		 "export MOUNT_FOLDER=/root/xtfs\n"
                +		 "export MOUNT_LOG=$BATS_HOME/mount.log\n"
                +		 "mkdir -p $MOUNT_FOLDER\n"
                +		 "echo \"" + System.getenv().get("XTFS_CERT") + "\" > vu_taskfarm_client.p12.hexed\n"
                +		 "xxd -r -p vu_taskfarm_client.p12.hexed > $BATS_HOME/vu_taskfarm_client.p12\n"
                +		 "echo \"" + System.getenv().get("GRID_MAP") + "\" > grid_mapfile.hexed\n"
                +		 "xxd -r -p grid_mapfile.hexed > $BATS_HOME/grid_mapfile\n"
                +		 "mount.xtreemfs --interrupt-signal=0 " + System.getenv().get("FRESH_BATS") + " $MOUNT_FOLDER"
                +		 " --pkcs12-file-path=$BATS_HOME/vu_taskfarm_client.p12 "
                + 		 " --globus-gridmap --gridmap-location=$BATS_HOME/grid_mapfile"
                +		 " 1>> $BATS_HOME/mounting.log 2>> $BATS_HOME/mounting.err.log\n"*/       
   //     +		 "cd $MOUNT_FOLDER\n"
   				+		 "java "
   				+		 "-classpath $BATS_HOME/lib/*:$IPL_HOME/lib/*:$IPL_HOME/external/* "
   				+		 "-Dibis.location=$ID@" + hostname  + " "
   				+ 		 "org.koala.runnersFramework.runners.bot.VMWorker "
   				+		 electionName + " " + poolName + " "
        /* FIXME: right now we have a hardcoded Ibis address*/
   				+ 		((ibisServer.equals("")) ? serverAddress : this.ibisServer) + " " + speedFactor
   				+ 		" &> $WORKER_LOG\n"
   			/*	+		"umount $MOUNT_FOLDER >> $MOUNT_LOG\n"*/;
        // shutdown!
        return script;
	}

	@Override
	public void terminateNode(IbisIdentifier from, Ibis myIbis)
			throws IOException {
        
		
		myIbis.registry().signal("die", from);
        String instanceIDWithLocation = from.location().toString();
        String instanceID[] = instanceIDWithLocation.split("@"); 
        
		/* set the instance ID of the VM to be terminated */
		TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest()
	    .withInstanceIds(instanceID[0]);
		
		/* Terminating the VM */
        try {
            ec2.terminateInstances(terminateInstancesRequest);
        } catch (AmazonServiceException e) {
            throw new RuntimeException(
                    "Exception while terminating the EC2 VM. "
                    + "ibisLocation=" + from.location() + "; InstanceID=" + instanceID
                    + " Error msg:\n" + e);
        }
	}
	
	public void initClient(){
		this.ec2 = new AmazonEC2Client(new BasicAWSCredentials(accessKey, secretKey));
	}
}
