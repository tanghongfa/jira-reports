package com.atlassian.plugins.proteus.jira.issue.view.util;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class DeploymentActivityRecord {
	
	/**
	 * Operations that are supported by Proteus Auto Deployment
	 */
	public static final String ACTION_TYPE_START_DEPLOY = "start_deploy";
	public static final String ACTION_TYPE_START_ROLLBACK = "start_rollback";
	
	private String action;
	private String environment;
	private Map<String,String> componentVersionMap;
	private Timestamp actionTime; 
	
	
	public DeploymentActivityRecord(String recordString, Timestamp actionTime) {
		String [] items = recordString.split(",");
		action = items[0];
		environment = items[1];
		componentVersionMap = new HashMap<String,String>();
		
		for(int i = 2; i < items.length; i ++) {
			String[] compVers = items[i].split(":");
			componentVersionMap.put(compVers[0], compVers[1]);
		}
		
		this.actionTime = actionTime;		
	}
	
	public String getAction() {
		return action;
	}
	
	public void setAction(String action) {
		this.action = action;
	}
	
	public String getEnvironment() {
		return environment;
	}
	
	public void setEnvironment(String environment) {
		this.environment = environment;
	}
	
	public Map<String, String> getComponentVersionMap() {
		return componentVersionMap;
	}
	
	public void setComponentVersionMap(Map<String, String> componentVersionMap) {
		this.componentVersionMap = componentVersionMap;
	}
		
	public Timestamp getActionTime() {
		return actionTime;
	}

	public void setActionTime(Timestamp actionTime) {
		this.actionTime = actionTime;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(" Action: ").append(this.action)
		       .append(" Environement: ").append(this.environment)
		       .append(" Deployment Comp: ").append(this.getComponentVersionMap())
		       .append(" Time: ").append(this.getActionTime().toString());
		return builder.toString();
	}
	
	public int compareTo(DeploymentActivityRecord anotherRcd) {		
		return anotherRcd.getActionTime().compareTo(this.getActionTime()); 
	}

}
