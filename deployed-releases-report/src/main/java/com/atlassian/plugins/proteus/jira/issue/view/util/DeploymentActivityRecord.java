package com.atlassian.plugins.proteus.jira.issue.view.util;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The purpose of this class is ... TODO javadoc for class
 * DeploymentActivityRecord
 */
public class DeploymentActivityRecord implements Comparable<DeploymentActivityRecord> {

    /**
     * Operations that are supported by Proteus Auto Deployment
     */
    public static final String ACTION_TYPE_START_DEPLOY = "start_deploy";
    /**
     * TODO javadoc for <code>ACTION_TYPE_FINISHED_DEPLOY</code>
     */
    public static final String ACTION_TYPE_FINISHED_DEPLOY = "finished_deploy";
    /**
     * TODO javadoc for <code>ACTION_TYPE_START_ROLLBACK</code>
     */
    public static final String ACTION_TYPE_START_ROLLBACK = "start_rollback";
    /**
     * TODO javadoc for <code>ACTION_TYPE_SUCCESS_DEPLOYED</code>
     */
    public static final String ACTION_TYPE_SUCCESS_DEPLOYED = "success_deploy";
    /**
     * TODO javadoc for <code>ACTION_TYPE_FAIL_DEPLOYED</code>
     */
    public static final String ACTION_TYPE_FAIL_DEPLOYED = "fail_deploy";

    private String action;
    private String environment;
    private Map<String, String> componentVersionMap;
    private Timestamp actionTime;

    /**
     * Creates a new instance of <code>DeploymentActivityRecord</code>
     * . TODO javadoc for DeploymentActivityRecord constructor.
     * 
     * @param recordString
     * @param actionTime
     */
    public DeploymentActivityRecord(String recordString, Timestamp actionTime) {
        String[] items = recordString.split(",");
        action = items[0];
        environment = items[1];
        componentVersionMap = new HashMap<String, String>();

        for (int i = 2; i < items.length; i++) {
            String[] compVers = items[i].split(":");
            componentVersionMap.put(compVers[0], compVers[1]);
        }

        this.actionTime = actionTime;
    }

    /**
     * @return String
     */
    public String getAction() {
        return action;
    }

    /**
     * @param action
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return String
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * @param environment
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * @return Map<String, String>
     */
    public Map<String, String> getComponentVersionMap() {
        return componentVersionMap;
    }

    /**
     * @param componentVersionMap
     */
    public void setComponentVersionMap(Map<String, String> componentVersionMap) {
        this.componentVersionMap = componentVersionMap;
    }

    /**
     * @return Timestamp
     */
    public Timestamp getActionTime() {
        return actionTime;
    }

    /**
     * @return Date
     */
    public Date getActionDate() {
        return new Date(actionTime.getTime());
    }

    /**
     * @param actionTime
     */
    public void setActionTime(Timestamp actionTime) {
        this.actionTime = actionTime;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(" Action: ").append(this.action).append(" Environement: ").append(this.environment)
                .append(" Deployment Comp: ").append(this.getComponentVersionMap()).append(" Time: ")
                .append(this.getActionTime().toString());
        return builder.toString();
    }

    /**
     * Will always sorted in reverse order (latest item is at the
     * beginning of the sorted result)
     */
    @Override
    public int compareTo(DeploymentActivityRecord anotherRcd) {
        return anotherRcd.getActionTime().compareTo(this.getActionTime());
    }

}
