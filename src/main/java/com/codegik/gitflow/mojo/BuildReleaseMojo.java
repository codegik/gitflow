package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.versions.api.PomHelper;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;
import com.codegik.gitflow.MergeGitFlow;
import com.codegik.gitflow.mojo.util.BranchUtil;


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
			throw buildMojoException("You must be on branch " + releaseBranch + " for execute this goal! ");
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
		compileProject("clean install", getSkipTests());

		// Recarrega a versao do pom
		String pomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));

		// Cria a tag da release com base na release
		getLog().info("Commiting changed files");
		gitFlow.commit("[GitFlow::build-release] Build release branch " + getVersion());
		gitFlow.pushAll();

		// Incrementa a versao baseado no pom
		String newVersion = gitFlow.incrementVersion(pomVersion);
		updatePomVersion(newVersion);

		getLog().info("Commiting changed files");
		gitFlow.commit("[GitFlow::build-release] Bumped version number to " + newVersion);
		gitFlow.pushAll();
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		throw buildMojoException("ERROR", e);
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}

}
