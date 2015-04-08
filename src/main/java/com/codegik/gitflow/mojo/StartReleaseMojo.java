package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;


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


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		validadeVersion(getVersion());
		setBranchName(PREFIX_RELEASE + SEPARATOR + getVersion());

		Ref develop = gitFlow.findBranch(DEVELOP);

		if (develop == null) {
			develop = gitFlow.createBranch(DEVELOP, " Because does not exists");
		}

		gitFlow.createBranch(getBranchName());

		getLog().info("Updating pom version");
		ReleaseDescriptor descriptor 	= gitFlow.buildReleaseDescriptor();
		ReleaseEnvironment environment 	= gitFlow.buildDefaultReleaseEnvironment();
		descriptor.mapDevelopmentVersion(getProject().getArtifactId(), getVersion() + SUFFIX_RELEASE);
		descriptor.setDefaultDevelopmentVersion(getVersion() + SUFFIX_RELEASE);
		getReleaseManager().updateVersions(descriptor, environment, buildMavenProjects());

		getLog().info("Commiting changed files");
		gitFlow.commit("[GitFlow::start-release] Create release branch " + getBranchName());
		gitFlow.push("Pushing commit");
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			gitFlow.reset(DEVELOP);
			gitFlow.checkoutBranchForced(branchName);
			gitFlow.deleteBranch(getBranchName());
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
