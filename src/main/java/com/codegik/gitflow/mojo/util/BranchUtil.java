package com.codegik.gitflow.mojo.util;

import static com.codegik.gitflow.AbstractGitFlowMojo.PREFIX_HOTFIX;
import static com.codegik.gitflow.AbstractGitFlowMojo.PREFIX_RELEASE;
import static com.codegik.gitflow.AbstractGitFlowMojo.PREFIX_TAG;
import static com.codegik.gitflow.AbstractGitFlowMojo.SEPARATOR;

import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.AbstractGitFlowMojo.BranchType;


public class BranchUtil {
	private static final String[] replaces = new String[]{"refs/tags/", "refs/heads/", "refs/remotes/origin/"};
	private static final String REMOTE_PREFIX = "refs/heads/";


	public static String getSimpleBranchName(Ref ref) {
		return replaceAll(ref);
	}


	public static String replaceAll(Ref ref) {
		String result = ref.getName();

		for (String replace : replaces) {
			result = result.replace(replace, "");
		}

		return result;
	}


	public static String getVersionFromTag(Ref tag) {
		return tag.getName().replace(PREFIX_TAG + SEPARATOR, "");
	}


	public static String buildDevBranchName(BranchType branchType, String version, String branchName) {
		return branchType.toString() + SEPARATOR + version + SEPARATOR + branchName;
	}


	public static String buildReleaseBranchName(String version) {
		return PREFIX_RELEASE + SEPARATOR + version;
	}


	public static String buildHotfixBranchName(String version) {
		return PREFIX_HOTFIX + SEPARATOR + version;
	}


	public static String buildRemoteBranchName(Ref ref) {
		return REMOTE_PREFIX + replaceAll(ref);
	}
}
