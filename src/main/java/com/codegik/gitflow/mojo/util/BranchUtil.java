package com.codegik.gitflow.mojo.util;

import static com.codegik.gitflow.AbstractGitFlowMojo.PREFIX_RELEASE;
import static com.codegik.gitflow.AbstractGitFlowMojo.PREFIX_TAG;
import static com.codegik.gitflow.AbstractGitFlowMojo.SEPARATOR;

import org.eclipse.jgit.lib.Ref;


public class BranchUtil {

	public static String getSimpleBranchName(Ref ref) {
		String result = ref.getName();
		String[] replaces = new String[]{"refs/tags/", "refs/heads/", "refs/remotes/origin/"};

		for (String replace : replaces) {
			result = result.replace(replace, "");
		}

		return result;
	}


	public static String getVersionFromTag(Ref tag) {
		return tag.getName().replace(PREFIX_TAG + SEPARATOR, "");
	}


	public static String buildReleaseBranchName(String version) {
		return PREFIX_RELEASE + SEPARATOR + version;
	}
}
