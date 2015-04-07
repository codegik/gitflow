package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.DefaultGitFlowMojo;


/**
 * Start new release branch from develop
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-release", aggregator = true)
public class StartReleaseMojo extends DefaultGitFlowMojo {
	private String branchName;

    @Parameter( property = "version", required = true )
	private String version;


	@Override
	public void run() throws Exception {
		validadeVersion(getVersion());
		setBranchName(PREFIX_RELEASE + SEPARATOR + getVersion());

		Ref develop = findBranch(DEVELOP);

		if (develop == null) {
			develop = createBranch(DEVELOP, " Because does not exists");
		}

		createBranch(getBranchName());

		getLog().info("Updating pom version");
		ReleaseDescriptor descriptor = buildReleaseDescriptor();
		ReleaseEnvironment environment = buildDefaultReleaseEnvironment();
		descriptor.mapDevelopmentVersion(getProject().getArtifactId(), getVersion() + SUFFIX_RELEASE);
		descriptor.setDefaultDevelopmentVersion(getVersion() + SUFFIX_RELEASE);
		getReleaseManager().updateVersions(descriptor, environment, buildMavenProjects());

		getLog().info("Commiting changed files");
		commit("[GitFlow::start-release] Create release branch " + getBranchName());

		push("Pushing commit");
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			reset(DEVELOP);
			checkoutBranchForced(branchName);
			deleteBranch(getBranchName());
		} catch (Exception e1) {;}
		throw buildMojoException("ERROR", e);
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
