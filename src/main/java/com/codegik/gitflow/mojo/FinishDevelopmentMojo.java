package com.codegik.gitflow.mojo;

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.core.MergeGitFlow;
import com.codegik.gitflow.core.impl.DefaultGitFlowMojo;


/**
 * Finish development branch and merge into release
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-development", aggregator = true)
public class FinishDevelopmentMojo extends DefaultGitFlowMojo {

    @Parameter( property = "fullBranchName", required = true )
    private String branchName;

    @Parameter( property = "keepBranch", defaultValue = "true" )
    private Boolean keepBranch;
    

	@Override
	public void run() throws Exception {
		Map<String, String> branchInfo 	= getGitFlow().validateFullBranchName(getBranchName());
		String releaseBranch 			= getGitFlow().buildReleaseBranchName(branchInfo.get("version"));

		getGitFlow().checkoutBranch(releaseBranch);

		Ref ref = getGitFlow().findBranch(getBranchName());

		if (ref == null) {
			throw new MojoExecutionException("The fullBranchName " + getBranchName() + " not found!");
		}

		MergeGitFlow mergeGitFlow = new MergeGitFlow();
		mergeGitFlow.setBranchName(releaseBranch);
		mergeGitFlow.setErrorMessage("finish-development -DfullBranchName=" + getBranchName());
		mergeGitFlow.setTargetRef(ref);
		mergeGitFlow.setIgnoringFilesStage(Stage.OURS);
		mergeGitFlow.addIgnoringFiles(getGitFlow().getGitFlowPattern().getPomFileName());

		getGitFlow().merge(mergeGitFlow);
		compileProject();
		getGitFlow().push();

		if (!keepBranch) {
			getGitFlow().deleteRemoteBranch(getBranchName());
		}
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		throw new MojoExecutionException("ERROR", e);
	}


	public String getBranchName() {
		return branchName;
	}


	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

}
