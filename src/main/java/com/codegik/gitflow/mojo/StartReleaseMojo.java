package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.codegik.gitflow.core.impl.DefaultGitFlowMojo;


/**
 * Start new release branch from develop
 * To execute this goal the current branch must be develop
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-release", aggregator = true)
public class StartReleaseMojo extends DefaultGitFlowMojo {
	private String branchName;

    @Parameter( property = "version", required = true )
	private String version;
    

	@Override
	public void run() throws Exception {
		validadeBefore();

		setBranchName(getGitFlow().buildReleaseBranchName(getVersion()));

		getGitFlow().createBranch(getBranchName());

		String newVersion = getVersion();

		updatePomVersion(newVersion + getGitFlow().getGitFlowPattern().getSuffixRelease());
		compileProject();

		getGitFlow().commit("[GitFlow::start-release] Create release branch " + getBranchName() + ": Bumped version number to " + newVersion + getGitFlow().getGitFlowPattern().getSuffixRelease());
		getGitFlow().pushBranch(getBranchName());
	}


	private void validadeBefore() throws Exception {
		getGitFlow().validadePatternReleaseVersion(getVersion());

		if (getGitFlow().findBranch(getVersion()) != null) {
			throw new MojoExecutionException("The release " + getVersion() + " already exists!");
		}

		if (getGitFlow().findLastTag(getVersion()) != null) {
			throw new MojoExecutionException("The release " + getVersion() + " already existed!");
		}

		if (getGitFlow().findBranch(getGitFlow().getGitFlowPattern().getDevelopName()) == null) {
			throw new MojoExecutionException("Please run gitflow:init goal to initialize your repository!");
		}

		// O brach atual deve ser o develop pois o updatePomVersion utiliza o MavenProject que ja possui
		// todos os poms com as versoes atuais, se estiver em outro branch todos os poms deverao ser recarregados
		if (!getGitFlow().getBranch().equals(getGitFlow().getGitFlowPattern().getDevelopName())) {
			throw new MojoExecutionException("You must be on branch develop for execute this goal!");
		}
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			getGitFlow().reset(getGitFlow().getGitFlowPattern().getDevelopName());
			getGitFlow().checkoutBranchForced(getGitFlow().getGitFlowPattern().getDevelopName());
			getGitFlow().deleteLocalBranch(getBranchName());
		} catch (Exception e1) {;}
		throw new MojoExecutionException("ERROR", e);
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}


	public String getBranchName() {
		return branchName;
	}


	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}
}
