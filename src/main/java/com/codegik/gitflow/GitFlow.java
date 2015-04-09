package com.codegik.gitflow;

import static com.codegik.gitflow.AbstractGitFlowMojo.PREFIX_TAG;
import static com.codegik.gitflow.AbstractGitFlowMojo.SEPARATOR;
import static com.codegik.gitflow.AbstractGitFlowMojo.TAG_VERSION_PATTERN;
import static com.codegik.gitflow.AbstractGitFlowMojo.RELEASE_VERSION_PATTERN;

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


	public List<String> deleteLocalBranch(String branchName) throws Exception {
		getLog().info("Deleting local branch " + branchName);
		Ref branchRef = findLocalBranch(branchName);

		if (branchRef == null) {
			throw buildMojoException("Branch " + branchName + " not found");
		}

		return deleteBranch(branchRef);
	}


	public List<String> deleteBranch(String version, BranchType branchType) throws Exception {
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
//	        return getGit().push().setPushTags().setPushAll().setCredentialsProvider(credentialsProvider).call();
	        return getGit().push().setCredentialsProvider(credentialsProvider).call();
		}

		return getGit().push().call();
	}


	public Iterable<PushResult> pushAll() throws Exception {
		getLog().info("Pushing all");

		if (username != null && password != null) {
			CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
	        return getGit().push().setPushTags().setPushAll().setCredentialsProvider(credentialsProvider).call();
		}

		return getGit().push().setPushTags().setPushAll().call();
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


	public Ref findLocalBranch(String branch) throws Exception {
		getLog().info("Looking for local branch " + branch);

		for (Ref b : getGit().branchList().setListMode(ListMode.ALL).call()) {
			if (branch.equals(b.getName().toLowerCase().replace("refs/heads/", ""))) {
				return b;
			}
		}

		return null;
	}


	public Ref findLasTag() throws Exception {
		return findLasTag(null);
	}

	public Ref findLasTag(String releaseVersion) throws Exception {
		final RevWalk walk = new RevWalk(getGit().getRepository());
		List<Ref> tags = getGit().tagList().call();

		if (releaseVersion != null) {
			for (int i = 0; i < tags.size(); i++) {
				if (!tags.get(i).getName().startsWith(PREFIX_TAG + SEPARATOR + releaseVersion)) {
					tags.remove(i);
					i = 0;
				}
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


	public Ref findTag(String tag) throws Exception {
		getLog().info("Looking for tag " + tag);

		Matcher matcher = TAG_VERSION_PATTERN.matcher(tag);

        if (matcher.find()) {
        	String matchTag = matcher.group(0);
        	for (Ref b : getGit().tagList().call()) {
        		if (matchTag.equals(b.getName().toLowerCase().replace(PREFIX_TAG + SEPARATOR, ""))) {
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


	public String incrementVersion(Ref lastTag) throws Exception {
		return incrementVersion(lastTag.getName().replace(PREFIX_TAG + SEPARATOR, ""));
	}


	public String incrementVersion(String version) throws Exception {
		Matcher matcher = TAG_VERSION_PATTERN.matcher(version);

		if (matcher.find()) {
			String releaseVersion 	= String.format("%s.%s", matcher.group(1), matcher.group(2));
			Ref lastTag 			= findLasTag(releaseVersion);
			String newVersion 		= null;
			Integer increment 		= null;

			if (lastTag == null) {
				newVersion = matcher.group(3);
			} else {
				newVersion 	= lastTag.getName().replace(PREFIX_TAG + SEPARATOR, "");
				matcher 	= TAG_VERSION_PATTERN.matcher(newVersion);
				newVersion 	= matcher.find() ? matcher.group(3) : null;
			}

			increment = new Integer(newVersion);
			increment++;

			return String.format("%s.%s.%s", matcher.group(1), matcher.group(2), increment.toString());
		}

		throw buildMojoException("The version " + version + " does not match with pattern " + TAG_VERSION_PATTERN.toString());
	}


	public Stage defineStageForMerge(String currentVersion, String releaseBranchVersion) throws Exception {
		Matcher matcherCurrentVersion 		= TAG_VERSION_PATTERN.matcher(currentVersion);
		Matcher matcherReleaseBranchVersion = RELEASE_VERSION_PATTERN.matcher(releaseBranchVersion);

		if (!matcherCurrentVersion.find()) {
			throw buildMojoException("The currentVersion " + currentVersion + " does not match with pattern " + TAG_VERSION_PATTERN.toString());
		}

		if (!matcherReleaseBranchVersion.find()) {
			throw buildMojoException("The releaseBranchVersion " + releaseBranchVersion + " does not match with pattern " + RELEASE_VERSION_PATTERN.toString());
		}

		Integer currVersion 	= new Integer(matcherCurrentVersion.group(2));
		Integer releaseVersion 	= new Integer(matcherReleaseBranchVersion.group(2));

		return currVersion < releaseVersion ? Stage.THEIRS : Stage.OURS;
	}


	protected Log getLog() {
		return log;
	}
}
