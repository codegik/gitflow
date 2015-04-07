package com.codegik.gitflow.merge;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.RecursiveMerger;


public class GitFlowResolveMerger extends RecursiveMerger {

	public GitFlowResolveMerger(Repository local) {
		super(local);
	}

}
