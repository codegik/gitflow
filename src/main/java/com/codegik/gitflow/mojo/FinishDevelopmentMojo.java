package com.codegik.gitflow.mojo;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.DefaultGitFlowMojo;


/**
 * Finish development branch and merge into release
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-development", aggregator = true)
public class FinishDevelopmentMojo extends DefaultGitFlowMojo {

    @Parameter( property = "fullBranchName", required = true )
    private String branchName;

    @Parameter( property = "deleteBranchAfter", defaultValue = "false" )
    private Boolean deleteBranchAfter;


	@Override
	public void run() throws Exception {
		String[] branchInfo 	= validateFullBranchName(getBranchName());
		String releaseBranch 	= PREFIX_RELEASE + SEPARATOR + branchInfo[1];

		checkoutBranch(releaseBranch);

		Ref ref = findBranch(getBranchName());

		if (ref == null) {
			throw buildMojoException("The fullBranchName " + getBranchName() + " not found!");
		}

		MergeResult merge = merge(ref);

		if (!merge.getMergeStatus().isSuccessful()) {
			throw buildMojoException("The merge has conflicts, please try resolve manually! [from " + releaseBranch + " to " + getBranchName() + "]");
		}

		push("Pushing merge");

		if (deleteBranchAfter) {
			deleteBranch(getBranchName());
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
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			reset(DEVELOP);
			checkoutBranchForced(DEVELOP);
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
