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

    @Parameter( property = "tag", required = true )
	private String tag;


	@Override
	public void run() throws Exception {
		Ref tag = findTag(getTag());

		if (tag == null) {
			throw buildMojoException("Could not find tag " + getTag());
		}

//		Gettting last tag
//		Ref found = findTag(getGit().describe().call());

		checkoutBranchForced(MASTER);

		MergeResult merge = merge(tag, MergeStrategy.THEIRS);

		if (!merge.getMergeStatus().isSuccessful()) {
			throw buildConflictExeption(merge, tag, MASTER, "finish-release -Dtag=" + getTag());
		}

		push("Pushing merge");

		/**
		 * TODO
		 * criar os metodos para deletar branches de feature, bugfix e release
		 */
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			reset(DEVELOP);
			checkoutBranchForced(DEVELOP);
			deleteTag(getTag());
		} catch (Exception e1) {;}
		throw buildMojoException("ERROR", e);
	}


	public String getTag() {
		return tag;
	}


	public void setTag(String tag) {
		this.tag = tag;
	}
}
