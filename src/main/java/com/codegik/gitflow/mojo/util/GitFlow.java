package com.codegik.gitflow.mojo.util;

import static com.codegik.gitflow.AbstractGitFlowMojo.PREFIX_TAG;
import static com.codegik.gitflow.AbstractGitFlowMojo.RELEASE_VERSION_PATTERN;
import static com.codegik.gitflow.AbstractGitFlowMojo.SEPARATOR;
import static com.codegik.gitflow.AbstractGitFlowMojo.TAG_VERSION_PATTERN;

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

import com.codegik.gitflow.command.CommandExecutor;


public class GitFlow extends BaseGitFlow {


	public GitFlow(Log log, CommandExecutor gitExecutor) {
		this(log, gitExecutor, new File("."));
	}


	public GitFlow(Log log, CommandExecutor gitExecutor, File repository) {
		super(log, gitExecutor, repository);
	}


	public void validadePatternReleaseVersion(String version) throws Exception {
		if (!RELEASE_VERSION_PATTERN.matcher(version).find()) {
			throw new MojoExecutionException("The version pattern is " + RELEASE_VERSION_PATTERN.toString() + "  EX: 1.3");
		}
	}


	public Ref validadeReleaseVersion(String version) throws Exception {
		validadePatternReleaseVersion(version);

		String fullVersion = BranchUtil.buildReleaseBranchName(version);
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
				if (!tags.get(index).getName().startsWith(PREFIX_TAG + SEPARATOR + releaseVersion + ".")) {
					tags.remove(index);
					continue;
				}
				index++;
			}
		}

		// Filtra a lista de tags pelo padrao de nomenclatura
		index = 0;
		while (index < tags.size()) {
			if (!TAG_VERSION_PATTERN.matcher(BranchUtil.getVersionFromTag(tags.get(index))).find()) {
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

		return releaseVersion < currVersion;
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
}
