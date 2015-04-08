package com.atlassian.plugins.proteus.jira.issue.view.util;

public class IssueInfo {
	
	private String issueNo;
	private String issueTitle;
	
	public String getIssueNo() {
		return issueNo;
	}
	
	public IssueInfo setIssueNo(String issueNo) {
		this.issueNo = issueNo;
		return this;
	}
	public String getIssueTitle() {
		return issueTitle;
	}
	
	public IssueInfo setIssueTitle(String issueTitle) {
		this.issueTitle = issueTitle;
		return this;
	}
	
	public String toString() {
		return this.issueTitle;
	}
}
