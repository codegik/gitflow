package com.codegik.gitflow;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;

import edu.emory.mathcs.backport.java.util.Arrays;


/**
 * Finish hotfix branch and merge into develop and master
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-hotfix", aggregator = true)
public class FinishHotfixMojo extends AbstractGitFlowMojo {

    @Parameter( property = "branchName", required = true )
    private String branchName;


	@SuppressWarnings("unchecked")
	@Override
	public void run() throws Exception {
		setBranchName(PREFIX_HOTFIX + SEPARATOR + getBranchName());

		getLog().info("Checkout into " + getBranchName());
		getGit().checkout().setCreateBranch(false).setName(getBranchName()).call();

		getLog().info("Updating pom version");
		ReleaseDescriptor descriptor 	= buildReleaseDescriptor();
		ReleaseEnvironment environment 	= buildDefaultReleaseEnvironment();

		getReleaseManager().prepare(descriptor, environment, Arrays.asList(new MavenProject[]{getProject()}));
		getReleaseManager().perform(descriptor, environment, Arrays.asList(new MavenProject[]{getProject()}));

		/**
		 * TODO
		 * Testar com o comando describe
		 */
		List<Ref> tags 	= getGit().tagList().call();
		Ref lastTag 	= tags.get(tags.size() - 1);
		Ref hotfixRef 	= findBranch(getBranchName());

		push("Pushing changes to " + getBranchName());

		getLog().info("Checkout into " + DEVELOP);
		getGit().checkout().setCreateBranch(false).setName(DEVELOP).call();

		getLog().info("Merging " + getBranchName() + " into " + DEVELOP);
		getGit().merge().setStrategy(MergeStrategy.THEIRS).include(hotfixRef).call();

		push("Pushing changes to " + DEVELOP);

		getLog().info("Checkout into " + MASTER);
		getGit().checkout().setCreateBranch(false).setName(MASTER).call();

		getLog().info("Merging tag " + lastTag.getName() + " into " + MASTER);
		getGit().merge().setStrategy(MergeStrategy.THEIRS).include(lastTag).call();

		push("Pushing changes to " + MASTER);

		getLog().info("Deleting hotfix branch " + getBranchName());
		getGit().branchDelete().setForce(true).setBranchNames(getBranchName()).call();

        getLog().info("DONE");
	}


	@SuppressWarnings("unchecked")
	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			ReleaseDescriptor descriptor 	= buildReleaseDescriptor();
			ReleaseEnvironment environment 	= buildDefaultReleaseEnvironment();

			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			getReleaseManager().rollback(descriptor, environment, Arrays.asList(new MavenProject[]{getProject()}));
			getGit().reset().setMode(ResetType.HARD).setRef(DEVELOP).call();
			getGit().checkout().setCreateBranch(false).setForce(true).setName(DEVELOP).call();
		} catch (Exception e1) {;}
		throw buildMojoException("ERROR", e);
	}


	private ReleaseEnvironment buildDefaultReleaseEnvironment() throws Exception {
		ReleaseEnvironment environment = new DefaultReleaseEnvironment();

		environment.setLocalRepositoryDirectory(getGit().getRepository().getDirectory());

		return environment;
	}


	private ReleaseDescriptor buildReleaseDescriptor() throws Exception {
		ReleaseDescriptor descriptor = new ReleaseDescriptor();

		descriptor.mapDevelopmentVersion(getProject().getArtifactId(), getProject().getVersion());
		descriptor.setDefaultDevelopmentVersion(getProject().getVersion());
		descriptor.setAutoVersionSubmodules(true);
		descriptor.setInteractive(false);
		descriptor.setUpdateWorkingCopyVersions(true);
		descriptor.setWorkingDirectory(getGit().getRepository().getDirectory().getParent());
		descriptor.setScmUsername(getUsername());
		descriptor.setScmPassword(getPassword());

		return descriptor;
	}


	public String getBranchName() {
		return branchName;
	}


	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

}
