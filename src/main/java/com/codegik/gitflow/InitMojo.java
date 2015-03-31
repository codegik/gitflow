package com.codegik.gitflow;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;


/**
 * Verificar se está no developer e criar o branch da release
 * @author igklassmann
 *
 */
@Mojo(name = "start-release")
public class InitMojo extends AbstractMojo {

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info( "Hello, world." );
	}

}
