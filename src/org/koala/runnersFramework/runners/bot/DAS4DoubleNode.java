package org.koala.runnersFramework.runners.bot;

import java.util.ArrayList;

public class DAS4DoubleNode extends DAS4Node {
	String name1, name2;
	Integer port1, port2;

	public DAS4DoubleNode(String name1, String name2, String rpdu,
			Integer port1, Integer port2) {
		super();
		this.name1 = name1;
		this.name2 = name2;
		this.rpdu = rpdu;
		this.port1 = port1;
		this.port2 = port2;
		this.name = this.name1 + "-" + this.name2;
	}

	@Override 
	void addPower(long time, int noTasks, int pw) {
		
			if (power.get(noTasks) == null) {
				power.put(noTasks, new ArrayList<Integer>());
				timestamp.put(noTasks, new ArrayList<Long>());
			}
			power.get(noTasks).add(pw);
			timestamp.get(noTasks).add(time);
			System.out.println("pw;"+name1 + "-" + name2 +"; " + noTasks + "; " + pw);
		
	}
	
	@Override
	boolean isThisNode(String node) {
		if (node.equals(name1)|| node.equals(name2)) {
			return true;
		} else {
			return false;
		}
			
	}

}
