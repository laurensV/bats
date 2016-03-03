package org.koala.runnersFramework.runners.bot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisProperties;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

public class DAS4Worker implements RegistryEventHandler {

	private static final PortType masterReplyPortType = new PortType(
			PortType.COMMUNICATION_RELIABLE, PortType.SERIALIZATION_OBJECT,
			PortType.RECEIVE_EXPLICIT, PortType.RECEIVE_TIMEOUT, PortType.CONNECTION_ONE_TO_ONE);
	private static final PortType workerRequestPortType = new PortType(
			PortType.COMMUNICATION_RELIABLE, PortType.SERIALIZATION_OBJECT,
			PortType.RECEIVE_EXPLICIT, PortType.RECEIVE_TIMEOUT, PortType.CONNECTION_MANY_TO_ONE, PortType.CONNECTION_DOWNCALLS);
	private static final IbisCapabilities ibisCapabilities = new IbisCapabilities(
			IbisCapabilities.MALLEABLE,
			IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED,
			IbisCapabilities.ELECTIONS_STRICT,
			/*for job preemption*/
			IbisCapabilities.SIGNALS);

	protected Ibis myIbis;
	protected SendPort workRequestPort;
	protected ReceivePort workReplyPort;

	int noThreads;
	int noJobRequests = Integer.MAX_VALUE;
	double factor;

	DAS4Worker(String masterID, String poolName, String serverAddress, String speedFactor)
			throws Exception {
		Properties props = new Properties();

		props.setProperty(IbisProperties.POOL_NAME, poolName);
		props.setProperty(IbisProperties.SERVER_ADDRESS, serverAddress);
		myIbis = IbisFactory.createIbis(ibisCapabilities, props, true, this,
				workerRequestPortType, masterReplyPortType);

		// small hack to get node name
		String node = myIbis.identifier().location().getLevel(0);
		String hostname = myIbis.identifier().location().getLevel(3);
		myIbis.end();

		props.setProperty(IbisProperties.LOCATION, node + "@" + hostname);
		props.setProperty(IbisProperties.LOCATION_POSTFIX, "");

		myIbis = IbisFactory.createIbis(ibisCapabilities, props, true, this,
				workerRequestPortType, masterReplyPortType);

		IbisIdentifier master = myIbis.registry().getElectionResult(masterID);

		/*for job preemption*/
		myIbis.registry().enableEvents();

		workRequestPort = myIbis.createSendPort(workerRequestPortType);
		workRequestPort.connect(master, "master");

		workReplyPort = myIbis.createReceivePort(
				this.masterReplyPortType, "worker");
		workReplyPort.enableConnections();

		WriteMessage requestWork;
		factor = Double.parseDouble(speedFactor);

		List<Future<JobResult>> futures;
		ExecutorService executorService = Executors.newCachedThreadPool();

		boolean pauseReceived = false;
		boolean sendAll = false;
		boolean done = false;

		futures = new ArrayList<Future<JobResult>>();
		HashMap<Integer, Boolean> mapFutures = new HashMap<Integer, Boolean>();

		while (!done) {

			requestWork = workRequestPort.newMessage();
			requestWork.writeObject(new JobRequest());
			requestWork.finish();

			// receive all tasks and create threads for them
			//System.out.println("No job requests " + noJobRequests);
			while (!pauseReceived || noJobRequests != 0) {
				ReadMessage reply = workReplyPort.receive();
				Object replyObj = reply.readObject();

				if (replyObj instanceof NoJob) {
					reply.finish();
					boolean validJob = false; 

					if (((NoJob)replyObj).type.equals(EnergyConst.terminate)) {
						System.out.println("termination received");
						pauseReceived = true;
						done = true;
						sendAll = true;
					}

					if (((NoJob)replyObj).type.equals(EnergyConst.pause)) {
						System.out.println("pause received");
						pauseReceived = true;
						if (noThreads == 0) {
							noThreads = futures.size();
						}
						System.out.println("no threads " + noThreads);
					}

					if (((NoJob)replyObj).type.equals(EnergyConst.wait)) {
						//System.out.println("waiting ... befor noJobRequests == noThreads");
						//System.out.println("no Job req " + noJobRequests);
						//System.out.println("no threads " + noThreads);
						if (futures.size() == mapFutures.size()) {
							futures = new ArrayList<Future<JobResult>>();
							mapFutures = new HashMap<Integer, Boolean>(); 
							while (((NoJob)replyObj).type.equals(EnergyConst.wait)) {
								System.out.println("waiting ...");
								//Thread.sleep(2000);
								requestWork = workRequestPort.newMessage();
								requestWork.writeObject(new JobRequest());
								requestWork.finish();
								reply = workReplyPort.receive();
								replyObj = reply.readObject();
								if (replyObj instanceof NoJob) {
									reply.finish();
								} else {
									noJobRequests = Integer.MAX_VALUE;
									noThreads = 0;
									validJob = true;
									break;
								}
							}
						} else {
							sendAll = true;
						}
					}
					if (!validJob) {
						break;
					}
				}
				Job job = (Job) replyObj;

				reply.finish();

				futures.add(executorService.submit(new HandleJob(factor, job)));
				noJobRequests--;
				if (noJobRequests != 0) { 
					requestWork = workRequestPort.newMessage();
					requestWork.writeObject(new JobRequest());
					requestWork.finish();
				}
			}
			noJobRequests = 0;
			int futuresSize = futures.size();
			while (noJobRequests == 0 && futuresSize != 0) {
				// send task results
				Iterator<Future<JobResult>> it = futures.iterator();
				while (it.hasNext()) {
					Future<JobResult> future = it.next();
					if (mapFutures.get(future.hashCode()) == null && (future.isDone() || sendAll)) {
						System.out.println("write job result");
						requestWork = workRequestPort.newMessage();
						JobResult jr = future.get();
						requestWork.writeObject(jr);
						requestWork.finish();
						noJobRequests++;
						mapFutures.put(future.hashCode(), true);
					}
				}
			}
			System.out.println("Number of slots empty " + noJobRequests);
			//System.out.println("done " + done);
			sendAll = false;
		}

		/* could print some worker stats? */				
		closePorts();
		myIbis.end();
		System.out.println("shutting down");
	}

