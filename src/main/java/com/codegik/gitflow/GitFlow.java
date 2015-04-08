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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.Git;
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

import com.codegik.gitflow.AbstractGitFlowMojo.BranchType;


public class GitFlow {
	private Git git;
	private Log log;
	private String username;
	private String password;


	public GitFlow(Log log, String username, String passwd) {
		this.log 		= log;
		this.username 	= username;
		this.password 	= passwd;
	}


	protected Git getGit() throws Exception {
		if (git == null) {
			git = Git.open(new File("."));
		}

		return git;
	}


	public String getBranch() throws Exception {
		return getGit().getRepository().getBranch();
	}


	public void merge(MergeGitFlow mergeGitFlow) throws Exception {
		merge(mergeGitFlow, null);
	}


	public void merge(MergeGitFlow mergeGitFlow, MergeStrategy mergeStrategy) throws Exception {
		getLog().info("Merging " + mergeGitFlow.getTargetRef().getName() + " into " + mergeGitFlow.getBranchName());

		MergeResult mergeResult = null;

		if (mergeStrategy == null) {
			mergeResult = getGit().merge().include(mergeGitFlow.getTargetRef()).call();
		} else {
			mergeResult = getGit().merge().setStrategy(mergeStrategy).include(mergeGitFlow.getTargetRef()).call();
		}

		if (!mergeResult.getMergeStatus().isSuccessful()) {
			processPreferentialConflicts(mergeGitFlow, mergeResult);
		}
	}


	private void processPreferentialConflicts(MergeGitFlow mergeGitFlow, MergeResult merge) throws Exception {
		List<String> toRemove = new ArrayList<String>();

		for (String key : merge.getConflicts().keySet()) {
			for (String file : mergeGitFlow.getIgnoringFiles()) {
				if (key.contains(file)) {
					checkoutFiles(mergeGitFlow.getBranchName(), key, mergeGitFlow.getIgnoringFilesStage());
					toRemove.add(key);
				}
			}
		}

		for (String key : toRemove) {
			merge.getConflicts().remove(key);
		}

		if (merge.getConflicts().size() > 0) {
			throw buildConflictExeption(mergeGitFlow, merge);
		}

		commit("Commiting resolved conflicts");
	}


	public List<String> deleteBranch(Ref branchRef) throws Exception {
		return getGit().branchDelete().setForce(true).setBranchNames(branchRef.getName()).call();
	}


	public List<String> deleteBranch(String branchName) throws Exception {
		getLog().info("Deleting branch " + branchName);
		Ref branchRef = findBranch(branchName);

		if (branchRef == null) {
			throw buildMojoException("Branch " + branchName + " not found");
		}

		return deleteBranch(branchRef);
	}


	public List<String> deleteBranch(String version, BranchType branchType) throws Exception {
		getLog().info("Deleting " + branchType.toString() + " branch of release " + version);

		List<String> result = new ArrayList<String>();
		String release		= branchType.toString() + AbstractGitFlowMojo.SEPARATOR + version;

		for (Ref b : getGit().branchList().setListMode(ListMode.ALL).call()) {
			if (b.getName().contains(release)) {
				result.addAll(deleteBranch(b));
				getLog().info(" > Deleted " + b.getName());
			}
		}

		return result;
	}


	public List<String> deleteTag(String tagName) throws Exception {
		getLog().info("Deleting tag " + tagName);
		return getGit().tagDelete().setTags(tagName).call();
	}


	public Ref reset(String branchName) throws Exception {
		getLog().info("Reseting into " + branchName);
		return getGit().reset().setMode(ResetType.HARD).setRef(branchName).call();
	}


	public Ref checkoutBranchForced(String branchName) throws Exception {
		getLog().info("Checkout forced into " + branchName);
		return getGit().checkout().setCreateBranch(false).setForce(true).setName(branchName).call();
	}


	public Ref checkoutBranch(String branchName) throws Exception {
		getLog().info("Checkout into " + branchName);
		return getGit().checkout().setCreateBranch(false).setName(branchName).call();
	}


