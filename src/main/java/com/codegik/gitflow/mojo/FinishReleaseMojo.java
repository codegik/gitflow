package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.versions.api.PomHelper;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.mojo.util.BranchUtil;
import com.codegik.gitflow.mojo.util.GitFlow;
import com.codegik.gitflow.mojo.util.MergeGitFlow;


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
			throw new MojoExecutionException("You must be on branch develop for execute this goal! ");
		}

		// Verifica se a release esta ultrapassada
		pomVersion = getProject().getVersion();
		Ref lastTag = gitFlow.findLastTag();
		String lastTagVer = BranchUtil.getVersionFromTag(lastTag);

		/**
		 * TODO
		 * Arrumar aqui, a comparacao de versoes nao pode ser esta, deve ser a release (1.2) com a tag (1.5.4)
		 * Continuar a configuracao no jenkins...
		 */
		if (gitFlow.whatIsTheBigger(pomVersion, lastTagVer, Boolean.FALSE) < 0) {
			throw new MojoExecutionException("The release " + getVersion() + " is older than " + lastTagVer + ", please start new release!");
		}

		// Buscar a ultima tag da release e incrementa a versao pois pode existir uma tag nova de hotfix
		lastTag = gitFlow.findLastTag(getVersion());
		if (lastTag != null) {
			getLog().info("Finding the newest tag");
			lastTagVer = BranchUtil.getVersionFromTag(lastTag);

			if (gitFlow.whatIsTheBigger(pomVersion, lastTagVer) <= 0) {
				getLog().info("Found newer " + lastTagVer);

				String newVersion = gitFlow.increaseVersionBasedOnTag(lastTag);
				updatePomVersion(newVersion);
				compileProject();

				revertCommit = gitFlow.commit("[GitFlow::finish-release] Bumped version number to " + newVersion);
				gitFlow.push();
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
		compileProject();

		// Recarrega a versao do pom pois provavelmente deve ter alterada depois do merge
		pomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));

		// Cria a tag da release com base no develop
		Ref tag = gitFlow.tag(pomVersion, "[GitFlow::finish-release] Create tag " + pomVersion);
		gitFlow.commit("[GitFlow::finish-release] Finish release branch " + getVersion());
		gitFlow.push();
		gitFlow.pushTag(tag);

		// Volta para o branch da release
		gitFlow.checkoutBranch(BranchUtil.getSimpleBranchName(releaseRef));

		// Incrementa a versao baseado na tag
		String newVersion = gitFlow.increaseVersionBasedOnTag(tag);
		updatePomVersion(newVersion);

		gitFlow.commit("[GitFlow::finish-release] Bumped version number to " + newVersion);
		gitFlow.push();
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");

			if (revertCommit != null) {
				gitFlow.revertCommit(revertCommit);
				gitFlow.push();
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
