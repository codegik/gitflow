package com.codegik.gitflow.mojo;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;

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
	private Ref lastTag;
    private String branchName;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		setBranchName(gitFlow.getBranch());

		if (!getBranchName().startsWith(PREFIX_HOTFIX)) {
			throw buildMojoException("You must be on hotfix branch for execute this goal! EX: hotfix/issue3423");
		}

		getLog().info("Updating pom version");
		ReleaseDescriptor descriptor 	= gitFlow.buildReleaseDescriptor();
		ReleaseEnvironment environment 	= gitFlow.buildDefaultReleaseEnvironment();
		List<MavenProject> projects		= buildMavenProjects();
		Ref hotfixRef 					= gitFlow.findBranch(getBranchName());
		lastTag 						= gitFlow.findLasTag();
		String develVersion				= lastTag.getName().split("/")[2] + SUFFIX;

		/**
		 * TODO
		 * Tem problema em gerar a nova tag pois ela j‡ existe devido a ter sido gerada pelo finish-release
		 * Provavelmente o pom dever‡ ser atualizado para ter a vers‹o mais nova (inc lastTag)
		 * Talves seja necess‡rio chamar o updatePomVersion(develVersion) antes de realizar o prepare
		 */

		descriptor.setDefaultDevelopmentVersion(develVersion);
		getReleaseManager().prepare(descriptor, environment, projects);

		gitFlow.push("Pushing changes to " + getBranchName());
		gitFlow.checkoutBranch(DEVELOP);

		MergeGitFlow mergeGitFlow = new MergeGitFlow();
		mergeGitFlow.setBranchName(DEVELOP);
		mergeGitFlow.setErrorMessage("finish-hotfix -DbranchName=" + getBranchName());
		mergeGitFlow.setTargetRef(hotfixRef);

		gitFlow.merge(mergeGitFlow, MergeStrategy.THEIRS);
		gitFlow.push("Pushing changes to " + DEVELOP);

		gitFlow.checkoutBranch(MASTER);

		mergeGitFlow.setBranchName(MASTER);
		mergeGitFlow.setTargetRef(lastTag);

		gitFlow.merge(mergeGitFlow, MergeStrategy.THEIRS);
		gitFlow.push("Pushing changes to " + MASTER);
		gitFlow.deleteBranch(getBranchName());
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			ReleaseDescriptor descriptor 	= gitFlow.buildReleaseDescriptor();
			ReleaseEnvironment environment 	= gitFlow.buildDefaultReleaseEnvironment();

			descriptor.setDefaultDevelopmentVersion(getProject().getVersion());

			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			getReleaseManager().rollback(descriptor, environment, buildMavenProjects());
			gitFlow.reset(MASTER);
			gitFlow.checkoutBranchForced(MASTER);
			if (lastTag != null) {
				gitFlow.deleteTag(lastTag.getName());
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
