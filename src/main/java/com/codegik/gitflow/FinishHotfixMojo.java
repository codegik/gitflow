package com.codegik.gitflow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
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
	private static final Pattern TAG_VERSION_PATTERN = Pattern.compile("([0-9]{1,})\\.([0-9]{1,})\\.([0-9]{1,})");
	private String newTagName;

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

		Ref lastTag 	= findTag(getGit().describe().call());
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
			getGit().reset().setMode(ResetType.HARD).setRef(MASTER).call();
			getGit().checkout().setCreateBranch(false).setForce(true).setName(MASTER).call();
			getGit().tagDelete().setTags(newTagName).call();
		} catch (Exception e1) {;}
		throw buildMojoException("ERROR", e);
	}


	public String getBranchName() {
		return branchName;
	}


	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}


	private String extractVersionFromString(String tag) {
		Matcher matcher = TAG_VERSION_PATTERN.matcher(tag);

        if (matcher.find()) {
        	return matcher.group(0);
        }

        return null;
	}

}
