package com.codegik.gitflow;

import java.text.MessageFormat;

import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.lib.Ref;


public class MergeGitFlow {
	private String branchName;
	private Ref targetRef;
	private String[] ignoringFiles = {AbstractGitFlowMojo.FILE_POM};
	private Stage ignoringFilesStage = Stage.OURS;
	private String errorMessage;


	public String getBranchName() {
		return branchName;
	}
	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}
	public Ref getTargetRef() {
		return targetRef;
	}
	public void setTargetRef(Ref targetRef) {
		this.targetRef = targetRef;
	}
	public String[] getIgnoringFiles() {
		return ignoringFiles;
	}
	public void setIgnoringFiles(String... ignoringFiles) {
		this.ignoringFiles = ignoringFiles;
	}
	public Stage getIgnoringFilesStage() {
		return ignoringFilesStage;
	}
	public void setIgnoringFilesStage(Stage ignoringFilesStage) {
		this.ignoringFilesStage = ignoringFilesStage;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public String toString() {
		return MessageFormat.format("branchName: {0}, targetRef: {1}", branchName, targetRef.getName());
	}

}
