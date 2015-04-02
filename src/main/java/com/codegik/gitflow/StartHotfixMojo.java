package com.codegik.gitflow;

import java.io.File;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.change.ProjectVersionChanger;
import org.codehaus.mojo.versions.change.VersionChange;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;
import org.eclipse.jgit.api.ResetCommand.ResetType;

import edu.emory.mathcs.backport.java.util.Arrays;


/**
 * Start new hotfix branch from master
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-hotfix", aggregator = true)
public class StartHotfixMojo extends AbstractGitFlowMojo {

	@Parameter( property = "branchName", required = true )
	private String branchName;


	@SuppressWarnings("unchecked")
	@Override
	public void run() throws Exception {
		setBranchName(PREFIX_HOTFIX + SEPARATOR + getBranchName());

		getLog().info("Reseting to branch " + MASTER);
		getGit().reset().setMode(ResetType.HARD).setRef(MASTER).call();
		getGit().checkout().setCreateBranch(false).setForce(true).setName(MASTER).call();

		ReleaseDescriptor descriptor 	= buildReleaseDescriptor();
		ReleaseEnvironment environment 	= buildDefaultReleaseEnvironment();

		getReleaseManager().prepare(descriptor, environment, Arrays.asList(new MavenProject[]{getProject()}));

//		getLog().info("Creating branch " + getBranchName());
//		getGit().checkout().setCreateBranch(true).setName(getBranchName()).call();


		getLog().info("Updating pom version");
		File pom 							= new File("pom.xml");
		ModifiedPomXMLEventReader newPom 	= newModifiedPomXER(PomHelper.readXmlFile(pom));
		Model newPomModel 					= newPom.parse();
		VersionChange versionChange 		= new VersionChange(
			getProject().getGroupId(),
			getProject().getArtifactId(),
			newPomModel.getVersion(),
			newPomModel.getVersion() + SUFFIX
		);

		ProjectVersionChanger projectVersionChanger = new ProjectVersionChanger(getProject().getModel(), newPom, getLog());
		projectVersionChanger.apply(versionChange);

		writeFile(pom, projectVersionChanger.getPom().asStringBuilder());

		getLog().info("Commiting changed files");
		commit("[GitFlow::start-release] Create release branch " + getBranchName());

        push("Pushing commit");

        getLog().info("DONE");
	}


	@Override
	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rollbacking all changes");
			getGit().reset().setMode(ResetType.HARD).setRef(MASTER).call();
			getGit().checkout().setCreateBranch(false).setForce(true).setName(MASTER).call();
			getGit().branchDelete().setForce(true).setBranchNames(getBranchName()).call();
		} catch (Exception e1) {;}
		throw buildMojoException("ERROR", e);
	}


	public String getBranchName() {
		return branchName;
	}


	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

}
