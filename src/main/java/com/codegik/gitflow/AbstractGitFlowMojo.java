package com.codegik.gitflow;

import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.MavenExecutor;


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
	public static final Pattern POM_SNAPSHOT_VERSION_PATTERN = Pattern.compile("([0-9]{1,})\\.([0-9]{1,})\\.([0-9]{1,2})(-SNAPSHOT)");
	public static final Pattern RELEASE_VERSION_PATTERN = Pattern.compile("([0-9]{1,})\\.([0-9]{1,})");
	public static final String SEPARATOR = "/";
	public static final String FILE_POM = "pom.xml";

    @Component
    private MavenProject project;

    @Component
    private MavenExecutor mavenExecutor;

    @Parameter( property = "password" )
    private String password;

    @Parameter( property = "username" )
    private String username;

    @Parameter( property = "skipTests" )
    private Boolean skipTests;


    public abstract void run(GitFlow gitFlow) throws Exception;

    public abstract void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException;


    public void execute() throws MojoExecutionException, MojoFailureException {
    	GitFlow gitFlow = new GitFlow(getLog(), getUsername(), getPassword());

    	if (gitFlow.getCredentialsProvider() == null) {
    		throw buildMojoException("Please set your credentials: -Dusername=<username> -Dpassword=<password>");
    	}

    	try {
    		run(gitFlow);
    		getLog().info("DONE");
    	} catch (Exception e) {
    		rollback(gitFlow, e);
    	}
    }


	protected MojoExecutionException buildMojoException(String errorMessage) {
		getLog().error(errorMessage);
		return new MojoExecutionException(errorMessage);
	}


	protected MojoExecutionException buildMojoException(String errorMessage, Exception e) {
		getLog().error(errorMessage);
		return new MojoExecutionException(errorMessage, e);
	}


	protected ReleaseResult updatePomVersion(String newVersion) throws Exception {
		getLog().info("Bumping version of files to " + newVersion);
		ReleaseResult releaseResult = new ReleaseResult();
		ReleaseEnvironment releaseEnvironment = new DefaultReleaseEnvironment();
		String params = getSkipTests() ? " -DskipTests" : null;
		params += " -DgenerateBackupPoms=false";
		params += " -DnewVersion=" + newVersion;

		getMavenExecutor().executeGoals(getProject().getBasedir(), "versions:set", releaseEnvironment, false, params, releaseResult);

		return releaseResult;
	}


	public ReleaseResult compileProject(String goals, Boolean skipTests) throws Exception {
		getLog().info("Compiling project: " + goals + " -DskipTests=" + skipTests);
		ReleaseResult releaseResult = new ReleaseResult();
		ReleaseEnvironment releaseEnvironment = new DefaultReleaseEnvironment();

		getMavenExecutor().executeGoals(getProject().getBasedir(), goals, releaseEnvironment, false, (skipTests ? " -DskipTests" : null), releaseResult);

		return releaseResult;
	}


	public MavenProject getProject() {
		return project;
	}


	public void setProject(MavenProject project) {
		this.project = project;
	}


	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Boolean getSkipTests() {
		return skipTests;
	}

	public void setSkipTests(Boolean skipTests) {
		this.skipTests = skipTests;
	}

	public MavenExecutor getMavenExecutor() {
		return mavenExecutor;
	}

	public void setMavenExecutor(MavenExecutor mavenExecutor) {
		this.mavenExecutor = mavenExecutor;
	}

}
