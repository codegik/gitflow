package com.codegik.gitflow;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Ref;

import edu.emory.mathcs.backport.java.util.Arrays;


/**
 * Start new release branch from develop
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-release", aggregator = true)
public class StartReleaseMojo extends AbstractMojo {
	private static final String DEVELOP = "develop";
	private static final String PREFIX = "release";
	private static final String SUFFIX = "-SNAPSHOT";
	private static final String SEPARATOR = "/";
	private Git git;

    @Parameter( property = "version", required = true )
	private String version;

    @Component
    private MavenProject project;

    @Component
    private ReleaseManager releaseManager;



	@SuppressWarnings("unchecked")
	public void execute() throws MojoExecutionException, MojoFailureException {
		String branchName = PREFIX + SEPARATOR + version;

		version = version.split(".").length < 2 ? version + ".000" : version;

		try {

			getLog().info("Looking for develop");
			Ref develop = findBranch(DEVELOP);

			if (develop == null) {
				getLog().info("Develop does not exists, creating branch develop");
				develop = getGit().checkout().setCreateBranch(true).setName(DEVELOP).call();
			}

			getLog().info("Creating branch " + branchName);
			getGit().checkout().setCreateBranch(true).setName(branchName).call();

			getLog().info("Updating pom version");
			ReleaseDescriptor descriptor = new ReleaseDescriptor();
			ReleaseEnvironment environment = new DefaultReleaseEnvironment();
			descriptor.mapDevelopmentVersion(getProject().getArtifactId(), version + SUFFIX);
			descriptor.setDefaultDevelopmentVersion(version + SUFFIX);
			getReleaseManager().updateVersions(descriptor, environment, Arrays.asList(new MavenProject[]{project}));

			getLog().info("Commiting changed files");
			git.add().addFilepattern(".").call();
            git.commit().setMessage("[GitFlow::start-release] Create release branch " + branchName).call();

            getLog().info("Pushing commit");
            git.push().call();

            getLog().info("DONE");

		} catch (Exception e) {
			try {
				getLog().error(e.getMessage());
				getLog().info("Rollbacking all changes");
				getGit().reset().setMode(ResetType.HARD).setRef(DEVELOP).call();
				getGit().checkout().setCreateBranch(false).setForce(true).setName(DEVELOP).call();
				getGit().branchDelete().setForce(true).setBranchNames(branchName).call();
			} catch (Exception e1) {;}
			throw new MojoExecutionException("ERROR", e);
		}
	}


	private Git getGit() throws Exception {
		if (git == null) {
			git = Git.open(new File("."));
		}

		return git;
	}


	private Ref findBranch(String branch) throws Exception {
		for (Ref b : getGit().branchList().setListMode(ListMode.ALL).call()) {
			if (branch.equals(b.getName().toLowerCase().replace("refs/heads/", ""))) {
				return b;
			}
		}

		return null;
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
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
