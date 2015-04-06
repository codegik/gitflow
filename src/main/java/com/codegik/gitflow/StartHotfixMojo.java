package com.codegik.gitflow;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.ResetCommand.ResetType;


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
	public void run() throws Exception {
		if (!getGit().getRepository().getBranch().toLowerCase().equals(MASTER)) {
			throw buildMojoException("You must be on master branch for execute this goal!");
		}

		setBranchName(PREFIX_HOTFIX + SEPARATOR + getBranchName());

		getLog().info("Creating branch " + getBranchName());
		getGit().checkout().setCreateBranch(true).setName(getBranchName()).call();

		getLog().info("Updating pom version");
		updatePomVersion(getProject().getVersion() + SUFFIX);

		getLog().info("Commiting changed files");
		commit("[GitFlow::start-hotfix] Create release branch " + getBranchName());

        push("Pushing commit");

        getLog().info("DONE");
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rollbacking all changes");
			getGit().reset().setMode(ResetType.HARD).setRef(MASTER).call();
			getGit().checkout().setCreateBranch(false).setForce(true).setName(MASTER).call();
			getGit().branchDelete().setForce(true).setBranchNames(getBranchName()).call();
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
