package com.codegik.gitflow.mojo;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;
import com.codegik.gitflow.MergeGitFlow;


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
		String[] branchInfo 	= validateFullBranchName(getBranchName());
		String releaseBranch 	= PREFIX_RELEASE + SEPARATOR + branchInfo[1];

		gitFlow.checkoutBranch(releaseBranch);

		Ref ref = gitFlow.findBranch(getBranchName());

		if (ref == null) {
			throw buildMojoException("The fullBranchName " + getBranchName() + " not found!");
		}

		MergeGitFlow mergeGitFlow = new MergeGitFlow();
		mergeGitFlow.setBranchName(releaseBranch);
		mergeGitFlow.setErrorMessage("finish-development -DfullBranchName=" + getBranchName());
		mergeGitFlow.setTargetRef(ref);
		mergeGitFlow.setIgnoringFilesStage(Stage.OURS);

		gitFlow.merge(mergeGitFlow);
		compileProject("clean install", getSkipTests());
		gitFlow.push("Pushing merge");

		if (deleteBranchAfter) {
			gitFlow.deleteRemoteBranch(getBranchName());
		}
	}


	private List<String> branchTypeToArray() {
		List<String> values = new ArrayList<String>();

		for (BranchType type : BranchType.values()) {
			values.add(type.name());
		}

		return values;
	}


	private String[] validateFullBranchName(String branchName) throws Exception {
		String errorMessage = "The fullBranchName must be <branchType=[feature|bugfix]>/<releaseVersion>/<branchName>. EX: feature/1.1.0/issue3456";
		String[] pattern 	= branchName.split("/");

		if (pattern.length != 3) {
			throw buildMojoException(errorMessage);
		}

		if (!branchTypeToArray().contains(pattern[0])) {
			throw buildMojoException(errorMessage);
		}

		return pattern;
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			gitFlow.reset(DEVELOP);
			gitFlow.checkoutBranchForced(DEVELOP);
		} catch (Exception e1) {;}
		throw buildMojoException("ERROR", e);
	}


	public String getBranchName() {
		return branchName;
	}


	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

}
