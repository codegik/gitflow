package com.codegik.gitflow;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseManager;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.lib.Ref;


public abstract class AbstractGitFlowMojo extends AbstractMojo {
	protected static final String MASTER = "master";
	protected static final String DEVELOP = "develop";
	protected static final String PREFIX_RELEASE = "release";
	protected static final String PREFIX_HOTFIX = "hotfix";
	protected static final String SUFFIX = "-SNAPSHOT";
	protected static final String SEPARATOR = "/";
	private Git git;

    @Component
    private MavenProject project;

    @Component
    private ReleaseManager releaseManager;


    public abstract void run() throws Exception;

    public abstract void rollback(Exception e) throws MojoExecutionException;


    public void execute() throws MojoExecutionException, MojoFailureException {
    	try {
    		run();
    	} catch (Exception e) {
    		rollback(e);
    	}
    }


	protected Git getGit() throws Exception {
		if (git == null) {
			git = Git.open(new File("."));
		}

		return git;
	}


	protected Ref findBranch(String branch) throws Exception {
		for (Ref b : getGit().branchList().setListMode(ListMode.ALL).call()) {
			if (branch.equals(b.getName().toLowerCase().replace("refs/heads/", ""))) {
				return b;
			}
		}

		return null;
	}

	public MavenProject getProject() {
		return project;
	}


	public void setProject(MavenProject project) {
		this.project = project;
	}


	public ReleaseManager getReleaseManager() {
		return releaseManager;
	}


	public void setReleaseManager(ReleaseManager releaseManager) {
		this.releaseManager = releaseManager;
	}
}
