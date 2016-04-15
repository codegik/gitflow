package com.codegik.gitflow.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;

import com.codegik.gitflow.command.CommandExecutor;
import com.codegik.gitflow.command.GitCommandExecutor;
import com.codegik.gitflow.core.impl.DefaultBranchType;


public abstract class GitFlow {
	private Git git;
	private Log log;
	private File repository;
	private CommandExecutor gitExecutor;
	private GitFlowPattern gitFlowPattern;
	

	public GitFlow(GitFlowPattern gitFlowPattern, Log log, File repository) {
		this.log 			= log;
		this.repository 	= repository;
		this.gitFlowPattern	= gitFlowPattern;
		this.gitExecutor	= new GitCommandExecutor(log);
	}


	public void setGit(Git git) {
		this.git = git;
	}

	public Git getGit() throws Exception {
		if (git == null) {
			git = Git.open(repository);
		}

		return git;
	}


	public String getBranch() throws Exception {
		return getGit().getRepository().getBranch();
	}


	public Ref tag(String tagName, String message) throws Exception {
		getLog().info("Tagging " + tagName);
		return getGit().tag().setName(tagName).setMessage(message).call();
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


	public String deleteRemoteBranch(Ref branchRef) throws Exception {
		String simpleName = getSimpleBranchName(branchRef);

		deleteLocalBranch(simpleName);

		return gitExecutor.execute("push", "origin", ":" + simpleName);
	}


	public String deleteRemoteBranch(String branchName) throws Exception {
		getLog().info("Deleting branch " + branchName);
		Ref branchRef = findBranch(branchName);

		if (branchRef == null) {
			throw new MojoExecutionException("Branch " + branchName + " not found");
		}

		return deleteRemoteBranch(branchRef);
	}


	public void deleteRemoteBranch(String version, BranchType branchType) throws Exception {
		getLog().info("Deleting " + branchType.toString() + " branch of release " + version);

		List<String> deleted = new ArrayList<String>();
		String release = branchType.toString() + gitFlowPattern.getGitSeparator() + version;

		for (Ref b : getGit().branchList().setListMode(ListMode.ALL).call()) {
			if (b.getName().contains(release) && !deleted.contains(getSimpleBranchName(b))) {
				deleteRemoteBranch(b);
				deleted.add(getSimpleBranchName(b));
			}
		}
	}


	public String deleteLocalBranch(String branchName) throws Exception {
		getLog().info("Deleting local branch " + branchName);

		if (getBranch().equals(branchName)) {
			throw new MojoExecutionException("Please change to another branch before delete");
		}

		if (findLocalBranch(branchName) != null) {
			return gitExecutor.execute("branch", "-D", branchName);
		}

		return null;
	}


	public String deleteTag(String tagName) throws Exception {
		return gitExecutor.execute("push", "origin", ":" + tagName);
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
		Boolean branchExists = getGit().getRepository().getRef(branchName) != null;

		if (!branchExists) {
			getGit().branchCreate().setName(branchName).setUpstreamMode(SetupUpstreamMode.TRACK).setStartPoint("origin/" + branchName).call();
		}

		return getGit().checkout().setName(branchName).call();
	}


	public Ref checkoutFiles(String branchName, String file, Stage stage) throws Exception {
		getLog().info("Updating file " + file + " from branch " + branchName + " using " + stage.toString());
		Ref ref = getGit().checkout().addPath(file).setName(branchName).setCreateBranch(false).setStage(stage).call();
		getGit().add().addFilepattern(file).call();
		return ref;
	}


	public Ref createBranch(String branchName) throws Exception {
		getLog().info("Creating branch " + branchName);
		return getGit().checkout().setCreateBranch(true).setName(branchName).call();
	}


	public RevCommit commit(String message) throws Exception {
		getLog().info("Commiting... " + message);
		getGit().add().addFilepattern(".").call();
		return getGit().commit().setAll(true).setMessage(message).call();
	}


	public String pull() throws Exception {
		return gitExecutor.execute("pull");
	}


	public String pushBranch(String branchName) throws Exception {
		return gitExecutor.execute("push", "--set-upstream", "origin", branchName);
	}


	public String pushTag(Ref tag) throws Exception {
		getLog().info("Pushing Tag " + tag.getName());

		return gitExecutor.execute("push", "origin", getVersionFromTag(tag));
	}


	public String push() throws Exception {
		getLog().info("Pushing commit");

		return gitExecutor.execute("push");
	}


	private Ref findLocalBranch(String branch) throws Exception {
		getLog().info("Looking for local branch " + branch);

		for (Ref b : getGit().branchList().call()) {
			if (branch.equals(b.getName().toLowerCase().replace("refs/heads/", ""))) {
				return b;
			}
		}

		return null;
	}


	public Ref findBranch(String branch) throws Exception {
		getLog().info("Looking for branch " + branch);

		for (Ref b : getGit().branchList().setListMode(ListMode.REMOTE).call()) {
			if (branch.equals(getSimpleBranchName(b))) {
				return b;
			}
		}

		return null;
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

		return new MojoExecutionException(message);
	}


	public RevCommit revertCommit(RevCommit commit) throws Exception {
		getLog().info("Reverting commit");
		return getGit().revert().include(commit).setStrategy(MergeStrategy.OURS).call();
	}

	public String getSimpleBranchName(Ref ref) {
		return replaceAll(ref);
	}


	public String replaceAll(Ref ref) {
		String result = ref.getName();

		for (String replace : getGitFlowPattern().getPrefixToReplace()) {
			result = result.replace(replace, "");
		}

		return result;
	}


	public String getReleaseFromVersion(String fullVersion) {
		Matcher matcher = getGitFlowPattern().getTagVersionPattern().matcher(fullVersion);

		if (matcher.find()) {
			return String.format("%s.%s", matcher.group(1), matcher.group(2));
		}

		return null;
	}


	public String getVersionFromTag(Ref tag) {
		return tag.getName().replace(getGitFlowPattern().getPrefixGitTag() + getGitFlowPattern().getGitSeparator(), "");
	}


	public String buildDevBranchName(String branchType, String version, String branchName) {
		return branchType + getGitFlowPattern().getGitSeparator() + version + getGitFlowPattern().getGitSeparator() + branchName;
	}


	public String buildReleaseBranchName(String version) {
		return getGitFlowPattern().getPrefixGitRelease() + getGitFlowPattern().getGitSeparator() + version;
	}


	public String buildHotfixBranchName(String version, String branchName) {
		return getGitFlowPattern().getPrefixGitHotfix() + getGitFlowPattern().getGitSeparator() + version + getGitFlowPattern().getGitSeparator() + branchName;
	}


	public String buildHotfixBranchName(String branchName) {
		return getGitFlowPattern().getPrefixGitHotfix() + getGitFlowPattern().getGitSeparator() + branchName;
	}


	public String buildRemoteBranchName(Ref ref) {
		return getGitFlowPattern().getPrefixGitHeads() + replaceAll(ref);
	}


	public List<String> branchTypeToArray() {
		List<String> values = new ArrayList<String>();

		for (DefaultBranchType type : DefaultBranchType.values()) {
			values.add(type.name());
		}

		return values;
	}


	public Map<String, String> validateFullBranchName(String branchName) throws Exception {
		String errorMessage 		= "The fullBranchName must be <branchType=[feature|bugfix]>/<releaseVersion>/<branchName>. EX: feature/1.1.0/issue3456";
		String[] pattern 			= branchName.split("/");
		Map<String, String> result 	= new HashMap<String, String>();

		if (pattern.length != 3) {
			throw new MojoExecutionException(errorMessage);
		}

		if (!branchTypeToArray().contains(pattern[0])) {
			throw new MojoExecutionException(errorMessage);
		}

		result.put("branchType", pattern[0]);
		result.put("version", pattern[1]);
		result.put("branchName", pattern[2]);

		return result;
	}
	

	protected Log getLog() {
		return log;
	}

	
	public GitFlowPattern getGitFlowPattern() {
		return gitFlowPattern;
	}
}
