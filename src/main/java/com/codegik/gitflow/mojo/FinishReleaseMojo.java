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
import com.codegik.gitflow.mojo.util.BranchUtil;


/**
 * Finish release
 * Increment version, merge release into develop and tag develop
 * To execute this goal the current branch must be develop
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-release", aggregator = true)
public class FinishReleaseMojo extends AbstractGitFlowMojo {
	private String pomVersion;
	private RevCommit revertCommit;

    @Parameter( property = "version", required = true )
	private String version;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		Ref releaseRef = gitFlow.validadeReleaseVersion(getVersion());

		if (!gitFlow.getBranch().equals(DEVELOP)) {
			throw buildMojoException("You must be on branch develop for execute this goal! ");
		}

		pomVersion = getProject().getVersion();

		// Buscar a ultima tag da release e incrementa a versao pois pode existir uma tag nova de hotfix
		Ref lastTag = gitFlow.findLastTag(getVersion());
		if (lastTag != null) {
			getLog().info("Checking for most current tag");
			String lastTagVer = BranchUtil.getVersionFromTag(lastTag);

			if (gitFlow.whatIsTheBigger(pomVersion, lastTagVer) <= 0) {
				getLog().info("Found newer " + lastTagVer);

				String newVersion = gitFlow.incrementVersion(lastTag);
				updatePomVersion(newVersion);

				getLog().info("Commiting changed files");
				revertCommit = gitFlow.commit("[GitFlow::finish-release] Bumped version number to " + newVersion);
				gitFlow.push("Pushing commit");
				pomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));
			}
		}

		// Realiza o merge da release para o develop
		MergeGitFlow mergeGitFlow = new MergeGitFlow();
		mergeGitFlow.setBranchName(DEVELOP);
		mergeGitFlow.setErrorMessage("finish-release -Dversion=" + getVersion());
		mergeGitFlow.setTargetRef(releaseRef);
		mergeGitFlow.setIgnoringFilesStage(gitFlow.defineStageForMerge(pomVersion, getVersion()));

		gitFlow.merge(mergeGitFlow);

		// Recarrega a versao do pom pois provavelmente deve ter alterada depois do merge
		pomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));

		// Cria a tag da release com base no develop
		Ref newTag = gitFlow.tag(pomVersion, "[GitFlow::finish-release] Create tag " + pomVersion);
		getLog().info("Commiting changed files");
		gitFlow.commit("[GitFlow::finish-release] Finish release branch " + getVersion());
		gitFlow.pushAll();

		// Incrementa a versao baseado na tag
		String newVersion = gitFlow.incrementVersion(newTag);
		getLog().info("Bumping version of files to " + newVersion);

		// Volta para o branch da release
		gitFlow.checkoutBranch(BranchUtil.getSimpleBranchName(releaseRef));

		// Atualiza a versao incrementada dos poms
		updatePomVersion(newVersion);
		getLog().info("Commiting changed files");
		gitFlow.commit("[GitFlow::finish-release] Bumped version number to " + newVersion);
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

			if (pomVersion != null) {
				gitFlow.deleteTag(pomVersion);
				gitFlow.pushAll();
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
