package com.codegik.gitflow.mojo;

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;
import com.codegik.gitflow.MergeGitFlow;
import com.codegik.gitflow.mojo.util.BranchUtil;


/**
 * Finish development branch and merge into release
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-development", aggregator = true)
public class FinishDevelopmentMojo extends AbstractGitFlowMojo {

    @Parameter( property = "fullBranchName", required = true )
    private String branchName;

    @Parameter( property = "deleteBranchAfter", defaultValue = "false" )
    private Boolean deleteBranchAfter;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		Map<String, String> branchInfo 	= BranchUtil.validateFullBranchName(getBranchName());
		String releaseBranch 			= BranchUtil.buildReleaseBranchName(branchInfo.get("version"));

		gitFlow.checkoutBranch(releaseBranch);

		Ref ref = gitFlow.findBranch(getBranchName());

		if (ref == null) {
			throw new MojoExecutionException("The fullBranchName " + getBranchName() + " not found!");
		}

		MergeGitFlow mergeGitFlow = new MergeGitFlow();
		mergeGitFlow.setBranchName(releaseBranch);
		mergeGitFlow.setErrorMessage("finish-development -DfullBranchName=" + getBranchName());
		mergeGitFlow.setTargetRef(ref);
		mergeGitFlow.setIgnoringFilesStage(Stage.OURS);

		gitFlow.merge(mergeGitFlow);
		compileProject();
		gitFlow.push("Pushing merge");

		if (deleteBranchAfter) {
			gitFlow.deleteRemoteBranch(getBranchName());
		}
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		throw new MojoExecutionException("ERROR", e);
	}


	public String getBranchName() {
		return branchName;
	}


	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

}
