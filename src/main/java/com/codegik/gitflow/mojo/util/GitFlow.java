package com.codegik.gitflow.mojo.util;

import static com.codegik.gitflow.AbstractGitFlowMojo.PREFIX_RELEASE;
import static com.codegik.gitflow.AbstractGitFlowMojo.PREFIX_TAG;
import static com.codegik.gitflow.AbstractGitFlowMojo.RELEASE_VERSION_PATTERN;
import static com.codegik.gitflow.AbstractGitFlowMojo.SEPARATOR;
import static com.codegik.gitflow.AbstractGitFlowMojo.TAG_VERSION_PATTERN;

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
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import com.codegik.gitflow.AbstractGitFlowMojo.BranchType;
import com.codegik.gitflow.command.CommandExecutor;


public class GitFlow {
	private Git git;
	private Log log;
	private File repository;
	private CommandExecutor gitExecutor;


	public GitFlow(Log log, CommandExecutor gitExecutor) {
		this(log, gitExecutor, new File("."));
	}


	public GitFlow(Log log, CommandExecutor gitExecutor, File repository) {
		this.log 		= log;
		this.repository = repository;
		this.gitExecutor= gitExecutor;
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


	public void validadePatternReleaseVersion(String version) throws Exception {
		if (!RELEASE_VERSION_PATTERN.matcher(version).find()) {
			throw new MojoExecutionException("The version pattern is " + RELEASE_VERSION_PATTERN.toString() + "  EX: 1.3");
		}
	}


	public Ref validadeReleaseVersion(String version) throws Exception {
		validadePatternReleaseVersion(version);

		Ref ref = findBranch(BranchUtil.buildReleaseBranchName(version));

		if (ref == null) {
			throw new MojoExecutionException("The version " + PREFIX_RELEASE + SEPARATOR + version + "  not found");
		}

		return ref;
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
		String simpleName = BranchUtil.getSimpleBranchName(branchRef);

		gitExecutor.execute("branch", "-D", simpleName);

		return gitExecutor.execute("push", "origin", ":" + simpleName);
	}


	public List<String> deleteBranch(Ref branchRef) throws Exception {
		return getGit().branchDelete().setForce(true).setBranchNames(branchRef.getName()).call();
	}


	public String deleteRemoteBranch(String branchName) throws Exception {
		getLog().info("Deleting branch " + branchName);
		Ref branchRef = findBranch(branchName);

		if (branchRef == null) {
			throw new MojoExecutionException("Branch " + branchName + " not found");
		}

		return deleteRemoteBranch(branchRef);
	}


	public List<String> deleteLocalBranch(String branchName) throws Exception {
		getLog().info("Deleting local branch " + branchName);
		Ref branchRef = findLocalBranch(branchName);

		if (branchRef == null) {
			throw new MojoExecutionException("Branch " + branchName + " not found");
		}

		return deleteBranch(branchRef);
	}


	public void deleteRemoteBranch(String version, BranchType branchType) throws Exception {
		getLog().info("Deleting " + branchType.toString() + " branch of release " + version);

		String release = branchType.toString() + SEPARATOR + version;

		for (Ref b : getGit().branchList().setListMode(ListMode.ALL).call()) {
			if (b.getName().contains(release)) {
				deleteRemoteBranch(b);
				getLog().info(" > Deleted " + b.getName());
			}
		}
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
		return createBranch(branchName, null);
	}


	public Ref createBranch(String branchName, String logMessage) throws Exception {
		getLog().info("Creating branch " + branchName + (logMessage == null ? "" : " " + logMessage));
		return getGit().checkout().setCreateBranch(true).setName(branchName).call();
	}


	public RevCommit commit(String message) throws Exception {
		getGit().add().addFilepattern(".").call();
		return getGit().commit().setAll(true).setMessage(message).call();
	}


	public String pull() throws Exception {
		return gitExecutor.execute("pull");
	}


	public String pushBranch(String branchName) throws Exception {
		return gitExecutor.execute("push", "--set-upstream", "origin", branchName);
	}


	public String push(String logMessage) throws Exception {
		getLog().info(logMessage == null ? "Pushing commit" : logMessage);

		return gitExecutor.execute("push");
	}


	public String pushAll() throws Exception {
		getLog().info("Pushing all");

		gitExecutor.execute("push", "--all", "origin");

		return gitExecutor.execute("push", "--tags", "origin");
	}


	public Ref findBranch(String branch) throws Exception {
		getLog().info("Looking for branch " + branch);

		for (Ref b : getGit().branchList().setListMode(ListMode.REMOTE).call()) {
			if (branch.equals(b.getName().toLowerCase().replace("refs/remotes/origin/", ""))) {
				return b;
			}
		}

		return null;
	}


	public Ref findLocalBranch(String branch) throws Exception {
		getLog().info("Looking for local branch " + branch);

		for (Ref b : getGit().branchList().setListMode(ListMode.ALL).call()) {
			if (branch.equals(b.getName().toLowerCase().replace("refs/heads/", ""))) {
				return b;
			}
		}

		return null;
	}


	/**
	 * Find last remote tag off repository
	 *
	 * @return Ref - Reference off a remote tag
	 * @throws Exception
	 */
	public Ref findLastTag() throws Exception {
		return findLastTag(null);
	}


	/**
	 * Find last remote tag off release version. Ex: tags/1.4*
	 *
	 * @param String releaseVersion - Version off release. Ex: 1.4
	 * @return Ref - Reference off a remote tag
	 * @throws Exception
	 */
	public Ref findLastTag(String releaseVersion) throws Exception {
		final RevWalk walk = new RevWalk(getGit().getRepository());
		List<Ref> tags = getGit().tagList().call();

		if (releaseVersion != null) {
			int i = 0;
			while (i < tags.size()) {
				if (!tags.get(i).getName().startsWith(PREFIX_TAG + SEPARATOR + releaseVersion)) {
					tags.remove(i);
					continue;
				}
				i++;
			}
		}

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

		return tags.size() > 0 ? tags.get(tags.size()-1) : null;
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


	/**
	 * Increase version without validation
	 *
	 * @param String version - Pom version. Ex: 1.4.3
	 * @return The increased version. Ex: 1.4.4
	 * @throws Exception
	 */
	public String increaseVersion(String version) throws Exception {
		Matcher matcher = TAG_VERSION_PATTERN.matcher(version);

		if (matcher.find()) {
			Integer increment = new Integer(matcher.group(3));
			increment++;
			return String.format("%s.%s.%s", matcher.group(1), matcher.group(2), increment.toString());
		}

		throw new MojoExecutionException("The version " + version + " does not match with pattern " + TAG_VERSION_PATTERN.toString());
	}


	/**
	 * Increase version based on last tag off release
	 *
	 * @param Ref lastTag - Reference to a remote tag. Ex: tags/1.4.3
	 * @return The increased version. Ex: 1.4.4
	 * @throws Exception
	 */
	public String increaseVersionBasedOnTag(Ref lastTag) throws Exception {
		return increaseVersionBasedOnTag(BranchUtil.getVersionFromTag(lastTag));
	}


	/**
	 * Increase version based on last tag off release
	 *
	 * @param String version - Pom version. Ex: 1.4.3
	 * @return The increased version. Ex: 1.4.4
	 * @throws Exception
	 */
	public String increaseVersionBasedOnTag(String version) throws Exception {
		Matcher matcher = TAG_VERSION_PATTERN.matcher(version);

		if (matcher.find()) {
			String releaseVersion 	= String.format("%s.%s", matcher.group(1), matcher.group(2));
			Ref lastTag 			= findLastTag(releaseVersion);
			String newVersion 		= null;
			Integer increment 		= null;

			if (lastTag == null) {
				newVersion = matcher.group(3);
			} else {
				newVersion 	= BranchUtil.getVersionFromTag(lastTag);
				matcher 	= TAG_VERSION_PATTERN.matcher(newVersion);
				newVersion 	= matcher.find() ? matcher.group(3) : null;
			}

			increment = new Integer(newVersion);
			increment++;

			return String.format("%s.%s.%s", matcher.group(1), matcher.group(2), increment.toString());
		}

		throw new MojoExecutionException("The version " + version + " does not match with pattern " + TAG_VERSION_PATTERN.toString());
	}


	/**
	 * Returns
	 * The value 0 if currentVersion is equal to the releaseBranchVersion
	 * A value less than 0 if currentVersion is numerically less than the releaseBranchVersion
	 * A value greater than 0 if currentVersion is numerically greater than the releaseBranchVersion
	 *
	 * @param currentVersion - The current version of pom.xml to compare
	 * @param releaseBranchVersion - The release version to compare
	 * @return org.eclipse.jgit.api.Stage
	 * @throws Exception
	 */
	public Stage defineStageForMerge(String currentVersion, String releaseBranchVersion) throws Exception {
		Matcher matcherCurrentVersion 		= TAG_VERSION_PATTERN.matcher(currentVersion);
		Matcher matcherReleaseBranchVersion = RELEASE_VERSION_PATTERN.matcher(releaseBranchVersion);

		if (!matcherCurrentVersion.find()) {
			throw new MojoExecutionException("The currentVersion " + currentVersion + " does not match with pattern " + TAG_VERSION_PATTERN.toString());
		}

		if (!matcherReleaseBranchVersion.find()) {
			throw new MojoExecutionException("The releaseBranchVersion " + releaseBranchVersion + " does not match with pattern " + RELEASE_VERSION_PATTERN.toString());
		}

		Integer currVersion 	= new Integer(matcherCurrentVersion.group(2));
		Integer releaseVersion 	= new Integer(matcherReleaseBranchVersion.group(2));

		return currVersion < releaseVersion ? Stage.THEIRS : Stage.OURS;
	}


	/**
	 * Returns
	 * The value 0 if firstVersion is equal to the secondVersion
	 * A value less than 0 if firstVersion is numerically less than the secondVersion
	 * A value greater than 0 if firstVersion is numerically greater than the secondVersion
	 *
	 * @param firstVersion - The first version to compare
	 * @param secondVersion - The first version to compare
	 * @return Integer
	 * @throws Exception
	 */
	public Integer whatIsTheBigger(String firstVersion, String secondVersion) throws Exception {
		getLog().info("What is bigger " + firstVersion + " -> " + secondVersion + "?");
		Matcher matcherFirstVersion 	= TAG_VERSION_PATTERN.matcher(firstVersion);
		Matcher matcherSecondVersion 	= TAG_VERSION_PATTERN.matcher(secondVersion);

		if (!matcherFirstVersion.find()) {
			throw new MojoExecutionException("The firstVersion " + firstVersion + " does not match with pattern " + TAG_VERSION_PATTERN.toString());
		}

		if (!matcherSecondVersion.find()) {
			throw new MojoExecutionException("The secondVersion " + secondVersion + " does not match with pattern " + TAG_VERSION_PATTERN.toString());
		}

		Integer intFirstVersion 	= new Integer(matcherFirstVersion.group(2));
		Integer intSecondVersion 	= new Integer(matcherSecondVersion.group(2));

		if (intFirstVersion.compareTo(intSecondVersion) == 0) {
			intFirstVersion 	= new Integer(matcherFirstVersion.group(3));
			intSecondVersion 	= new Integer(matcherSecondVersion.group(3));
		}

		return intFirstVersion.compareTo(intSecondVersion);
	}


	public RevCommit revertCommit(RevCommit commit) throws Exception {
		return getGit().revert().include(commit).setStrategy(MergeStrategy.OURS).call();
	}


	protected Log getLog() {
		return log;
	}

}
