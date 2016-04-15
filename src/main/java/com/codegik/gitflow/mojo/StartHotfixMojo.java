package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.codegik.gitflow.core.impl.DefaultGitFlowMojo;


/**
 * Start new hotfix branch from master and increment version
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-hotfix", aggregator = true)
public class StartHotfixMojo extends DefaultGitFlowMojo {

	@Parameter( property = "branchName", required = true )
	private String branchName;

	@Parameter( property = "version" )
	private String version;
    

	@Override
	public void run() throws Exception {
		String newVersion = null;

		if (getVersion() != null) {
			getGitFlow().validadePatternReleaseVersion(getVersion());
			newVersion = getVersion() + getGitFlow().getGitFlowPattern().getSuffixRelease();
		}

		setBranchName(getGitFlow().buildHotfixBranchName(getBranchName()));
		validadeBefore();
		getGitFlow().createBranch(getBranchName());

		if (newVersion == null) {
			newVersion = getGitFlow().increaseVersionBasedOnTag(getProject().getVersion());
		}

		updatePomVersion(newVersion);
		compileProject();

		getGitFlow().commit("[GitFlow::start-hotfix] Create hotfix branch " + getBranchName() + ": Bumped version number to " + newVersion);
		getGitFlow().pushBranch(getBranchName());
	}


	private void validadeBefore() throws Exception {
		if (!getGitFlow().getBranch().toLowerCase().equals(getGitFlow().getGitFlowPattern().getMasterName())) {
			throw new MojoExecutionException("You must be on branch master for execute this goal!");
		}

		if (getGitFlow().findBranch(getBranchName()) != null) {
			throw new MojoExecutionException("The branch " + getBranchName() + " already exists!");
		}
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rollbacking all changes");
			getGitFlow().reset(getGitFlow().getGitFlowPattern().getMasterName());
			getGitFlow().checkoutBranchForced(getGitFlow().getGitFlowPattern().getMasterName());
			getGitFlow().deleteRemoteBranch(getBranchName());
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
