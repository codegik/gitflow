package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import com.codegik.gitflow.DefaultGitFlowMojo;


/**
 * Start new development branch from release
 * The branch type must be feature or bugfix
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-development", aggregator = true)
public class StartDevelopmentMojo extends DefaultGitFlowMojo {

    @Parameter( property = "version", required = true )
	private String version;

    @Parameter( property = "branchType", required = true )
    private BranchType branchType;

    @Parameter( property = "branchName", required = true )
    private String branchName;


	@Override
	public void run() throws Exception {
		validadeVersion(getVersion());

		setBranchName(getBranchType().toString() + SEPARATOR + getVersion() + SEPARATOR + getBranchName());

		checkoutBranch(PREFIX_RELEASE + SEPARATOR + getVersion());

		createBranch(getBranchName());

		push("Pushing branch " + getBranchName());
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			reset(DEVELOP);
			checkoutBranchForced(DEVELOP);
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


	public BranchType getBranchType() {
		return branchType;
	}


	public void setBranchType(BranchType branchType) {
		this.branchType = branchType;
	}

}
