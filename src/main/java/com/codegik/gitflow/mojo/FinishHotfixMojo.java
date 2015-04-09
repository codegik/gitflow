package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.versions.api.PomHelper;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;
import com.codegik.gitflow.MergeGitFlow;


/**
 * Finish hotfix branch and merge into develop and master
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-hotfix", aggregator = true)
public class FinishHotfixMojo extends AbstractGitFlowMojo {

	@Parameter( property = "branchName", required = true )
    private String branchName;

	private String mergedPomVersion;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		String simpleName = getBranchName();

		if (!gitFlow.getBranch().equals(MASTER)) {
			throw buildMojoException("You must be on branch master for execute this goal!");
		}

		setBranchName(PREFIX_HOTFIX + SEPARATOR + getBranchName());

		Ref hotfixRef = gitFlow.findBranch(getBranchName());
		MergeGitFlow mergeGitFlow = new MergeGitFlow();

		mergeGitFlow.setBranchName(MASTER);
		mergeGitFlow.setErrorMessage("finish-hotfix -DbranchName=" + simpleName);
		mergeGitFlow.setTargetRef(hotfixRef);
		mergeGitFlow.setIgnoringFilesStage(Stage.THEIRS);

		gitFlow.merge(mergeGitFlow);

		mergedPomVersion = PomHelper.getVersion(PomHelper.getRawModel(getProject().getFile()));

		gitFlow.tag(mergedPomVersion, "[GitFlow::finish-hotfix] Create tag " + mergedPomVersion);
		gitFlow.pushAll();
		gitFlow.checkoutBranchForced(DEVELOP);
		gitFlow.merge(mergeGitFlow);
		gitFlow.deleteBranch(getBranchName());
		gitFlow.deleteLocalBranch(getBranchName());
		gitFlow.pushAll();

		/**
		 * TODO
		 * Describrir como remover o branch remoto
		 */
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			gitFlow.reset(MASTER);
			gitFlow.checkoutBranchForced(MASTER);
			if (mergedPomVersion != null) {
				gitFlow.deleteTag(mergedPomVersion);
			}
		} catch (Exception e1) {;}
		throw buildMojoException("ERROR", e);
	}


	public String getBranchName() {
		return branchName;
	}


	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

}
