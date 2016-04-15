package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.versions.api.PomHelper;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.core.MergeGitFlow;
import com.codegik.gitflow.core.impl.DefaultGitFlowMojo;


/**
 * Build release
 * Merge develop into release and do not create tag
 * To execute this goal the current branch must be a relase (Ex: release/1.1)
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "build-release", aggregator = true)
public class BuildReleaseMojo extends DefaultGitFlowMojo {
	
	@Parameter( property = "version", required = true )
	private String version;


	public void run() throws Exception {
		String releaseBranch = getGitFlow().buildReleaseBranchName(getVersion());

		if (!getGitFlow().getBranch().equals(releaseBranch)) {
			throw new MojoExecutionException("You must be on branch " + releaseBranch + " for execute this goal! ");
		}

		Ref releaseRef 		= getGitFlow().validadeReleaseVersion(getVersion());
		String devPomVer	= PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));
		Ref develop 		= getGitFlow().findBranch(getGitFlow().getGitFlowPattern().getDevelopName());
		String branch 		= getGitFlow().getSimpleBranchName(releaseRef);

		getGitFlow().checkoutBranch(branch);

		// Realiza o merge do develop para a release
		MergeGitFlow mergeGitFlow = new MergeGitFlow();
		mergeGitFlow.setBranchName(branch);
		mergeGitFlow.setErrorMessage("build-release -Dversion=" + getVersion());
		mergeGitFlow.setTargetRef(develop);
		mergeGitFlow.setIgnoringFilesStage(getGitFlow().defineStageForMerge(devPomVer, getVersion()));
		mergeGitFlow.addIgnoringFiles(getGitFlow().getGitFlowPattern().getPomFileName());

		getGitFlow().merge(mergeGitFlow);
		compileProject();

		// Commit do merge
		getGitFlow().commit("[GitFlow::build-release] Build release branch " + getVersion());
		getGitFlow().push();
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		throw new MojoExecutionException("ERROR", e);
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}

}
