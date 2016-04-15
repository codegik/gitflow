package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.core.impl.DefaultGitFlowMojo;


/**
 * Create branch develop
 * Set first version on file (1.0.0)
 * Create first tag (1.0.0)
 * To execute this goal the current branch must be master
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "init", aggregator = true)
public class InitMojo extends DefaultGitFlowMojo {

	@Parameter( property = "version" )
	private String version;

	
	@Override
	public void run() throws Exception {

		if (!getGitFlow().getBranch().equals(getGitFlow().getGitFlowPattern().getMasterName())) {
			throw new MojoExecutionException("You must be on branch master for execute this goal!");
		}

		String newVersion = "1.0";

		if (version != null) {
			getGitFlow().validadePatternReleaseVersion(version);
			newVersion = version;
		}

		Ref lastTag = getGitFlow().findLastTag(newVersion);
		newVersion = newVersion + getGitFlow().getGitFlowPattern().getSuffixRelease();

		if (lastTag != null) {
			String lastTagVer = getGitFlow().getVersionFromTag(getGitFlow().findLastTag());
			if (getGitFlow().whatIsTheBigger(newVersion, lastTagVer) <= 0) {
				newVersion = getGitFlow().increaseVersionBasedOnTag(lastTag);
			}
		}

		if (getGitFlow().findBranch(getGitFlow().getGitFlowPattern().getDevelopName()) != null) {
			throw new MojoExecutionException("The branch develop already exists!");
		}

		getGitFlow().createBranch(getGitFlow().getGitFlowPattern().getDevelopName());

		updatePomVersion(newVersion);
		compileProject();

		Ref tag = getGitFlow().tag(newVersion, "[GitFlow::init] Create tag " + newVersion);
		getGitFlow().commit("[GitFlow::init] Bumped version number to " + newVersion);
		getGitFlow().push();
		getGitFlow().pushTag(tag);

		getLog().info("Now your repository is ready to start a release");
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		throw new MojoExecutionException("ERROR", e);
	}
}
