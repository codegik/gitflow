package com.codegik.gitflow.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.lib.Ref;


public class MergeGitFlow {
	private String branchName;
	private Ref targetRef;
	private List<String> ignoringFiles = new ArrayList<String>();
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
	public List<String> getIgnoringFiles() {
		return ignoringFiles;
	}
	public void addIgnoringFiles(String ignoringFile) {
		this.ignoringFiles.add(ignoringFile);
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