	public static void main(String[] args) {
		try {
			new DAS4Worker(args[0], args[1], args[2], args[3]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void died(IbisIdentifier arg0) {
	}

	@Override
	public void electionResult(String arg0, IbisIdentifier arg1) {
	}

	@Override
	public void gotSignal(String arg0, IbisIdentifier arg1) {
		System.out.println("Lease expired! Closing ibis ...");
		try {
			myIbis.end();
			oneSelfShutdown();
		} catch (IOException ex) {
			Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
		}
		System.out.println("Lease expired! Exiting computation ...");
		System.exit(0);
	}

	@Override
	public void joined(IbisIdentifier arg0) {}

	@Override
	public void left(IbisIdentifier arg0) {}

	@Override
	public void poolClosed() {}

	@Override
	public void poolTerminated(IbisIdentifier arg0) {}

	protected void closePorts() throws IOException {
		workRequestPort.close();
		workReplyPort.close();
	}

	public void oneSelfShutdown() {      
		System.exit(0);
	}

	private class HandleJob implements Callable<JobResult> {

		double factor;
		Job job;

		HandleJob (double factor, Job job) {
			// if this is a simulation we need the speedfactor
			this.factor = factor;
			this.job = job;
		}

		@Override
		public JobResult call() throws Exception {
			if("/bin/sleep".equalsIgnoreCase(job.getExec())) {
				long et = (long)((double)Long.parseLong(job.args[0])/factor);
				job.args[0] = "" + et;
			}

			String cmd = job.getExec();
			for (int i = 0; i < job.args.length; i++) {
				cmd += " " + job.args[i];
			}

			System.out.println("Running job: " + cmd);

			long startTime = System.nanoTime();

			ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);

			/* modify current working directory for the ProcessBuilder
			 * so as to treat as relative path the mounted file system */
			try {
				String mountFolder = System.getenv("MOUNT_FOLDER");
				if (mountFolder != null && !"".equals(mountFolder)) {
					pb.directory(new File(mountFolder));
				}
			} catch (Exception ex) {
				System.err.println(ex.getLocalizedMessage());
			}
			/* end */
			pb.redirectErrorStream(true);
			Process shell = pb.start();

			InputStream shellIn = shell.getInputStream();
			int c;
			while ((c = shellIn.read()) != -1) {
				/*pull bytes from output stream of shell process
			         	  to avoid blocking it*/
			}
			shell.waitFor();

			long endTime = System.nanoTime();

			System.out.println("Runtime(s) " + ((double)(endTime - startTime) / 1000000000));
			return new JobResult(job.jobID, new JobStats(
					endTime - startTime));
		}

	}

}
