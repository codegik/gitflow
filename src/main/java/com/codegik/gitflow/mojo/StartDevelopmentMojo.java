package com.codegik.gitflow.mojo;

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;
import com.codegik.gitflow.mojo.util.BranchUtil;


/**
 * Start new development branch from release
 * The branch type must be feature or bugfix
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-development", aggregator = true)
public class StartDevelopmentMojo extends AbstractGitFlowMojo {

    @Parameter( property = "fullBranchName", required = true )
    private String fullBranchName;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		Map<String, String> branchInfo 	= BranchUtil.validateFullBranchName(getFullBranchName());
		String version  				= branchInfo.get("version");

		gitFlow.validadeReleaseVersion(version);

		if (gitFlow.findBranch(getFullBranchName()) != null) {
			throw new MojoExecutionException("The branch " + getFullBranchName() + " already exists!");
		}

		compileProject();

		gitFlow.checkoutBranch(BranchUtil.buildReleaseBranchName(version));
		gitFlow.createBranch(getFullBranchName());
		gitFlow.push("Pushing branch " + getFullBranchName());
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			gitFlow.reset(DEVELOP);
			gitFlow.checkoutBranchForced(DEVELOP);
			gitFlow.deleteLocalBranch(getFullBranchName());
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
