package com.codegik.gitflow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
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


	protected List<String> deleteBranch(Ref branchRef) throws Exception {
		return getGit().branchDelete().setForce(true).setBranchNames(branchRef.getName()).call();
	}


	protected List<String> deleteBranch(String branchName) throws Exception {
		getLog().info("Deleting branch " + branchName);
		Ref branchRef = findBranch(branchName);

		if (branchRef == null) {
			throw buildMojoException("Branch " + branchName + " not found");
		}

		return deleteBranch(branchRef);
	}


	protected List<String> deleteBranch(String version, BranchType branchType) throws Exception {
		getLog().info("Deleting " + branchType.toString() + " branch of release " + version);

		List<String> result = new ArrayList<String>();
		String release		= branchType.toString() + SEPARATOR + version;

		for (Ref b : getGit().branchList().setListMode(ListMode.ALL).call()) {
			if (b.getName().contains(release)) {
				result.addAll(deleteBranch(b));
				getLog().info(" > Deleted " + b.getName());
			}
		}

		return result;
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


	protected Ref checkoutFiles(String branchName, String file, Stage stage) throws Exception {
		getLog().info("Updating file " + file + " from branch " + branchName + " using " + stage.toString());
		getGit().add().addFilepattern(file).call();
		return getGit().checkout().addPath(file).setName(branchName).setCreateBranch(false).setStage(stage).call();
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
			if (branch.equals(b.getName().toLowerCase().replace("refs/remotes/origin/", ""))) {
				return b;
			}
		}

		return null;
	}


	protected Ref findLasTag() throws Exception {
		final RevWalk walk = new RevWalk(getGit().getRepository());
		List<Ref> tags = getGit().tagList().call();

		Collections.sort(tags, new Comparator<Ref>() {
			public int compare(Ref o1, Ref o2) {
				Date d1 = null;
				Date d2 = null;
				try {
					d1 = walk.parseTag(o1.getObjectId()).getTaggerIdent().getWhen();
					d2 = walk.parseTag(o2.getObjectId()).getTaggerIdent().getWhen();

				} catch (IOException e) {
					e.printStackTrace();
				}
				return d1.compareTo(d2);
			}
		});

		return tags.get(tags.size()-1);
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
