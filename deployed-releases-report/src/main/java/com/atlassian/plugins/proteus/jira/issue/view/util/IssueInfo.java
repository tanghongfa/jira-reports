package com.atlassian.plugins.proteus.jira.issue.view.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IssueInfo implements Comparable<IssueInfo> {
	
	private Long issueNo;
	private String issueKey;
	private String issueTitle;
	
	private List<DeploymentActivityRecord> activityRcd; 
	
	public Long getIssueNo() {
		return issueNo;
	}
	
	public IssueInfo setIssueNo(Long issueNo) {
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
	
	public String getIssueKey() {
		return issueKey;
	}

	public IssueInfo setIssueKey(String issueKey) {
		this.issueKey = issueKey;
		return this;
	}
	
	public List<DeploymentActivityRecord> getActivityRcd() {
		return activityRcd;
	}

	public IssueInfo setActivityRcd(List<DeploymentActivityRecord> activityRcd) {
		this.activityRcd = activityRcd;
		Collections.sort(this.activityRcd);
		return this;
	}

	public String toString() {
		return this.issueTitle;
	}

	@Override
	public int compareTo(IssueInfo anotherIssue) {
		return (int)(anotherIssue.getIssueNo() - this.getIssueNo());
	}
	
	public DeploymentActivityRecord getLastDeploymentToEnv(String environment) {
		if(activityRcd == null) {
			return null;
		}
		
		for(int i = 0; i < activityRcd.size(); i ++) {
			DeploymentActivityRecord activity = activityRcd.get(i);
			if(activity.getEnvironment().equalsIgnoreCase(environment) && activity.getAction().equalsIgnoreCase(DeploymentActivityRecord.ACTION_TYPE_START_DEPLOY) ) {
				return activity;
			}
		}
		
		return null;
	}
	
	public Set<String> getDeployedEnvironments() {
		Set<String> envHash = new HashSet<String>();
		for(int i = 0; i < activityRcd.size(); i ++) {
			envHash.add(activityRcd.get(i).getEnvironment());
		}
		return envHash;
	}
	
}
