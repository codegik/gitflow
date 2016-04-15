package com.codegik.gitflow.mojo;

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.codegik.gitflow.core.impl.DefaultGitFlowMojo;


/**
 * Start new development branch from release
 * The branch type must be feature or bugfix
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-development", aggregator = true)
public class StartDevelopmentMojo extends DefaultGitFlowMojo {

    @Parameter( property = "fullBranchName", required = true )
    private String fullBranchName;
    

	@Override
	public void run() throws Exception {
		Map<String, String> branchInfo 	= getGitFlow().validateFullBranchName(getFullBranchName());
		String version  				= branchInfo.get("version");

		getGitFlow().validadeReleaseVersion(version);

		if (getGitFlow().findBranch(getFullBranchName()) != null) {
			throw new MojoExecutionException("The branch " + getFullBranchName() + " already exists!");
		}

		compileProject();

		getGitFlow().checkoutBranch(getGitFlow().buildReleaseBranchName(version));
		getGitFlow().createBranch(getFullBranchName());
		getGitFlow().pushBranch(getFullBranchName());
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			getGitFlow().reset(getGitFlow().getGitFlowPattern().getDevelopName());
			getGitFlow().checkoutBranchForced(getGitFlow().getGitFlowPattern().getDevelopName());
			getGitFlow().deleteLocalBranch(getFullBranchName());
		} catch (Exception e1) {;}
		throw new MojoExecutionException("ERROR", e);
	}


	public String getFullBranchName() {
		return fullBranchName;
	}


	public void setFullBranchName(String fullBranchName) {
		this.fullBranchName = fullBranchName;
	}

}
