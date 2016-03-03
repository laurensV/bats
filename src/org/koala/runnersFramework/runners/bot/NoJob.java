package org.koala.runnersFramework.runners.bot;

import java.util.List;

public class NoJob extends Job {
	private static final long serialVersionUID = 1L;
	String type = "";

	public NoJob(List<String> args2, String executable, String jobID) {
		super(args2, executable, jobID);
	}

	public NoJob() {
		super();
	}
	
	public NoJob(String type) {
		super();
		this.type = type;
	}

}
