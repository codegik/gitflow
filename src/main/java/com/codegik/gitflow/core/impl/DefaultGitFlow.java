package com.codegik.gitflow.core.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevWalk;

import com.codegik.gitflow.core.GitFlow;
import com.codegik.gitflow.core.GitFlowPattern;


public class DefaultGitFlow extends GitFlow {
	

	public DefaultGitFlow(GitFlowPattern gitFlowPattern, Log log) {
		this(gitFlowPattern, log, new File("."));
	}


	public DefaultGitFlow(GitFlowPattern gitFlowPattern, Log log, File repository) {
		super(gitFlowPattern, log, repository);
	}


	public void validadePatternReleaseVersion(String version) throws Exception {
		if (!getGitFlowPattern().getReleaseVersionPattern().matcher(version).matches()) {
			throw new MojoExecutionException("The version pattern is " + getGitFlowPattern().getReleaseVersionPattern().toString() + "  EX: 1.3");
		}
	}


	public Ref validadeReleaseVersion(String version) throws Exception {
		validadePatternReleaseVersion(version);

		String fullVersion = buildReleaseBranchName(version);
		Ref ref = findBranch(fullVersion);

		if (ref == null) {
			throw new MojoExecutionException("The version " + fullVersion + "  not found");
		}

		return ref;
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
		final RevWalk walk 	= new RevWalk(getGit().getRepository());
		List<Ref> tags 		= getGit().tagList().call();
		int index 			= 0;

		// Filtra a lista de tags pela release informada
		if (releaseVersion != null) {
			while (index < tags.size()) {
				if (!tags.get(index).getName().startsWith(getGitFlowPattern().getPrefixGitTag() + getGitFlowPattern().getGitSeparator() + releaseVersion + ".")) {
					tags.remove(index);
					continue;
				}
				index++;
			}
		}

		// Filtra a lista de tags pelo padrao de nomenclatura
		index = 0;
		while (index < tags.size()) {
			if (!getGitFlowPattern().getTagVersionPattern().matcher(getVersionFromTag(tags.get(index))).matches()) {
				tags.remove(index);
				continue;
			}
			index++;
		}

		// Ordena a lista de tags baseado na data de criacao
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


	/**
	 * Increase version without validation
	 *
	 * @param String version - Pom version. Ex: 1.4.3
	 * @return The increased version. Ex: 1.4.4
	 * @throws Exception
	 */
	public String increaseVersion(String version) throws Exception {
		Matcher matcher = getGitFlowPattern().getTagVersionPattern().matcher(version);

		if (matcher.matches()) {
			Integer increment = new Integer(matcher.group(3));
			increment++;
			return String.format("%s.%s.%s", matcher.group(1), matcher.group(2), increment.toString());
		}

		throw new MojoExecutionException("The version " + version + " does not match with pattern " + getGitFlowPattern().getTagVersionPattern().toString());
	}


	/**
	 * Increase version based on last tag off release
	 *
	 * @param Ref lastTag - Reference to a remote tag. Ex: tags/1.4.3
	 * @return The increased version. Ex: 1.4.4
	 * @throws Exception
	 */
	public String increaseVersionBasedOnTag(Ref lastTag) throws Exception {
		return increaseVersionBasedOnTag(getVersionFromTag(lastTag));
	}


	/**
	 * Increase version based on last tag off release
	 *
	 * @param String version - Pom version. Ex: 1.4.3
	 * @return The increased version. Ex: 1.4.4
	 * @throws Exception
	 */
	public String increaseVersionBasedOnTag(String version) throws Exception {
		Matcher matcher = getGitFlowPattern().getTagVersionPattern().matcher(version);

		if (matcher.matches()) {
			String releaseVersion 	= String.format("%s.%s", matcher.group(1), matcher.group(2));
			Ref lastTag 			= findLastTag(releaseVersion);
			String newVersion 		= null;
			Integer increment 		= null;

			if (lastTag == null) {
				newVersion = matcher.group(3);
			} else {
				newVersion 	= getVersionFromTag(lastTag);
				matcher 	= getGitFlowPattern().getTagVersionPattern().matcher(newVersion);
				newVersion 	= matcher.matches() ? matcher.group(3) : null;
			}

			increment = new Integer(newVersion);
			increment++;

			return String.format("%s.%s.%s", matcher.group(1), matcher.group(2), increment.toString());
		}

		throw new MojoExecutionException("The version " + version + " does not match with pattern " + getGitFlowPattern().getTagVersionPattern().toString());
	}


	/**
	 * Returns Stage.OURS if releaseBranchVersion is smaller than currentVersion or returns Stage.THEIRS otherwise
	 *
	 * @param currentVersion - The current version of pom.xml to compare
	 * @param releaseBranchVersion - The release version to compare
	 * @return org.eclipse.jgit.api.Stage
	 * @throws Exception
	 */
	public Stage defineStageForMerge(String currentVersion, String releaseBranchVersion) throws Exception {
		return Boolean.TRUE.equals(isReleaseSmallerThanCurrentVersion(releaseBranchVersion, currentVersion)) ? Stage.OURS : Stage.THEIRS;
	}


	/**
	 * Returns true if releaseBranchVersion is smaller than currentVersion or returns false otherwise
	 *
	 * @param releaseBranchVersion - The release version to compare
	 * @param currentVersion - The current version of pom.xml to compare
	 * @return Boolean
	 * @throws Exception
	 */
	public Boolean isReleaseSmallerThanCurrentVersion(String releaseBranchVersion, String currentVersion) throws Exception {
		getLog().info("Is release smaller than currentVersion " + releaseBranchVersion + " -> " + currentVersion + "?");

		Matcher matcherReleaseBranchVersion = getGitFlowPattern().getReleaseVersionPattern().matcher(releaseBranchVersion);
		Matcher matcherCurrentVersion 		= getGitFlowPattern().getTagVersionPattern().matcher(currentVersion);

		if (!matcherCurrentVersion.matches()) {
			throw new MojoExecutionException("The currentVersion " + currentVersion + " does not match with pattern " + getGitFlowPattern().getTagVersionPattern().toString());
		}

		if (!matcherReleaseBranchVersion.matches()) {
			throw new MojoExecutionException("The releaseBranchVersion " + releaseBranchVersion + " does not match with pattern " + getGitFlowPattern().getTagVersionPattern().toString());
		}
		
		return compareVersions(matcherReleaseBranchVersion, matcherCurrentVersion) == -1;
	}
	
	
	private Integer compareVersions(Matcher version1, Matcher version2) throws MojoExecutionException {
		int maxGroup = version1.groupCount() < version2.groupCount() ? version1.groupCount() : version2.groupCount();
		
		for (int i = 1; i <= maxGroup; i++) {
			int intVersion1	= new Integer(version1.group(i));
			int intVersion2	= new Integer(version2.group(i));
			
			if (intVersion1 == intVersion2) {
				continue;
			}
			
			if (intVersion1 < intVersion2) {
				return -1; // the version1 is smaller than version2
			}
			
			return 1; // the version1 is greater than version2
		}
		
		return 0; // the version1 is iqual to version2
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

		Matcher matcherFirstVersion 	= getGitFlowPattern().getTagVersionPattern().matcher(firstVersion);
		Matcher matcherSecondVersion 	= getGitFlowPattern().getTagVersionPattern().matcher(secondVersion);

		if (!matcherFirstVersion.matches()) {
			throw new MojoExecutionException("The firstVersion " + firstVersion + " does not match with pattern " + getGitFlowPattern().getTagVersionPattern().toString());
		}

		if (!matcherSecondVersion.matches()) {
			throw new MojoExecutionException("The secondVersion " + secondVersion + " does not match with pattern " + getGitFlowPattern().getTagVersionPattern().toString());
		}

		Integer intFirstVersion 	= new Integer(matcherFirstVersion.group(2));
		Integer intSecondVersion 	= new Integer(matcherSecondVersion.group(2));

		if (intFirstVersion.compareTo(intSecondVersion) == 0) {
			intFirstVersion 	= new Integer(matcherFirstVersion.group(3));
			intSecondVersion 	= new Integer(matcherSecondVersion.group(3));
		}

		return intFirstVersion.compareTo(intSecondVersion);
	}
}
