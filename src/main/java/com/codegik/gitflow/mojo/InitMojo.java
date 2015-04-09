package com.codegik.gitflow.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import com.codegik.gitflow.AbstractGitFlowMojo;
import com.codegik.gitflow.GitFlow;


/**
 * TODO
 * Criar o develop caso nao exista
 * Setar a versao do cpom para 1.0.0
 * Criar a tag 1.0.0
 *
 * @author Inacio G Klassmann
 */
@Mojo(name = "init", aggregator = true)
public class InitMojo extends AbstractGitFlowMojo {

	@Override
	public void run(GitFlow gitFlow) throws Exception {
	}

	@Override
	public void rollback(GitFlow gitFlow, Exception e) throws MojoExecutionException {
	}
}