	public Ref checkoutFiles(String branchName, String file, Stage stage) throws Exception {
		getLog().info("Updating file " + file + " from branch " + branchName + " using " + stage.toString());
		Ref ref = getGit().checkout().addPath(file).setName(branchName).setCreateBranch(false).setStage(stage).call();
		getGit().add().addFilepattern(file).call();
		return ref;
	}


	public Ref createBranch(String branchName) throws Exception {
		return createBranch(branchName, null);
	}


	public Ref createBranch(String branchName, String logMessage) throws Exception {
		getLog().info("Creating branch " + branchName + (logMessage == null ? "" : " " + logMessage));
		return getGit().checkout().setCreateBranch(true).setName(branchName).call();
	}


	public RevCommit commit(String message) throws Exception {
		return getGit().commit().setAll(true).setMessage(message).call();
	}


	public Iterable<PushResult> push() throws Exception {
		return push(null);
	}


	public Iterable<PushResult> push(String logMessage) throws Exception {
		getLog().info(logMessage == null ? "Pushing commit" : logMessage);

		if (username != null && password != null) {
	        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
	        return getGit().push().setCredentialsProvider(credentialsProvider).call();
		}

		return getGit().push().call();
	}


	public Ref findBranch(String branch) throws Exception {
		getLog().info("Looking for branch " + branch);

		for (Ref b : getGit().branchList().setListMode(ListMode.ALL).call()) {
			if (branch.equals(b.getName().toLowerCase().replace("refs/remotes/origin/", ""))) {
				return b;
			}
		}

		return null;
	}


	public Ref findLasTag() throws Exception {
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


	public Ref findTag(String tag) throws Exception {
		getLog().info("Looking for tag " + tag);

		Matcher matcher = AbstractGitFlowMojo.TAG_VERSION_PATTERN.matcher(tag);

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


	public String describe() throws Exception {
		return getGit().describe().call();
	}


	public ReleaseEnvironment buildDefaultReleaseEnvironment() throws Exception {
		ReleaseEnvironment environment = new DefaultReleaseEnvironment();

		environment.setLocalRepositoryDirectory(getGit().getRepository().getDirectory());

		return environment;
	}


	public ReleaseDescriptor buildReleaseDescriptor() throws Exception {
		ReleaseDescriptor descriptor = new ReleaseDescriptor();

		descriptor.setAutoVersionSubmodules(true);
		descriptor.setInteractive(false);
		descriptor.setUpdateWorkingCopyVersions(true);
		descriptor.setWorkingDirectory(getGit().getRepository().getDirectory().getParent());
		descriptor.setScmUsername(username);
		descriptor.setScmPassword(password);
		descriptor.setUpdateDependencies(true);
		descriptor.setScmTagNameFormat("@{project.version}");

		return descriptor;
	}


	public MojoExecutionException buildConflictExeption(MergeGitFlow mergeGitFlow, MergeResult merge) {
		getLog().error("There is conflicts in the following files:");

		for (String key : merge.getConflicts().keySet()) {
			getLog().error(key);
		}

		String message = "\nThe merge has conflicts, please try resolve manually! [from " + mergeGitFlow.getTargetRef().getName() + " to " + mergeGitFlow.getBranchName() + "]";
		message += "\nExecute the steps:";
		message += "\ngit reset --hard " + mergeGitFlow.getBranchName();
		message += "\ngit checkout " + mergeGitFlow.getBranchName();
		message += "\ngit merge " + mergeGitFlow.getTargetRef().getName();
		message += "\nmvn gitflow:" + mergeGitFlow.getErrorMessage();

		return buildMojoException(message);
	}



	public MojoExecutionException buildMojoException(String errorMessage) {
		getLog().error(errorMessage);
		return new MojoExecutionException(errorMessage);
	}


	public MojoExecutionException buildMojoException(String errorMessage, Exception e) {
		getLog().error(errorMessage);
		return new MojoExecutionException(errorMessage, e);
	}


	protected Log getLog() {
		return log;
	}
}
