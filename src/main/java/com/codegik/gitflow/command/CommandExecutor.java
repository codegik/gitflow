package com.codegik.gitflow.command;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.release.exec.ForkedMavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.apache.maven.shared.release.exec.TeeOutputStream;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;


public abstract class CommandExecutor {


	protected abstract Commandline getCommandline();


	protected abstract Log getLog();


	public String execute(final String... args) throws Exception {

		getLog().info(getCommandline().getExecutable() + " " + StringUtils.join(args, " "));

		getCommandline().clearArgs();
		getCommandline().addArguments(args);

        TeeOutputStream stdOut 	= new TeeOutputStream(System.out);
        TeeOutputStream stdErr 	= new TeeOutputStream(System.err);
		final int exitCode 		= ForkedMavenExecutor.executeCommandLine(getCommandline(), System.in, stdOut, stdErr);

		if (exitCode != 0) {
			throw new MavenExecutorException("Error, exit code: '" + exitCode + "'", exitCode, stdOut.toString(), stdErr.toString());
		}

		return stdOut.toString();
	}
}
