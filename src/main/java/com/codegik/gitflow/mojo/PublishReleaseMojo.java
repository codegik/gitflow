package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;

import com.codegik.gitflow.core.MergeGitFlow;
import com.codegik.gitflow.core.impl.DefaultBranchType;
import com.codegik.gitflow.core.impl.DefaultGitFlowMojo;


/**
 * Put last tag from release on master
 * Delete all related branches (release|feature|bugfix)
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "publish-release", aggregator = true)
public class PublishReleaseMojo extends DefaultGitFlowMojo {

    @Parameter( property = "version", required = true )
	private String version;
    

	@Override
	public void run() throws Exception {
		validadeBefore();

		// Busca ultima tag da release
		Ref tagRef = getGitFlow().findLastTag(getVersion());
		if (tagRef == null) {
			throw new MojoExecutionException("The release " + getVersion() + " was never finished, please execute finish-release goal before!");
		}

		// Realiza o merge da tag para o master (using theirs)
		getGitFlow().checkoutBranch(getGitFlow().getGitFlowPattern().getMasterName());

		MergeGitFlow mergeGitFlow = new MergeGitFlow();
		mergeGitFlow.setBranchName(getGitFlow().getGitFlowPattern().getMasterName());
		mergeGitFlow.setErrorMessage("publish-release -Dversion=" + getVersion());
		mergeGitFlow.setTargetRef(tagRef);
		mergeGitFlow.addIgnoringFiles(getGitFlow().getGitFlowPattern().getPomFileName());

		/**
		 * TODO
		 * Como resolver a situacao que ocorre quando um hotfix eh aberto e entregue para producao
		 * durante o periodo de homologacao de uma release?
		 * Solucao: replicar as correcoes de hotfix para a versao que esta em homologacao
		 */
		getGitFlow().merge(mergeGitFlow, MergeStrategy.THEIRS);
		compileProject();
		getGitFlow().push();

		// Remove os branches de feature, bugfix e o branch da release
		getGitFlow().deleteRemoteBranch(getVersion(), DefaultBranchType.feature);
		getGitFlow().deleteRemoteBranch(getVersion(), DefaultBranchType.bugfix);
		getGitFlow().deleteRemoteBranch(getGitFlow().buildReleaseBranchName(getVersion()));
	}


	private void validadeBefore() throws Exception {
		getGitFlow().validadePatternReleaseVersion(getVersion());
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			getGitFlow().reset(getGitFlow().getGitFlowPattern().getMasterName());
			getGitFlow().checkoutBranchForced(getGitFlow().getGitFlowPattern().getMasterName());
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
