package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.mojo.util.BranchUtil;
import com.codegik.gitflow.mojo.util.GitFlow;


/**
 * Start new hotfix branch from master and increment version
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-hotfix", aggregator = true)
public class StartHotfixMojo extends AbstractGitFlowMojo {

	@Parameter( property = "branchName", required = true )
	private String branchName;

	@Parameter( property = "version" )
	private String version;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		String newVersion = null;

		if (getVersion() != null) {
			gitFlow.validadePatternReleaseVersion(getVersion());
			newVersion = getVersion() + SUFFIX_RELEASE;
		}

		setBranchName(BranchUtil.buildHotfixBranchName(getBranchName()));
		validadeBefore(gitFlow);
		gitFlow.createBranch(getBranchName());

		if (newVersion == null) {
			newVersion = gitFlow.increaseVersionBasedOnTag(getProject().getVersion());
		}

		updatePomVersion(newVersion);
		compileProject();

		getLog().info("Commiting changed files");
		gitFlow.commit("[GitFlow::start-hotfix] Create hotfix branch " + getBranchName() + ": Bumped version number to " + newVersion);
		gitFlow.pushBranch(getBranchName());
	}


	private void validadeBefore(GitFlow gitFlow) throws Exception {
		if (!gitFlow.getBranch().toLowerCase().equals(MASTER)) {
			throw new MojoExecutionException("You must be on branch master for execute this goal!");
		}

		if (gitFlow.findBranch(getBranchName()) != null) {
			throw new MojoExecutionException("The branch " + getBranchName() + " already exists!");
		}
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rollbacking all changes");
			gitFlow.reset(MASTER);
			gitFlow.checkoutBranchForced(MASTER);
			gitFlow.deleteRemoteBranch(getBranchName());
		} catch (Exception e1) {;}
		throw new MojoExecutionException("ERROR", e);
	}


	public String getBranchName() {
		return branchName;
	}


	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}

}
