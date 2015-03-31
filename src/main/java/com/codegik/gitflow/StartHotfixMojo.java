package com.codegik.gitflow;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.change.ProjectVersionChanger;
import org.codehaus.mojo.versions.change.VersionChange;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.stax2.XMLInputFactory2;
import org.eclipse.jgit.api.ResetCommand.ResetType;


/**
 * Start new hotfix branch from master
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "start-hotfix", aggregator = true)
public class StartHotfixMojo extends AbstractGitFlowMojo {
	@Parameter( property = "branchName", required = true )
	private String branchName;


	public void run() throws Exception {
		setBranchName(PREFIX_HOTFIX + SEPARATOR + getBranchName());

		getLog().info("Reseting to branch " + MASTER);
		getGit().reset().setMode(ResetType.HARD).setRef(MASTER).call();
		getGit().checkout().setCreateBranch(false).setForce(true).setName(MASTER).call();

		getLog().info("Creating branch " + getBranchName());
		getGit().checkout().setCreateBranch(true).setName(getBranchName()).call();

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
		getGit().commit().setAll(true).setMessage("[GitFlow::start-release] Create release branch " + getBranchName()).call();

        getLog().info("Pushing commit");
        getGit().push().call();

        getLog().info("DONE");
	}


	public void rollback(Exception e) throws MojoExecutionException {
		try {
			getLog().error(e.getMessage());
			getLog().info("Rollbacking all changes");
			getGit().reset().setMode(ResetType.HARD).setRef(DEVELOP).call();
			getGit().checkout().setCreateBranch(false).setForce(true).setName(DEVELOP).call();
			getGit().branchDelete().setForce(true).setBranchNames(getBranchName()).call();
		} catch (Exception e1) {;}
		throw new MojoExecutionException("ERROR", e);
	}


	private final ModifiedPomXMLEventReader newModifiedPomXER(StringBuilder input) {
		ModifiedPomXMLEventReader newPom = null;
		try {
			XMLInputFactory inputFactory = XMLInputFactory2.newInstance();
			inputFactory.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
			newPom = new ModifiedPomXMLEventReader(input, inputFactory);
		} catch (XMLStreamException e) {
			getLog().error(e);
		}
		return newPom;
	}


	private void writeFile(File outFile, StringBuilder input) throws IOException {
		Writer writer = WriterFactory.newXmlWriter(outFile);
		try {
			IOUtil.copy(input.toString(), writer);
		} finally {
			IOUtil.close(writer);
		}
	}

	public String getBranchName() {
		return branchName;
	}


	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

}
