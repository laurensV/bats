package org.koala.runnersFramework.runners.bot;

import java.util.ArrayList;
import java.util.HashMap;

public final class EnergyConst {

	//node, rpdu, ports
	static final int maxJobsPerNode = 200;
	static final int[] maxJobsPerNodeList = {4, 5, 10, 16, 20, 25, 32, 40};

	static final String terminate = "terminate";
	static final String pause = "pause";
	static final String wait = "wait";

	static ArrayList<DAS4DoubleNode> doubleNodes = new ArrayList<DAS4DoubleNode>();
	static ArrayList<DAS4Node> fatNodes = new ArrayList<DAS4Node>();
	
	static final String clusterAliasDoubleNodes = "double";
	static final String clusterAliasFatNodes = "fat";
	
	static HashMap<String, String> allNodes = new HashMap<String, String>();

	static {
		doubleNodes.add(new DAS4DoubleNode("node027", "node028", "rpdu04", 3, 4));
		doubleNodes.add(new DAS4DoubleNode("node059", "node060", "rpdu01", 3, 4));
		
		doubleNodes.add(new DAS4DoubleNode("node055", "node056", "rpdu01", 7, 8));
		
		
		fatNodes.add(new DAS4Node("node080", "rpdu06", 3, "-l fat,m_type=sandybridge"));
		fatNodes.add(new DAS4Node("node078", "rpdu06", 2, "-l fat,m_type=sandybridge"));
		fatNodes.add(new DAS4Node("node081", "rpdu06", 4, "-l fat,m_type=sandybridge"));
		fatNodes.add(new DAS4Node("node082", "rpdu05", 1, "-l fat,m_type=sandybridge"));
		
		
		
		fatNodes.add(new DAS4Node("node085", "rpdu05", 4, "-l fat,m_type=sandybridge"));
		
		fatNodes.add(new DAS4Node("node079", "rpdu06", 1, "-l fat,m_type=sandybridge"));
		
		
		
		fatNodes.add(new DAS4Node("node083", "rpdu05", 2, "-l fat,m_type=sandybridge"));
		fatNodes.add(new DAS4Node("node084", "rpdu05", 3, "-l fat,m_type=sandybridge"));
		fatNodes.add(new DAS4Node("node085", "rpdu05", 4, "-l fat,m_type=sandybridge"));
		
		for (DAS4DoubleNode dNode: doubleNodes) {
			allNodes.put(dNode.name1, clusterAliasDoubleNodes);
			allNodes.put(dNode.name2, clusterAliasDoubleNodes);
		}
		for (DAS4Node node: fatNodes) {
			allNodes.put(node.name, clusterAliasFatNodes);
		}

	}

	public static DAS4DoubleNode isPartOfDoubleNode(String node) {
		for (int i = 0; i < doubleNodes.size(); i++) {
			if (node.equals(doubleNodes.get(i).name1) || 
					node.equals(doubleNodes.get(i).name2))
				return doubleNodes.get(i);
		}
		return null;
	}
}

