package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.versions.api.PomHelper;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.mojo.util.BranchUtil;
import com.codegik.gitflow.mojo.util.GitFlow;
import com.codegik.gitflow.mojo.util.MergeGitFlow;


/**
 * Build release
 * Merge develop into release and do not create tag
 * To execute this goal the current branch must be a relase (Ex: release/1.1)
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "build-release", aggregator = true)
public class BuildReleaseMojo extends AbstractGitFlowMojo {

    @Parameter( property = "version", required = true )
	private String version;


	public void run(GitFlow gitFlow) throws Exception {
		String releaseBranch = BranchUtil.buildReleaseBranchName(getVersion());

		if (!gitFlow.getBranch().equals(releaseBranch)) {
			throw new MojoExecutionException("You must be on branch " + releaseBranch + " for execute this goal! ");
		}

		Ref releaseRef 		= gitFlow.validadeReleaseVersion(getVersion());
		String devPomVer	= PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));
		Ref develop 		= gitFlow.findBranch(DEVELOP);
		String branch 		= BranchUtil.getSimpleBranchName(releaseRef);

		gitFlow.checkoutBranch(branch);

		// Realiza o merge do develop para a release
		MergeGitFlow mergeGitFlow = new MergeGitFlow();
		mergeGitFlow.setBranchName(branch);
		mergeGitFlow.setErrorMessage("build-release -Dversion=" + getVersion());
		mergeGitFlow.setTargetRef(develop);
		mergeGitFlow.setIgnoringFilesStage(gitFlow.defineStageForMerge(devPomVer, getVersion()));

		gitFlow.merge(mergeGitFlow);
		compileProject();

		// Commit do merge
		gitFlow.commit("[GitFlow::build-release] Build release branch " + getVersion());
		gitFlow.push();
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		throw new MojoExecutionException("ERROR", e);
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}

}
