package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;
import com.codegik.gitflow.MergeGitFlow;


/**
 * Put last tag from release on master
 * Delete all related branches (release|feature|bugfix)
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "publish-release", aggregator = true)
public class PublishReleaseMojo extends AbstractGitFlowMojo {

    @Parameter( property = "version", required = true )
	private String version;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		gitFlow.checkoutBranchForced(DEVELOP);

		// Gettting last tag
		Ref tagRef = gitFlow.findLasTag();

		gitFlow.checkoutBranchForced(MASTER);

		MergeGitFlow mergeGitFlow = new MergeGitFlow();
		mergeGitFlow.setBranchName(MASTER);
		mergeGitFlow.setErrorMessage("publish-release -Dversion=" + getVersion());
		mergeGitFlow.setTargetRef(tagRef);

		gitFlow.merge(mergeGitFlow, MergeStrategy.THEIRS);
		gitFlow.push("Pushing merge");
		gitFlow.deleteBranch(getVersion(), BranchType.feature);
		gitFlow.deleteBranch(getVersion(), BranchType.bugfix);
		gitFlow.deleteBranch(PREFIX_RELEASE + SEPARATOR + getVersion());
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			gitFlow.reset(MASTER);
			gitFlow.checkoutBranchForced(MASTER);
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
