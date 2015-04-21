package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.versions.api.PomHelper;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.mojo.util.BranchUtil;
import com.codegik.gitflow.mojo.util.GitFlow;
import com.codegik.gitflow.mojo.util.MergeGitFlow;


/**
 * Finish hotfix branch and merge into develop and master
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-hotfix", aggregator = true)
public class FinishHotfixMojo extends AbstractGitFlowMojo {
	private String pomVersion;
	private RevCommit revertCommit;

	@Parameter( property = "branchName", required = true )
    private String branchName;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		validadeBefore(gitFlow);

		String simpleName = getBranchName();

		Ref hotfixRef = gitFlow.checkoutBranch(BranchUtil.buildHotfixBranchName(simpleName));

		setBranchName(BranchUtil.buildHotfixBranchName(simpleName));

		pomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));

		// Buscar a ultima tag do master e incrementa a versao pois pode existir uma release entregue anteriormente
		Ref lastTag = gitFlow.findLastTag();
		if (lastTag != null) {
			getLog().info("Checking for most current tag");
			String lastTagVer = BranchUtil.getVersionFromTag(lastTag);

			if (gitFlow.whatIsTheBigger(pomVersion, lastTagVer) <= 0) {
				getLog().info("Found newer " + lastTagVer);

				String newVersion = gitFlow.increaseVersionBasedOnTag(lastTag);
				updatePomVersion(newVersion);
				compileProject();

				getLog().info("Commiting changed files");
				revertCommit = gitFlow.commit("[GitFlow::finish-hotfix] Bumped version number to " + newVersion);
				gitFlow.push("Pushing commit");
			}
		}

		// Checkout para o branch master para fazer o merge com o branch do hotfix
		gitFlow.checkoutBranchForced(MASTER);
		gitFlow.reset(ORIGIN + SEPARATOR + MASTER);
		gitFlow.deleteLocalBranch(getBranchName());

		hotfixRef = gitFlow.findBranch(BranchUtil.buildHotfixBranchName(simpleName));

		MergeGitFlow mergeGitFlow = new MergeGitFlow();

		mergeGitFlow.setBranchName(MASTER);
		mergeGitFlow.setErrorMessage("finish-hotfix -DbranchName=" + simpleName);
		mergeGitFlow.setTargetRef(hotfixRef);
		mergeGitFlow.setIgnoringFilesStage(Stage.THEIRS);

		gitFlow.merge(mergeGitFlow);
		compileProject();

		pomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));

		gitFlow.tag(pomVersion, "[GitFlow::finish-hotfix] Create tag " + pomVersion);
		gitFlow.pushAll();
		gitFlow.checkoutBranchForced(DEVELOP);
		gitFlow.merge(mergeGitFlow);
		gitFlow.deleteRemoteBranch(getBranchName());
		gitFlow.pushAll();
	}


	private void validadeBefore(GitFlow gitFlow) throws Exception {
		if (gitFlow.findBranch(getBranchName()) != null) {
			throw new MojoExecutionException("The branch " + getBranchName() + " dosen't exists!");
		}
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			gitFlow.reset(MASTER);
			gitFlow.checkoutBranchForced(MASTER);

			if (revertCommit != null) {
				 gitFlow.revertCommit(revertCommit);
				 gitFlow.push("Pushing revert commit");
			}

			if (pomVersion != null) {
				gitFlow.deleteTag(pomVersion);
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

}
