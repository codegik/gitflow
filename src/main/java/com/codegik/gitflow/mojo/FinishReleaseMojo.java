package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.versions.api.PomHelper;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

import com.codegik.gitflow.core.MergeGitFlow;
import com.codegik.gitflow.core.impl.DefaultGitFlowMojo;


/**
 * Finish release
 * Increment version, merge release into develop and tag develop
 * To execute this goal the current branch must be develop
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-release", aggregator = true)
public class FinishReleaseMojo extends DefaultGitFlowMojo {
	private String pomVersion;
	private RevCommit revertCommit;

    @Parameter( property = "version", required = true )
	private String version;


	@Override
	public void run() throws Exception {
		Ref releaseRef = getGitFlow().validadeReleaseVersion(getVersion());

		if (!getGitFlow().getBranch().equals(getGitFlow().getGitFlowPattern().getDevelopName())) {
			throw new MojoExecutionException("You must be on branch develop for execute this goal! ");
		}

		// Verifica se a release esta ultrapassada
		pomVersion 			= getProject().getVersion();
		Ref lastTag 		= getGitFlow().findLastTag();
		String lastTagVer 	= getGitFlow().getVersionFromTag(lastTag);

		if (getGitFlow().isReleaseSmallerThanCurrentVersion(getVersion(), lastTagVer)) {
			throw new MojoExecutionException("The release " + getVersion() + " is older than " + lastTagVer + ", please start new release!");
		}

		// Buscar a ultima tag da release e incrementa a versao pois pode existir uma tag nova de hotfix
		lastTag = getGitFlow().findLastTag(getVersion());
		if (lastTag != null) {
			getLog().info("Finding the newest tag");
			lastTagVer = getGitFlow().getVersionFromTag(lastTag);

			if (getGitFlow().whatIsTheBigger(pomVersion, lastTagVer) <= 0) {
				getLog().info("Found newer " + lastTagVer);

				String newVersion = getGitFlow().increaseVersionBasedOnTag(lastTag);
				updatePomVersion(newVersion);
				compileProject();

				revertCommit = getGitFlow().commit("[GitFlow::finish-release] Bumped version number to " + newVersion);
				getGitFlow().push();
				pomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));
			}
		}

		// Realiza o merge da release para o develop
		MergeGitFlow mergeGitFlow = new MergeGitFlow();
		mergeGitFlow.setBranchName(getGitFlow().getGitFlowPattern().getDevelopName());
		mergeGitFlow.setErrorMessage("finish-release -Dversion=" + getVersion());
		mergeGitFlow.setTargetRef(releaseRef);
		mergeGitFlow.setIgnoringFilesStage(getGitFlow().defineStageForMerge(pomVersion, getVersion()));
		mergeGitFlow.addIgnoringFiles(getGitFlow().getGitFlowPattern().getPomFileName());

		getGitFlow().merge(mergeGitFlow);
		compileProject();

		// Recarrega a versao do pom pois provavelmente deve ter alterada depois do merge
		pomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));

		// Cria a tag da release com base no develop
		Ref tag = getGitFlow().tag(pomVersion, "[GitFlow::finish-release] Create tag " + pomVersion);
		getGitFlow().commit("[GitFlow::finish-release] Finish release branch " + getVersion());
		getGitFlow().push();
		getGitFlow().pushTag(tag);

		// Volta para o branch da release
		getGitFlow().checkoutBranch(getGitFlow().getSimpleBranchName(releaseRef));

		// Incrementa a versao baseado na tag
		String newVersion = getGitFlow().increaseVersionBasedOnTag(tag);
		updatePomVersion(newVersion);

		getGitFlow().commit("[GitFlow::finish-release] Bumped version number to " + newVersion);
		getGitFlow().push();
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");

			if (revertCommit != null) {
				getGitFlow().revertCommit(revertCommit);
				getGitFlow().push();
			}

		} catch (Exception e1) {;}
		throw new MojoExecutionException("ERROR", e);
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}

}
