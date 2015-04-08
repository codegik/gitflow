package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;


/**
 * Start new development branch from release
 * The branch type must be feature or bugfix
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-development", aggregator = true)
public class StartDevelopmentMojo extends AbstractGitFlowMojo {

    @Parameter( property = "version", required = true )
	private String version;

    @Parameter( property = "branchType", required = true )
    private BranchType branchType;

    @Parameter( property = "branchName", required = true )
    private String branchName;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		validadeVersion(getVersion());

		setBranchName(getBranchType().toString() + SEPARATOR + getVersion() + SEPARATOR + getBranchName());

		gitFlow.checkoutBranch(PREFIX_RELEASE + SEPARATOR + getVersion());

		gitFlow.createBranch(getBranchName());

		gitFlow.push("Pushing branch " + getBranchName());
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			gitFlow.reset(DEVELOP);
			gitFlow.checkoutBranchForced(DEVELOP);
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


	public BranchType getBranchType() {
		return branchType;
	}


	public void setBranchType(BranchType branchType) {
		this.branchType = branchType;
	}

}
