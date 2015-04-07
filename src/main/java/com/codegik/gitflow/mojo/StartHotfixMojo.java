package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import com.codegik.gitflow.DefaultGitFlowMojo;


/**
 * Start new hotfix branch from master
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-hotfix", aggregator = true)
public class StartHotfixMojo extends DefaultGitFlowMojo {

	@Parameter( property = "branchName", required = true )
	private String branchName;


	@Override
	public void run() throws Exception {
		if (!getGit().getRepository().getBranch().toLowerCase().equals(MASTER)) {
			throw buildMojoException("You must be on master branch for execute this goal!");
		}

		setBranchName(PREFIX_HOTFIX + SEPARATOR + getBranchName());

		createBranch(getBranchName());

		updatePomVersion(getProject().getVersion() + SUFFIX);

		getLog().info("Commiting changed files");
		commit("[GitFlow::start-hotfix] Create release branch " + getBranchName());

        push("Pushing commit");
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rollbacking all changes");
			reset(MASTER);
			checkoutBranchForced(MASTER);
			deleteBranch(getBranchName());
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
