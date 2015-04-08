package com.codegik.gitflow.mojo;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseCleanRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;
import com.codegik.gitflow.MergeGitFlow;


/**
 * Start new release branch from develop
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-release", aggregator = true)
public class FinishReleaseMojo extends AbstractGitFlowMojo {
	private Ref lastTag;

    @Parameter( property = "version", required = true )
	private String version;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		validadeVersion(getVersion());

		gitFlow.checkoutBranchForced(DEVELOP);

		Ref ref = gitFlow.findBranch(PREFIX_RELEASE + SEPARATOR + getVersion());

		MergeGitFlow mergeGitFlow = new MergeGitFlow();
		mergeGitFlow.setBranchName(DEVELOP);
		mergeGitFlow.setErrorMessage("finish-release -Dversion=" + getVersion());
		mergeGitFlow.setTargetRef(ref);

		gitFlow.merge(mergeGitFlow);

		getLog().info("Updating pom version");
		ReleaseDescriptor descriptor 	= gitFlow.buildReleaseDescriptor();
		ReleaseEnvironment environment 	= gitFlow.buildDefaultReleaseEnvironment();
		ReleaseCleanRequest clean		= new ReleaseCleanRequest();
		List<MavenProject> projects		= buildMavenProjects();

		clean.setReactorProjects(projects);
		clean.setReleaseDescriptor(descriptor);

		getReleaseManager().prepare(descriptor, environment, projects);
		getReleaseManager().clean(clean);

		lastTag = gitFlow.findLasTag();

		getLog().info("Commiting changed files");
		gitFlow.commit("[GitFlow::finishi-release] Finish release branch " + getVersion());

		gitFlow.push("Pushing commit");
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			gitFlow.reset(DEVELOP);
			gitFlow.checkoutBranchForced(DEVELOP);
			if (lastTag != null) {
				gitFlow.deleteTag(lastTag.getName());
			}
		} catch (Exception e1) {;}
		throw buildMojoException("ERROR", e);
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}

}
