package org.koala.runnersFramework.runners.bot;

import java.io.IOException;
import java.util.ArrayList;

public class EnergyStats implements Runnable {
	ArrayList<DAS4Node> nodes;
	ArrayList<PDUThread> pduThreads = new ArrayList<PDUThread>();
	ArrayList<Thread> threads = new ArrayList<Thread>();
	
	EnergyStats(ArrayList<DAS4Node> nodes) {
		this.nodes = nodes;
	}
	
	@Override	
	public void run() {
		System.out.println("Start measuring energy for: " );
		
		for (int i = 0; i < nodes.size(); i++) {
			System.out.println("node " + nodes.get(i));
			PDUThread pduThread = new PDUThread(nodes.get(i));
			pduThreads.add(pduThread);
			Thread t = new Thread(pduThread);
			t.start();
			threads.add(t);
		}
		try {
			for (int i = 0; i < threads.size(); i++) {
				threads.get(i).join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void kill() {
		for (int i = 0; i < threads.size(); i++) {
			pduThreads.get(i).kill();	
		}
	}
	
	public void setNoTasks(String node, int noTasks) {
		int nodeIndex = findNode(node);
		//System.out.println("set no tasks " + pduThreads.get(nodeIndex).node.name + " to " + noTasks);
		pduThreads.get(nodeIndex).setNoTasks(noTasks);	
	}
	
	public void addPauseToEnergyMeas(String node) {
		int nodeIndex = findNode(node);
		pduThreads.get(nodeIndex).addPauseToEnergyMeas();
		
	}
	
	public int findNode(String node) {
		while (pduThreads.size()!=nodes.size()) {}
		
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i) instanceof DAS4DoubleNode) {
				if (((DAS4DoubleNode) nodes.get(i)).name1.equals(node) ||
						((DAS4DoubleNode) nodes.get(i)).name2.equals(node)) {
					return i;
				}
			} else if (nodes.get(i).name.equals(node)) {
				return i;
			}
		}
		
		return -1;
	}
	
	class PDUThread implements Runnable {
		RacktivityEnergyMeasurement racktivity;
		DAS4Node node;
		int noTasks;
		boolean done;
		
		PDUThread(DAS4Node node) {
			this.node = node;
			this.racktivity = new RacktivityEnergyMeasurement(node.rpdu);
			this.done = false;
			this.noTasks = 0;
		};
		
		@Override
		public void run() {
			while (!done) {
				try {
					ArrayList<Short> power = racktivity.getPower();
					long time = System.currentTimeMillis();
					if (!power.isEmpty()) {
						//System.out.println("power");
						//System.out.println(power);
						if (node instanceof DAS4DoubleNode) {
							DAS4DoubleNode dNode = (DAS4DoubleNode) node;
							int totalPower = power.get(dNode.port1-1) + power.get(dNode.port2-1);
							node.addPower(time, noTasks, totalPower);
						} else {
							node.addPower(time, noTasks, power.get(node.port-1));
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		public void kill() {
			System.out.println("pdu thread kill received");
			done = true;
		}
		
		public void setNoTasks(int noTasks) {
			this.noTasks = noTasks;
		}
		
		public void addPauseToEnergyMeas() {
			node.addPower(0, noTasks, 0);
			System.out.println("add power 0");
		}
		
	}
	
}
