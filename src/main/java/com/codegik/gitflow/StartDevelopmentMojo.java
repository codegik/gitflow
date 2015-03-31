package com.codegik.gitflow;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.ResetCommand.ResetType;


/**
 * Start new development branch from release
 * The branch type must be feature or bugfix
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-development", aggregator = true)
public class StartDevelopmentMojo extends AbstractGitFlowMojo {
	public enum BranchType { feature, bugfix }

    @Parameter( property = "version", required = true )
	private String version;

    @Parameter( property = "branchType", required = true )
    private BranchType branchType;

    @Parameter( property = "branchName", required = true )
    private String branchName;


	public void run() throws Exception {
		setBranchName(branchType.toString() + SEPARATOR + getVersion() + SEPARATOR + getBranchName());

		getLog().info("Checkout into " + PREFIX_RELEASE + SEPARATOR + getVersion());
		getGit().checkout().setCreateBranch(false).setName(PREFIX_RELEASE + SEPARATOR + getVersion()).call();

		getLog().info("Creating branch " + getBranchName());
		getGit().checkout().setCreateBranch(true).setName(getBranchName()).call();

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


	public BranchType getBranchType() {
		return branchType;
	}


	public void setBranchType(BranchType branchType) {
		this.branchType = branchType;
	}

}
