package com.codegik.gitflow;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Ref;

import edu.emory.mathcs.backport.java.util.Arrays;


/**
 * Start new release branch from develop
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-release", aggregator = true)
public class StartReleaseMojo extends AbstractGitFlowMojo {
	private String branchName;

    @Parameter( property = "version", required = true )
	private String version;


	@SuppressWarnings("unchecked")
	public void run() throws Exception {
		setBranchName(PREFIX_RELEASE + SEPARATOR + getVersion());
		setVersion(getVersion().split(".").length < 2 ? getVersion() + ".000" : getVersion());

		getLog().info("Looking for develop");
		Ref develop = findBranch(DEVELOP);

		if (develop == null) {
			getLog().info("Develop does not exists, creating branch develop");
			develop = getGit().checkout().setCreateBranch(true).setName(DEVELOP).call();
		}

		getLog().info("Creating branch " + getBranchName());
		getGit().checkout().setCreateBranch(true).setName(getBranchName()).call();

		getLog().info("Updating pom version");
		ReleaseDescriptor descriptor = new ReleaseDescriptor();
		ReleaseEnvironment environment = new DefaultReleaseEnvironment();
		descriptor.mapDevelopmentVersion(getProject().getArtifactId(), getVersion() + SUFFIX);
		descriptor.setDefaultDevelopmentVersion(getVersion() + SUFFIX);
		getReleaseManager().updateVersions(descriptor, environment, Arrays.asList(new MavenProject[]{getProject()}));

		getLog().info("Commiting changed files");
		getGit().add().addFilepattern(".").call();
		getGit().commit().setMessage("[GitFlow::start-release] Create release branch " + getBranchName()).call();

        getLog().info("Pushing commit");
        getGit().push().call();

        getLog().info("DONE");
	}


	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			getGit().reset().setMode(ResetType.HARD).setRef(DEVELOP).call();
			getGit().checkout().setCreateBranch(false).setForce(true).setName(DEVELOP).call();
			getGit().branchDelete().setForce(true).setBranchNames(getBranchName()).call();
		} catch (Exception e1) {;}
		throw new MojoExecutionException("ERROR", e);
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}


	public String getBranchName() {
		return branchName;
	}


	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

}
