package com.codegik.gitflow.core.impl;

import com.codegik.gitflow.core.GitFlowMojo;

public abstract class DefaultGitFlowMojo extends GitFlowMojo {
	private DefaultGitFlow defaultGitFlow;


	@Override
	public DefaultGitFlow getGitFlow() {
		if (defaultGitFlow == null) {
			defaultGitFlow = new DefaultGitFlow(new DefaultGitFlowPattern(), getLog());
		}

		return defaultGitFlow;
	}

}
