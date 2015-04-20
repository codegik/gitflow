package com.codegik.gitflow.command;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;



public class GitCommandExecutor extends CommandExecutor {
	private Commandline cmd;
	private Log log;


	public GitCommandExecutor(Log log) {
		this.log = log;
		this.cmd = new Commandline();
		if (StringUtils.isBlank(cmd.getExecutable())) {
			cmd.setExecutable("git" + (Os.isFamily(Os.FAMILY_WINDOWS) ? ".exe" : ""));
		}
	}


	@Override
	protected Commandline getCommandline() {
		return cmd;
	}


	@Override
	protected Log getLog() {
		return log;
	}
}
