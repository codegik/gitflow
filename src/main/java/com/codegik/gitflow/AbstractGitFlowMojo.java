package com.codegik.gitflow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.StringUtils;

import com.codegik.gitflow.command.CommandExecutor;
import com.codegik.gitflow.command.GitCommandExecutor;
import com.codegik.gitflow.command.MvnCommandExecutor;
import com.codegik.gitflow.mojo.util.GitFlow;


public abstract class AbstractGitFlowMojo extends AbstractMojo {

	public enum BranchType { feature, bugfix }

	protected static final String ORIGIN = "origin";
	protected static final String MASTER = "master";
	protected static final String DEVELOP = "develop";
	protected static final String SUFFIX_RELEASE = ".0";
	public static final String PREFIX_HOTFIX = "hotfix";
	public static final String PREFIX_TAG = "refs/tags";
	public static final String PREFIX_RELEASE = "release";
	public static final Pattern TAG_VERSION_PATTERN = Pattern.compile("([0-9]{1,})\\.([0-9]{1,})\\.([0-9]{1,})");
	public static final Pattern RELEASE_VERSION_PATTERN = Pattern.compile("([0-9]{1,})\\.([0-9]{1,})");
	public static final String SEPARATOR = "/";
	public static final String FILE_POM = "pom.xml";

    @Component
    private MavenProject project;

    @Component
    private Settings settings;

    @Parameter( property = "skipTests" )
    private Boolean skipTests;

    private CommandExecutor mvnExecutor;
    private CommandExecutor gitExecutor;


    public abstract void run(GitFlow gitFlow) throws Exception;

    public abstract void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException;


    public void execute() throws MojoExecutionException, MojoFailureException {
    	mvnExecutor 	= new MvnCommandExecutor(getLog());
    	gitExecutor 	= new GitCommandExecutor(getLog());
    	GitFlow gitFlow = new GitFlow(getLog(), gitExecutor);

    	try {
    		run(gitFlow);
    		getLog().info("DONE");
    	} catch (Exception e) {
    		rollback(gitFlow, e);
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

		if (getSettings().getActiveProfiles() != null) {
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

}
