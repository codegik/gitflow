package com.codegik.gitflow.mojo.util;

import static com.codegik.gitflow.AbstractGitFlowMojo.PREFIX_HOTFIX;
import static com.codegik.gitflow.AbstractGitFlowMojo.PREFIX_RELEASE;
import static com.codegik.gitflow.AbstractGitFlowMojo.PREFIX_TAG;
import static com.codegik.gitflow.AbstractGitFlowMojo.SEPARATOR;
import static com.codegik.gitflow.AbstractGitFlowMojo.TAG_VERSION_PATTERN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.maven.plugin.MojoExecutionException;
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


	public static String getReleaseFromVersion(String fullVersion) {
		Matcher matcher = TAG_VERSION_PATTERN.matcher(fullVersion);

		if (matcher.find()) {
			return String.format("%s.%s", matcher.group(1), matcher.group(2));
		}

		return null;
	}


	public static String getVersionFromTag(Ref tag) {
		return tag.getName().replace(PREFIX_TAG + SEPARATOR, "");
	}


	public static String buildDevBranchName(String branchType, String version, String branchName) {
		return branchType+ SEPARATOR + version + SEPARATOR + branchName;
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


	public static List<String> branchTypeToArray() {
		List<String> values = new ArrayList<String>();

		for (BranchType type : BranchType.values()) {
			values.add(type.name());
		}

		return values;
	}


	public static Map<String, String> validateFullBranchName(String branchName) throws Exception {
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


}
