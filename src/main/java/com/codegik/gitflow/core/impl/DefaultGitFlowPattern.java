package com.codegik.gitflow.core.impl;

import java.util.regex.Pattern;

import com.codegik.gitflow.core.GitFlowPattern;

public class DefaultGitFlowPattern implements GitFlowPattern {
	private Pattern tagVersionPattern = Pattern.compile("([0-9]{1,})\\.([0-9]{1,})\\.([0-9]{1,})");
	private Pattern releaseVersionPattern = Pattern.compile("([0-9]{1,})\\.([0-9]{1,})");

	
	@Override
	public String getOriginName() {
		return "origin";
	}

	@Override
	public String getMasterName() {
		return "master";
	}

	@Override
	public String getDevelopName() {
		return "develop";
	}

	@Override
	public String getSuffixRelease() {
		return ".0";
	}

	@Override
	public String getPrefixGitHotfix() {
		return "hotfix";
	}

	@Override
	public String getPrefixGitTag() {
		return "refs/tags";
	}

	@Override
	public String getPrefixGitRelease() {
		return "release";
	}

	@Override
	public Pattern getTagVersionPattern() {
		return tagVersionPattern;
	}

	@Override
	public Pattern getReleaseVersionPattern() {
		return releaseVersionPattern;
	}

	@Override
	public String getGitSeparator() {
		return "/";
	}

	@Override
	public String getPomFileName() {
		return "pom.xml";
	}

	@Override
	public String getPrefixGitHeads() {
		return "refs/heads/";
	}

	@Override
	public String[] getPrefixToReplace() {
		return new String[]{"refs/tags/", "refs/heads/", "refs/remotes/origin/"};
	}

}
