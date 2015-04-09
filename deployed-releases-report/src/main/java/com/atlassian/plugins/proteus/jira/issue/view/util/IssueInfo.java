package com.atlassian.plugins.proteus.jira.issue.view.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class IssueInfo implements Comparable<IssueInfo> {
	
	private Long issueNo;
	private String issueKey;
	private String issueTitle;
	
	/**
	 * Right now, we have assumed for each of the JIRA issue, we will only have one component & one version.
	 * It can be easily extended to support multi-components and version later on if needed.
	 */
	private String deployedComponent;
	private String deployedVersion;
	
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
		
		DeploymentActivityRecord lastDep = this.getLastDeploymentToEnv(null);
		Entry<String, String> oneComp = lastDep.getComponentVersionMap().entrySet().iterator().next();
		this.deployedComponent = oneComp.getKey();
		this.deployedVersion = oneComp.getValue();
		
		return this;
	}

	public String getDeployedComponent() {
		return deployedComponent;
	}

	public String getDeployedVersion() {
		return deployedVersion;
	}

	public String toString() {
		return this.issueTitle;
	}

	@Override
	public int compareTo(IssueInfo anotherIssue) {
		return (int)(anotherIssue.getIssueNo() - this.getIssueNo());
	}
	
	/**
	 * Return the last deployment activity for an specified environment.
	 * @param environment The environment name. When null, it means any environment.
	 * @return
	 */
	public DeploymentActivityRecord getLastDeploymentToEnv(String environment) {
		if(activityRcd == null) {
			return null;
		}
		
		//If environment is not specified, then just return the last deployment to any env.
		if(environment == null) {
			for(int i = 0; i < activityRcd.size(); i ++) {
				DeploymentActivityRecord activity = activityRcd.get(i);
				if(activity.getAction().equalsIgnoreCase(DeploymentActivityRecord.ACTION_TYPE_START_DEPLOY)) {
					return activity;
				}
			}
		}
		
		//This is the normal use-case, which is return the last deployment activity for specified environment
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
