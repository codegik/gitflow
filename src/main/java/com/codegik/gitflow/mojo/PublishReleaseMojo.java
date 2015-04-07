package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;

import com.codegik.gitflow.DefaultGitFlowMojo;


/**
 * Put last tag from release on master
 * Delete all related branches (release|feature|bugfix)
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "publish-release", aggregator = true)
public class PublishReleaseMojo extends DefaultGitFlowMojo {

    @Parameter( property = "version", required = true )
	private String version;


	@Override
	public void run() throws Exception {
		checkoutBranchForced(DEVELOP);

		// Gettting last tag
		Ref tagRef = findLasTag();

		checkoutBranchForced(MASTER);

		MergeResult merge = merge(tagRef, MergeStrategy.THEIRS);

		if (!merge.getMergeStatus().isSuccessful()) {
			throw buildConflictExeption(merge, tagRef, MASTER, "publish-release -Dversion=" + getVersion());
		}

		push("Pushing merge");

		deleteBranch(getVersion(), BranchType.feature);
		deleteBranch(getVersion(), BranchType.bugfix);
		deleteBranch(PREFIX_RELEASE + SEPARATOR + getVersion());
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			reset(MASTER);
			checkoutBranchForced(MASTER);
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
