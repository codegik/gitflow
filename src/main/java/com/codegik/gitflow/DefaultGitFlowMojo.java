package com.codegik.gitflow;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;


public abstract class DefaultGitFlowMojo extends AbstractGitFlowMojo {
	private Git git;


	protected Git getGit() throws Exception {
		if (git == null) {
			git = Git.open(new File("."));
		}

		return git;
	}


	protected MergeResult merge(Ref ref) throws Exception {
		return merge(ref, null);
	}


	protected MergeResult merge(Ref ref, MergeStrategy mergeStrategy) throws Exception {
		getLog().info("Merging " + ref.getName() + " into " + getGit().getRepository().getBranch());

		if (mergeStrategy == null) {
			return getGit().merge().include(ref).call();
		}

		return getGit().merge().setStrategy(mergeStrategy).include(ref).call();
	}


	protected List<String> deleteBranch(String branchName) throws Exception {
		getLog().info("Deleting branch " + branchName);
		return getGit().branchDelete().setForce(true).setBranchNames(branchName).call();
	}


	protected List<String> deleteTag(String tagName) throws Exception {
		getLog().info("Deleting tag " + tagName);
		return getGit().tagDelete().setTags(tagName).call();
	}


	protected Ref reset(String branchName) throws Exception {
		getLog().info("Reseting into " + branchName);
		return getGit().reset().setMode(ResetType.HARD).setRef(branchName).call();
	}


	protected Ref checkoutBranchForced(String branchName) throws Exception {
		getLog().info("Checkout forced into " + branchName);
		return getGit().checkout().setCreateBranch(false).setForce(true).setName(branchName).call();
	}


	protected Ref checkoutBranch(String branchName) throws Exception {
		getLog().info("Checkout into " + branchName);
		return getGit().checkout().setCreateBranch(false).setName(branchName).call();
	}


	protected Ref createBranch(String branchName) throws Exception {
		return createBranch(branchName, null);
	}


	protected Ref createBranch(String branchName, String logMessage) throws Exception {
		getLog().info("Creating branch " + branchName + (logMessage == null ? "" : " " + logMessage));
		return getGit().checkout().setCreateBranch(true).setName(branchName).call();
	}


	protected RevCommit commit(String message) throws Exception {
		return getGit().commit().setAll(true).setMessage(message).call();
	}


	protected Iterable<PushResult> push() throws Exception {
		return push(null);
	}


	protected Iterable<PushResult> push(String logMessage) throws Exception {
		getLog().info(logMessage == null ? "Pushing commit" : logMessage);

		if (getUsername() != null && getPassword() != null) {
	        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(getUsername(), getPassword());
	        return getGit().push().setCredentialsProvider(credentialsProvider).call();
		}

		return getGit().push().call();
	}


	protected Ref findBranch(String branch) throws Exception {
		getLog().info("Looking for branch " + branch);

		for (Ref b : getGit().branchList().setListMode(ListMode.ALL).call()) {
			if (branch.equals(b.getName().toLowerCase().replace("refs/heads/", "")) ||
				branch.equals(b.getName().toLowerCase().replace("refs/remotes/origin/", ""))) {
				return b;
			}
		}

		return null;
	}


	protected Ref findTag(String tag) throws Exception {
		getLog().info("Looking for tag " + tag);

		Matcher matcher = TAG_VERSION_PATTERN.matcher(tag);

        if (matcher.find()) {
        	String matchTag = matcher.group(0);
        	for (Ref b : getGit().tagList().call()) {
        		if (matchTag.equals(b.getName().toLowerCase().replace("refs/tags/", ""))) {
        			return b;
        		}
        	}
        }


		return null;
	}


	protected ReleaseEnvironment buildDefaultReleaseEnvironment() throws Exception {
		ReleaseEnvironment environment = new DefaultReleaseEnvironment();

		environment.setLocalRepositoryDirectory(getGit().getRepository().getDirectory());

		return environment;
	}


	protected ReleaseDescriptor buildReleaseDescriptor() throws Exception {
		ReleaseDescriptor descriptor = new ReleaseDescriptor();

		descriptor.setAutoVersionSubmodules(true);
		descriptor.setInteractive(false);
		descriptor.setUpdateWorkingCopyVersions(true);
		descriptor.setWorkingDirectory(getGit().getRepository().getDirectory().getParent());
		descriptor.setScmUsername(getUsername());
		descriptor.setScmPassword(getPassword());
		descriptor.setUpdateDependencies(true);
		descriptor.setScmTagNameFormat("@{project.version}");

		return descriptor;
	}


	protected MojoExecutionException buildConflictExeption(MergeResult merge, Ref ref, String destine, String callbackCommand) {
		getLog().error("There is conflicts in the following files:");
		for (String key : merge.getConflicts().keySet()) {
			getLog().error(key);
		}
		String message = "\nThe merge has conflicts, please try resolve manually! [from " + ref.getName() + " to " + destine + "]";
		message += "\nExecute the steps:";
		message += "\ngit reset --hard " + destine;
		message += "\ngit checkout " + destine;
		message += "\ngit merge " + ref.getName();
		message += "\nmvn gitflow:" + callbackCommand;
		return buildMojoException(message);
	}
}
