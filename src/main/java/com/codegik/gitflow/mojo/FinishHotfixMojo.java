package com.codegik.gitflow.mojo;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;

import com.codegik.gitflow.DefaultGitFlowMojo;


/**
 * Finish hotfix branch and merge into develop and master
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-hotfix", aggregator = true)
public class FinishHotfixMojo extends DefaultGitFlowMojo {
	private Ref lastTag;

    @Parameter( property = "branchName", required = true )
    private String branchName;


	@Override
	public void run() throws Exception {
		setBranchName(PREFIX_HOTFIX + SEPARATOR + getBranchName());

		checkoutBranch(getBranchName());

		getLog().info("Updating pom version");
		ReleaseDescriptor descriptor 	= buildReleaseDescriptor();
		ReleaseEnvironment environment 	= buildDefaultReleaseEnvironment();
		List<MavenProject> projects		= buildMavenProjects();

		descriptor.setDefaultDevelopmentVersion(getProject().getVersion());
		getReleaseManager().prepare(descriptor, environment, projects);
		getReleaseManager().perform(descriptor, environment, projects);

		lastTag 		= findTag(getGit().describe().call());
		Ref hotfixRef 	= findBranch(getBranchName());

		push("Pushing changes to " + getBranchName());

		checkoutBranch(DEVELOP);

		merge(hotfixRef, MergeStrategy.THEIRS);

		push("Pushing changes to " + DEVELOP);

		checkoutBranch(MASTER);

		merge(lastTag, MergeStrategy.THEIRS);

		push("Pushing changes to " + MASTER);

		deleteBranch(getBranchName());
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			ReleaseDescriptor descriptor 	= buildReleaseDescriptor();
			ReleaseEnvironment environment 	= buildDefaultReleaseEnvironment();

			descriptor.setDefaultDevelopmentVersion(getProject().getVersion());

			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			getReleaseManager().rollback(descriptor, environment, buildMavenProjects());
			reset(MASTER);
			checkoutBranchForced(MASTER);
			deleteTag(lastTag.getName());
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
