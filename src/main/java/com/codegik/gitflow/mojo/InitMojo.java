package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;
import com.codegik.gitflow.mojo.util.BranchUtil;


/**
 * Create branch develop
 * Set first version on file (1.0.0)
 * Create first tag (1.0.0)
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "init", aggregator = true)
public class InitMojo extends AbstractGitFlowMojo {

	@Parameter( property = "version" )
	private String version;

	@Override
	public void run(GitFlow gitFlow) throws Exception {

		if (!gitFlow.getBranch().equals(MASTER)) {
			throw buildMojoException("You must be on branch master for execute this goal!");
		}

		String newVersion = "1.0";

		if (version != null) {
			gitFlow.validadePatternReleaseVersion(version);
			newVersion = version;
		}

		Ref lastTag = gitFlow.findLastTag(newVersion);
		newVersion = newVersion + SUFFIX_RELEASE;

		if (lastTag != null) {
			String lastTagVer = BranchUtil.getVersionFromTag(gitFlow.findLastTag());
			if (gitFlow.whatIsTheBigger(newVersion, lastTagVer) <= 0) {
				newVersion = gitFlow.incrementVersion(lastTag);
			}
		}

		if (gitFlow.findBranch(DEVELOP) == null) {
			gitFlow.createBranch(DEVELOP);

			updatePomVersion(newVersion);

			gitFlow.commit("[GitFlow::init] Bumped version number to " + newVersion);
			gitFlow.tag(newVersion, "[GitFlow::init] Create tag " + newVersion);
			gitFlow.pushAll();

			getLog().info("Now your repository is ready to start a release");
		}
	}

	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
	}
}
