package com.codegik.gitflow.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.StringUtils;

import com.codegik.gitflow.command.CommandExecutor;
import com.codegik.gitflow.command.MvnCommandExecutor;
import com.codegik.gitflow.core.impl.DefaultGitFlow;


public abstract class GitFlowMojo extends AbstractMojo {

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    @Parameter( defaultValue = "${settings}", readonly = true )
    private Settings settings;
    
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;
    
    @Parameter( property = "skipTests" )
    private Boolean skipTests;

    private CommandExecutor mvnExecutor;

    public abstract DefaultGitFlow getGitFlow();
    
    public abstract void run() throws Exception;

    public abstract void rollback(Exception e) throws MojoExecutionException;
    
    
    public void execute() throws MojoExecutionException, MojoFailureException {
    	mvnExecutor = new MvnCommandExecutor(getLog());

    	try {
    		run();
    		getLog().info("DONE");
    	} catch (Exception e) {
    		rollback(e);
    	}
    }


	protected String updatePomVersion(String newVersion) throws Exception {
		getLog().info("Bumping version of files to " + newVersion);
		return mvnExecutor.execute("versions:set", "-DgenerateBackupPoms=false", "-DnewVersion=" + newVersion, "-DskipTests");
	}


	public String compileProject() throws Exception {
		getLog().info("Compiling project...");
		List<String> args = new ArrayList<String>();
		args.add("clean");
		args.add("install");

		if (Boolean.TRUE.equals(getSkipTests())) {
			args.add("-DskipTests");
		}
        
        if (Boolean.TRUE.equals(getSettings().isOffline())) {
			args.add("-o");
		}
		
		if (getSettings().getActiveProfiles() != null && getSettings().getActiveProfiles().size() > 0) {
			args.add("-P");
			args.add(StringUtils.join(getSettings().getActiveProfiles().iterator(), ","));
		}

		return mvnExecutor.execute(args.toArray(new String[args.size()]));
	}


	public MavenProject getProject() {
		return project;
	}


	public void setProject(MavenProject project) {
		this.project = project;
	}

	public Boolean getSkipTests() {
		return skipTests;
	}

	public void setSkipTests(Boolean skipTests) {
		this.skipTests = skipTests;
	}

	public Settings getSettings() {
		return settings;
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	public CommandExecutor getMvnExecutor() {
		return mvnExecutor;
	}

	public void setMvnExecutor(CommandExecutor mvnExecutor) {
		this.mvnExecutor = mvnExecutor;
	}

	public MavenSession getSession() {
		return session;
	}

}
