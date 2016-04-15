package com.codegik.gitflow.core;

import java.util.regex.Pattern;


public interface GitFlowPattern {
	public String getOriginName();
	public String getMasterName();
	public String getDevelopName();
	public String getSuffixRelease();
	public String getPrefixGitHotfix();
	public String getPrefixGitTag();
	public String getPrefixGitRelease();
	public String getPrefixGitHeads();
	public Pattern getTagVersionPattern();
	public Pattern getReleaseVersionPattern();
	public String getGitSeparator();
	public String getPomFileName();
	public String[] getPrefixToReplace();
}
