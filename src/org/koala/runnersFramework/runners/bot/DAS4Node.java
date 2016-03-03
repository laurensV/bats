package org.koala.runnersFramework.runners.bot;

import java.util.ArrayList;
import java.util.HashMap;

public class DAS4Node {

	String name;
	String rpdu;
	Integer port;
	String args;


	// number of tasks in parallel to the power consumption 
	HashMap<Integer, ArrayList<Integer>> power = 
			new HashMap<Integer, ArrayList<Integer>>();
	HashMap<Integer, ArrayList<Long>> timestamp = 
			new HashMap<Integer, ArrayList<Long>>();

	DAS4Node(){}

	DAS4Node(String name, String rpdu, Integer port, String args) {
		this.name = name;
		this.rpdu = rpdu;
		this.port = port;
		this.args = args;
	}

	void addPower(long time, int noTasks, int pw) {

		if (power.get(noTasks) == null) {
			power.put(noTasks, new ArrayList<Integer>());
			timestamp.put(noTasks, new ArrayList<Long>());
		}
		power.get(noTasks).add(pw);
		timestamp.get(noTasks).add(time);
		System.out.println("pw; "+ name + "; " + noTasks + "; " + pw);

	}

	boolean isThisNode(String node) {
		if (node.equals(name)) {
			return true;
		} else {
			return false;
		}
	}



}
