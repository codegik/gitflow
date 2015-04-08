package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;


/**
 * Start new hotfix branch from master
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-hotfix", aggregator = true)
public class StartHotfixMojo extends AbstractGitFlowMojo {

	@Parameter( property = "branchName", required = true )
	private String branchName;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		if (!gitFlow.getBranch().toLowerCase().equals(MASTER)) {
			throw buildMojoException("You must be on branch master for execute this goal!");
		}

		setBranchName(PREFIX_HOTFIX + SEPARATOR + getBranchName());

		gitFlow.createBranch(getBranchName());

		String newVersion = gitFlow.incrementVersion(getProject().getVersion());

		updatePomVersion(newVersion + SUFFIX);

		getLog().info("Commiting changed files");
		gitFlow.commit("[GitFlow::start-hotfix] Create release branch " + getBranchName());
		gitFlow.push("Pushing commit");
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rollbacking all changes");
			gitFlow.reset(MASTER);
			gitFlow.checkoutBranchForced(MASTER);
			gitFlow.deleteBranch(getBranchName());
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
