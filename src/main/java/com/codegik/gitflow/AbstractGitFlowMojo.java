package com.codegik.gitflow;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.stax2.XMLInputFactory2;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;


public abstract class AbstractGitFlowMojo extends AbstractMojo {

	public enum BranchType { feature, bugfix }

	protected static final String MASTER = "master";
	protected static final String DEVELOP = "develop";
	protected static final String PREFIX_RELEASE = "release";
	protected static final String PREFIX_HOTFIX = "hotfix";
	protected static final String SUFFIX = "-SNAPSHOT";
	protected static final String SUFFIX_RELEASE = ".00-SNAPSHOT";
	protected static final String SEPARATOR = "/";
	private Git git;

    @Component
    private MavenProject project;

    @Component
    private ReleaseManager releaseManager;

    @Parameter( property = "password" )
    private String password;

    @Parameter( property = "username" )
    private String username;


    public abstract void run() throws Exception;

    public abstract void rollback(Exception e) throws MojoExecutionException;


    public void execute() throws MojoExecutionException, MojoFailureException {
    	try {
    		run();
    	} catch (Exception e) {
    		rollback(e);
    	}
    }


	protected Git getGit() throws Exception {
		if (git == null) {
			git = Git.open(new File("."));
		}

		return git;
	}


	protected Ref findBranch(String branch) throws Exception {
		for (Ref b : getGit().branchList().setListMode(ListMode.ALL).call()) {
			if (branch.equals(b.getName().toLowerCase().replace("refs/heads/", "")) ||
				branch.equals(b.getName().toLowerCase().replace("refs/remotes/origin/", ""))) {
				return b;
			}
		}

		return null;
	}


	protected Ref findTag(String tag) throws Exception {
		for (Ref b : getGit().tagList().call()) {
			if (tag.equals(b.getName().toLowerCase().replace("tags/", ""))) {
				return b;
			}
		}

		return null;
	}


	protected void validadeVersion(String version) throws MojoExecutionException {
		String pattern = "[0-9]{1,}\\.[0-9]{1,}";

		if (!version.matches(pattern)) {
			throw buildMojoException("The version pattern is " + pattern + "  EX: 1.3");
		}
	}


	protected final ModifiedPomXMLEventReader newModifiedPomXER(StringBuilder input) {
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


	protected void writeFile(File outFile, StringBuilder input) throws IOException {
		Writer writer = WriterFactory.newXmlWriter(outFile);
		try {
			IOUtil.copy(input.toString(), writer);
		} finally {
			IOUtil.close(writer);
		}
	}


	protected MojoExecutionException buildMojoException(String errorMessage) {
		getLog().error(errorMessage);
		return new MojoExecutionException(errorMessage);
	}


	protected MojoExecutionException buildMojoException(String errorMessage, Exception e) {
		getLog().error(errorMessage);
		return new MojoExecutionException(errorMessage, e);
	}


	protected RevCommit commit(String message) throws Exception {
		return getGit().commit().setAll(true).setMessage(message).call();
	}


	protected Iterable<PushResult> push() throws Exception {
		return push(null);
	}


	protected Iterable<PushResult> push(String logMessage) throws Exception {
		getLog().info(logMessage == null ? "Pushing commit" : logMessage);

		if (getUsername() != null && getPassword() != null) {
	        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(getUsername(), getPassword());
	        return getGit().push().setCredentialsProvider(credentialsProvider).call();
		}

		return getGit().push().call();
	}


	protected ReleaseEnvironment buildDefaultReleaseEnvironment() throws Exception {
		ReleaseEnvironment environment = new DefaultReleaseEnvironment();

		environment.setLocalRepositoryDirectory(getGit().getRepository().getDirectory());

		return environment;
	}


	protected ReleaseDescriptor buildReleaseDescriptor() throws Exception {
		ReleaseDescriptor descriptor = new ReleaseDescriptor();

		descriptor.mapDevelopmentVersion(getProject().getArtifactId(), getProject().getVersion());
		descriptor.setDefaultDevelopmentVersion(getProject().getVersion());
		descriptor.setAutoVersionSubmodules(true);
		descriptor.setInteractive(false);
		descriptor.setUpdateWorkingCopyVersions(true);
		descriptor.setWorkingDirectory(getGit().getRepository().getDirectory().getParent());
		descriptor.setScmUsername(getUsername());
		descriptor.setScmPassword(getPassword());
		descriptor.setUpdateDependencies(true);

		return descriptor;
	}


	public MavenProject getProject() {
		return project;
	}


	public void setProject(MavenProject project) {
		this.project = project;
	}


	public ReleaseManager getReleaseManager() {
		return releaseManager;
	}


	public void setReleaseManager(ReleaseManager releaseManager) {
		this.releaseManager = releaseManager;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
