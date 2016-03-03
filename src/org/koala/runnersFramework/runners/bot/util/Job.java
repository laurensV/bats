package org.koala.runnersFramework.runners.bot.util;

import java.util.HashMap;

public class Job {
	
	String id;
	/*list of runtimes per machine type*/
	HashMap<String,Long> runtime;
	
    Job(HashMap<String,Long>runtime, String id) {
    	this.runtime = runtime; 
    }
}
