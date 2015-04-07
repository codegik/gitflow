package com.codegik.gitflow.mojo;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseCleanRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.Ref;

import com.codegik.gitflow.DefaultGitFlowMojo;


/**
 * Start new release branch from develop
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "finish-release", aggregator = true)
public class FinishReleaseMojo extends DefaultGitFlowMojo {
	private Ref lastTag;

    @Parameter( property = "version", required = true )
	private String version;


	@Override
	public void run() throws Exception {
		validadeVersion(getVersion());

		checkoutBranch(DEVELOP);

		Ref ref = findBranch(PREFIX_RELEASE + SEPARATOR + getVersion());
		MergeResult merge = merge(ref);

		if (!merge.getMergeStatus().isSuccessful()) {
			throw buildConflictExeption(merge, ref, DEVELOP, getVersion());
		}

		getLog().info("Updating pom version");
		ReleaseDescriptor descriptor 	= buildReleaseDescriptor();
		ReleaseCleanRequest clean		= new ReleaseCleanRequest();
		ReleaseEnvironment environment 	= buildDefaultReleaseEnvironment();
		List<MavenProject> projects		= buildMavenProjects();

		clean.setReactorProjects(projects);
		clean.setReleaseDescriptor(descriptor);

		getReleaseManager().prepare(descriptor, environment, projects);
		getReleaseManager().clean(clean);

		lastTag = findTag(getGit().describe().call());

		getLog().info("Commiting changed files");
		commit("[GitFlow::finishi-release] Finish release branch " + getVersion());

		push("Pushing commit");
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			reset(DEVELOP);
			checkoutBranchForced(DEVELOP);
			deleteTag(lastTag.getName());
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
