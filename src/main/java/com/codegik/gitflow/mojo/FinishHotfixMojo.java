package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.versions.api.PomHelper;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

import com.codegik.gitflow.core.MergeGitFlow;
import com.codegik.gitflow.core.impl.DefaultGitFlowMojo;


/**
 * Finish hotfix branch and merge into develop and master
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-hotfix", aggregator = true)
public class FinishHotfixMojo extends DefaultGitFlowMojo {
	private String pomVersion;
	private RevCommit revertCommit;

	@Parameter( property = "branchName", required = true )
    private String branchName;

	@Parameter( property = "version" )
	private String version;

	
	@Override
	public void run() throws Exception {
		String simpleName = getBranchName();
		setBranchName(getGitFlow().buildHotfixBranchName(getBranchName()));

		if (getVersion() != null) {
			getGitFlow().validadePatternReleaseVersion(getVersion());
		}

		if (getGitFlow().findBranch(getBranchName()) == null) {
			throw new MojoExecutionException("The branch " + getBranchName() + " dosen't exists!");
		}

		Ref hotfixRef = getGitFlow().checkoutBranch(getBranchName());
		pomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));

		// Buscar a ultima tag do master e incrementa a versao pois pode existir uma release entregue anteriormente
		Ref lastTag = getGitFlow().findLastTag();
		if (lastTag != null) {
			getLog().info("Finding the newest tag");
			String lastTagVer = getGitFlow().getVersionFromTag(lastTag);

			if (getGitFlow().whatIsTheBigger(pomVersion, lastTagVer) <= 0) {
				getLog().info("Found newer " + lastTagVer);

				String newVersion = getGitFlow().increaseVersionBasedOnTag(lastTag);
				updatePomVersion(newVersion);
				compileProject();

				revertCommit = getGitFlow().commit("[GitFlow::finish-hotfix] Bumped version number to " + newVersion);
				getGitFlow().push();
			}
		}

		// Checkout para o branch master para fazer o merge com o branch do hotfix
		getGitFlow().checkoutBranch(getGitFlow().getGitFlowPattern().getMasterName());
		getGitFlow().reset(getGitFlow().getGitFlowPattern().getOriginName() + getGitFlow().getGitFlowPattern().getGitSeparator() + getGitFlow().getGitFlowPattern().getMasterName());
		getGitFlow().deleteLocalBranch(getBranchName());

		hotfixRef = getGitFlow().findBranch(getBranchName());
		MergeGitFlow mergeGitFlow = new MergeGitFlow();

		mergeGitFlow.setBranchName(getGitFlow().getGitFlowPattern().getMasterName());
		mergeGitFlow.setErrorMessage("finish-hotfix -DbranchName=" + simpleName);
		mergeGitFlow.setTargetRef(hotfixRef);
		mergeGitFlow.setIgnoringFilesStage(Stage.THEIRS);
		mergeGitFlow.addIgnoringFiles(getGitFlow().getGitFlowPattern().getPomFileName());

		getGitFlow().merge(mergeGitFlow);
		compileProject();
		getGitFlow().push();

		pomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));

		Ref tag = getGitFlow().tag(pomVersion, "[GitFlow::finish-hotfix] Create tag " + pomVersion);
		getGitFlow().pushTag(tag);
		getGitFlow().checkoutBranch(getGitFlow().getGitFlowPattern().getDevelopName());
		getGitFlow().merge(mergeGitFlow);
		getGitFlow().deleteRemoteBranch(getBranchName());
		getGitFlow().push();
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			getGitFlow().reset(getGitFlow().getGitFlowPattern().getMasterName());
			getGitFlow().checkoutBranchForced(getGitFlow().getGitFlowPattern().getMasterName());

			if (revertCommit != null) {
				getGitFlow().revertCommit(revertCommit);
				getGitFlow().push();
			}

			if (pomVersion != null) {
				getGitFlow().deleteTag(pomVersion);
			}
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
