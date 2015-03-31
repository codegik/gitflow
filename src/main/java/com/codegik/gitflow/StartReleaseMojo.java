package com.codegik.gitflow;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;


/**
 * Verificar se está no master e criar o branch develop
 * @author igklassmann
 *
 */
@Mojo(name = "init")
public class StartReleaseMojo extends AbstractMojo {

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info( "Hello, world." );
	}

}
