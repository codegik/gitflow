package com.codegik.gitflow;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.change.VersionChange;
import org.codehaus.mojo.versions.change.VersionChanger;
import org.codehaus.mojo.versions.change.VersionChangerFactory;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.stax2.XMLInputFactory2;


public abstract class AbstractGitFlowMojo extends AbstractMojo {

	public enum BranchType { feature, bugfix }

	protected static final String ORIGIN = "origin";
	protected static final String MASTER = "master";
	protected static final String DEVELOP = "develop";
	protected static final String SUFFIX = "-SNAPSHOT";
	protected static final String SUFFIX_RELEASE = ".0";
	public static final String PREFIX_HOTFIX = "hotfix";
	public static final String PREFIX_TAG = "refs/tags";
	public static final String PREFIX_RELEASE = "release";
	public static final Pattern TAG_VERSION_PATTERN = Pattern.compile("([0-9]{1,})\\.([0-9]{1,})\\.([0-9]{1,})");
	public static final Pattern POM_SNAPSHOT_VERSION_PATTERN = Pattern.compile("([0-9]{1,})\\.([0-9]{1,})\\.([0-9]{1,2})(-SNAPSHOT)");
	public static final Pattern RELEASE_VERSION_PATTERN = Pattern.compile("([0-9]{1,})\\.([0-9]{1,})");
	public static final String SEPARATOR = "/";
	public static final String FILE_POM = "pom.xml";

    @Component
    private MavenProject project;

    @Component
    private MavenExecutor mavenExecutor;

    @Parameter( property = "password" )
    private String password;

    @Parameter( property = "username" )
    private String username;

    @Parameter( property = "skipTests" )
    private Boolean skipTests;


    public abstract void run(GitFlow gitFlow) throws Exception;

    public abstract void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException;


    public void execute() throws MojoExecutionException, MojoFailureException {
    	GitFlow gitFlow = new GitFlow(getLog(), getUsername(), getPassword());

    	if (gitFlow.getCredentialsProvider() == null) {
    		throw buildMojoException("Please set your credentials: -Dusername=<username> -Dpassword=<password>");
    	}

    	try {
    		run(gitFlow);
    		getLog().info("DONE");
    	} catch (Exception e) {
    		rollback(gitFlow, e);
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


	@SuppressWarnings("unchecked")
	protected List<MavenProject> buildMavenProjects() throws IOException {
		List<MavenProject> projectList = new ArrayList<MavenProject>();
		MavenProject rootProject = getProject();

		projectList.add(rootProject);

		List<MavenProject> submodules = rootProject.getCollectedProjects();

		projectList.addAll(submodules);

		return projectList;
	}


	@SuppressWarnings("rawtypes")
	protected void updatePomVersion(String newVersion) throws Exception {
		getLog().info("Bumping version of files to " + newVersion);

		List<MavenProject> projects = buildMavenProjects();
		List<VersionChange> changes = new ArrayList<VersionChange>();

		for (MavenProject project : projects) {
			Model model 						= PomHelper.getRawModel(project.getFile());
			File pom 							= project.getFile();
			ModifiedPomXMLEventReader newPom 	= newModifiedPomXER(PomHelper.readXmlFile(pom));

            getLog().info("Processing " + PomHelper.getGroupId( model ) + ":" + PomHelper.getArtifactId( model ) );

            VersionChangerFactory versionChangerFactory = new VersionChangerFactory();
            versionChangerFactory.setPom(newPom);
            versionChangerFactory.setLog(getLog());
            versionChangerFactory.setModel(model);

            VersionChanger changer = versionChangerFactory.newVersionChanger(true, true, true, true);

			// Adiciona a alteracao principal do pom
			changes.add(new VersionChange(
				getProject().getGroupId(),
				getProject().getArtifactId(),
				PomHelper.getVersion(model),
				newVersion
			));

			// Adiciona a alteracao do parent do pom
			Map<String, Model> reactor = PomHelper.getReactorModels( project, getLog() );
			Iterator j = PomHelper.getChildModels(reactor, PomHelper.getGroupId(model), PomHelper.getArtifactId(model)).entrySet().iterator();
			while ( j.hasNext() ) {
				Map.Entry target = (Map.Entry) j.next();
				Model targetModel = (Model) target.getValue();
				changes.add(new VersionChange(
					PomHelper.getGroupId(targetModel),
					PomHelper.getArtifactId(targetModel),
                    PomHelper.getVersion(targetModel),
                    newVersion
				));
			}

			for (VersionChange change : changes) {
				if (change.getOldVersion() != null) {
					changer.apply(change);
				}
			}

			writeFile(pom, versionChangerFactory.getPom().asStringBuilder());
		}
	}


	public ReleaseResult compileProject(String goals, Boolean skipTests) throws Exception {
		getLog().info("Compiling project: " + goals + " -DskipTests=" + skipTests);
		ReleaseResult releaseResult = new ReleaseResult();
		ReleaseEnvironment releaseEnvironment = new DefaultReleaseEnvironment();

		getMavenExecutor().executeGoals(getProject().getBasedir(), goals, releaseEnvironment, false, (skipTests ? " -DskipTests" : null), releaseResult);

		return releaseResult;
	}


	public MavenProject getProject() {
		return project;
	}


	public void setProject(MavenProject project) {
		this.project = project;
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

	public Boolean getSkipTests() {
		return skipTests;
	}

	public void setSkipTests(Boolean skipTests) {
		this.skipTests = skipTests;
	}

	public MavenExecutor getMavenExecutor() {
		return mavenExecutor;
	}

	public void setMavenExecutor(MavenExecutor mavenExecutor) {
		this.mavenExecutor = mavenExecutor;
	}

}
