package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.versions.api.PomHelper;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;
import com.codegik.gitflow.MergeGitFlow;


/**
 * Finish release
 * Increment version, merge release into develop and tag develop
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-release", aggregator = true)
public class FinishReleaseMojo extends AbstractGitFlowMojo {
	private String mergedPomVersion;
	private RevCommit revertCommit;

    @Parameter( property = "version", required = true )
	private String version;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		Ref releaseRef = gitFlow.validadeReleaseVersion(getVersion());

		if (!gitFlow.getBranch().equals(DEVELOP)) {
			throw buildMojoException("You must be on branch develop for execute this goal!");
		}

		mergedPomVersion = getProject().getVersion();

		/**
		 * Buscar a ultima tag da release e incrementa a versao pois pode existir uma tag nova de hotfix
		 */
		Ref lastTag = gitFlow.findLasTag(getVersion());
		if (lastTag != null) {
			String newVersion = gitFlow.incrementVersion(lastTag);

			updatePomVersion(newVersion);

			getLog().info("Commiting changed files");
			revertCommit = gitFlow.commit("[GitFlow::finish-release] Bumped version number to " + newVersion);
			gitFlow.push("Pushing commit");
			mergedPomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));
		}

		MergeGitFlow mergeGitFlow = new MergeGitFlow();
		mergeGitFlow.setBranchName(DEVELOP);
		mergeGitFlow.setErrorMessage("finish-release -Dversion=" + getVersion());
		mergeGitFlow.setTargetRef(releaseRef);
		mergeGitFlow.setIgnoringFilesStage(gitFlow.defineStageForMerge(mergedPomVersion, getVersion()));

		gitFlow.merge(mergeGitFlow);

		mergedPomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));

		gitFlow.tag(mergedPomVersion, "[GitFlow::finish-release] Create tag " + mergedPomVersion);
		gitFlow.pushAll();

		getLog().info("Commiting changed files");
		gitFlow.commit("[GitFlow::finishi-release] Finish release branch " + getVersion());
		gitFlow.pushAll();
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			gitFlow.reset(DEVELOP);
			gitFlow.checkoutBranchForced(DEVELOP);

			if (revertCommit != null) {
				 gitFlow.revertCommit(revertCommit);
				 gitFlow.push("Pushing revert commit");
			}

			if (mergedPomVersion != null) {
				gitFlow.deleteTag(mergedPomVersion);
			}
		} catch (Exception e1) {;}
		throw buildMojoException("ERROR", e);
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}

}
