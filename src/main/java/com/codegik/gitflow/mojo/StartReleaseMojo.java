package com.codegik.gitflow.mojo;

import java.util.regex.Matcher;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;


/**
 * Start new release branch from develop
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-release", aggregator = true)
public class StartReleaseMojo extends AbstractGitFlowMojo {
	private String branchName;

    @Parameter( property = "version", required = true )
	private String version;


	@Override
	public void run(GitFlow gitFlow) throws Exception {
		validadeReleaseVersion(getVersion());

		/**
		 * TODO
		 * Nao permitir criar a primeira release com 1.0, deve ser no minimo 1.1
		 * Verificar se ja existe uma tag com o 1.0 e negar a operacao
		 * FALTA TESTAR
		 */
		Matcher matcher = RELEASE_VERSION_PATTERN.matcher(getVersion());
		if (matcher.find()) {
			if (Integer.parseInt(matcher.group(2)) == 0) {
				throw buildMojoException("The first release must be 1.1 at least!");
			}
		}

		if (!gitFlow.getBranch().toLowerCase().equals(DEVELOP)) {
			throw buildMojoException("You must be on branch develop for execute this goal!");
		}

		setBranchName(PREFIX_RELEASE + SEPARATOR + getVersion());

		gitFlow.createBranch(getBranchName());

		String newVersion = getVersion();

		updatePomVersion(newVersion + SUFFIX_RELEASE);

		getLog().info("Commiting changed files");
		gitFlow.commit("[GitFlow::start-release] Create release branch " + getBranchName() + ": Bumped version number to " + newVersion + SUFFIX_RELEASE);
		gitFlow.push("Pushing commit");
	}


	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rolling back all changes");
			gitFlow.reset(DEVELOP);
			gitFlow.checkoutBranchForced(branchName);
			gitFlow.deleteLocalBranch(getBranchName());
		} catch (Exception e1) {;}
		throw buildMojoException("ERROR", e);
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}


	public String getBranchName() {
		return branchName;
	}


	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}
}
