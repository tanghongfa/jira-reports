package com.atlassian.plugins.proteus.jira.issue.view.util;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * The purpose of this class is to record issue info
 */
public class IssueInfo implements Comparable<IssueInfo> {

    private static final Logger log = Logger.getLogger(IssueInfo.class);

    private Long issueNo;
    private String issueKey;
    private String issueTitle;
    private String issueStatus;

    /**
     * Right now, we have assumed for each of the JIRA issue, we will
     * only have one component & one version. It can be easily
     * extended to support multi-components and version later on if
     * needed.
     */
    private String deployedComponent;
    private String deployedVersion;

    private List<DeploymentActivityRecord> activityRcd;
    private List<SortableChangeHistoryItem> statusChangeRcd;
    private List<SortableChangeHistoryItem> environmentChangeRcd;

    /**
     * @return issueNo
     */
    public Long getIssueNo() {
        return issueNo;
    }

    /**
     * @param issueNo
     * @return IssueInfo
     */
    public IssueInfo setIssueNo(Long issueNo) {
        this.issueNo = issueNo;
        return this;
    }

    /**
     * @return String
     */
    public String getIssueTitle() {
        return issueTitle;
    }

    /**
     * @param issueTitle
     * @return IssueInfo
     */
    public IssueInfo setIssueTitle(String issueTitle) {
        this.issueTitle = issueTitle;
        return this;
    }

    /**
     * @return String
     */
    public String getIssueKey() {
        return issueKey;
    }

    /**
     * @param issueKey
     * @return IssueInfo
     */
    public IssueInfo setIssueKey(String issueKey) {
        this.issueKey = issueKey;
        return this;
    }

    /**
     * @return List<DeploymentActivityRecord>
     */
    public List<DeploymentActivityRecord> getActivityRcd() {
        return activityRcd;
    }

    /**
     * @param activityRcd
     * @return IssueInfo
     */
    public IssueInfo setActivityRcd(List<DeploymentActivityRecord> activityRcd) {
        this.activityRcd = activityRcd;
        Collections.sort(this.activityRcd);

        DeploymentActivityRecord lastDep = this.getLastDeploymentToEnv(null);
        if (lastDep != null) {
            Entry<String, String> oneComp = lastDep.getComponentVersionMap().entrySet().iterator().next();
            this.deployedComponent = oneComp.getKey();
            this.deployedVersion = oneComp.getValue();
        }

        return this;
    }

    /**
     * @return String
     */
    public String getDeployedComponent() {
        return deployedComponent;
    }

    /**
     * @return String
     */
    public String getDeployedVersion() {
        return deployedVersion;
    }

    /**
     * @return List<SortableChangeHistoryItem>
     */
    public List<SortableChangeHistoryItem> getStatusChangeRcd() {
        return statusChangeRcd;
    }

    /**
     * @param statusChangeRcd
     * @return IssueInfo
     */
    public IssueInfo setStatusChangeRcd(List<SortableChangeHistoryItem> statusChangeRcd) {
        this.statusChangeRcd = statusChangeRcd;
        Collections.sort(this.statusChangeRcd);
        return this;
    }

    /**
     * @return List<SortableChangeHistoryItem>
     */
    public List<SortableChangeHistoryItem> getEnvironmentChangeRcd() {
        return environmentChangeRcd;
    }

    /**
     * @param environmentChangeRcd
     * @return IssueInfo
     */
    public IssueInfo setEnvironmentChangeRcd(List<SortableChangeHistoryItem> environmentChangeRcd) {
        this.environmentChangeRcd = environmentChangeRcd;
        Collections.sort(this.environmentChangeRcd);
        return this;
    }

    /**
     * @return String
     */
    public String getIssueStatus() {
        return issueStatus;
    }

    /**
     * @param issueStatus
     * @return IssueInfo
     */
    public IssueInfo setIssueStatus(String issueStatus) {
        this.issueStatus = issueStatus;
        return this;
    }

    @Override
    public String toString() {
        return this.issueTitle;
    }

    @Override
    public int compareTo(IssueInfo anotherIssue) {
        return (int) (anotherIssue.getIssueNo() - this.getIssueNo());
    }

    /**
     * Return the last deployment activity for an specified
     * environment.
     * 
     * @param environment The environment name. When null, it means
     *            any environment.
     * @return DeploymentActivityRecord
     */
    public DeploymentActivityRecord getLastDeploymentToEnv(String environment) {
        if (activityRcd == null) {
            return null;
        }

        //If environment is not specified, then just return the last deployment to any env.
        if (environment == null) {
            for (int i = 0; i < activityRcd.size(); i++) {
                DeploymentActivityRecord activity = activityRcd.get(i);
                if (activity.getAction().equalsIgnoreCase(DeploymentActivityRecord.ACTION_TYPE_START_DEPLOY)) {
                    return activity;
                }
            }
        }

        //This is the normal use-case, which is return the last deployment activity for specified environment
        for (int i = 0; i < activityRcd.size(); i++) {
            DeploymentActivityRecord activity = activityRcd.get(i);
            if (activity.getEnvironment().equalsIgnoreCase(environment)
                    && activity.getAction().equalsIgnoreCase(DeploymentActivityRecord.ACTION_TYPE_START_DEPLOY)) {
                return activity;
            }
        }

        return null;
    }

    /**
     * This function is to find the last successful deployment to an
     * env. It has been taking consideration of the following
     * scenarios: 1) people did multi deployments to the same
     * environment, those deployment could be success or failure
     * whatever, the result here is capturing the "End/Final" state of
     * the deployment to that environment 2) people made mistake when
     * they choose the "Deployment Result". They changed the result
     * between "Success" and "Failure" back and forth. Here the result
     * only cares about the last(final) status. It doesn't care about
     * any status before the last one.
     * 
     * @param environment environment value can't be null
     * @return DeploymentActivityRecord
     */
    public DeploymentActivityRecord getLastSuccessDeploymentToEnv(String environment) {
        log.error(activityRcd);
        boolean gotLastSuccessDeployToEnvRcd = false;
        for (int i = 0; i < activityRcd.size(); i++) {
            DeploymentActivityRecord activity = activityRcd.get(i);
            if (activity.getEnvironment().equalsIgnoreCase(environment)) {

                //If it has been confirmed that the final deployment status to this environment is successful, then try to get the last deployment time/activity
                if (gotLastSuccessDeployToEnvRcd
                        && activity.getAction().equalsIgnoreCase(DeploymentActivityRecord.ACTION_TYPE_START_DEPLOY)) {
                    return activity;
                }

                //The last status shows deployment to this environment is successful, just continue the loop to find when the deployment kicked off
                if (activity.getAction().equalsIgnoreCase(DeploymentActivityRecord.ACTION_TYPE_SUCCESS_DEPLOYED)) {
                    gotLastSuccessDeployToEnvRcd = true;
                }

                //The last status shows deployment to this environment is failed
                if (activity.getAction().equalsIgnoreCase(DeploymentActivityRecord.ACTION_TYPE_FAIL_DEPLOYED)) {
                    return null;
                }
            }
        }

        return null;
    }

    private SortableChangeHistoryItem getItemBefore(List<SortableChangeHistoryItem> itemLst, Timestamp timestamp) {
        for (int i = 0; i < itemLst.size(); i++) {
            if (itemLst.get(i).getCreated().before(timestamp)) {
                return itemLst.get(i);
            }
        }
        return null;
    }

    /**
     * @param environment
     * @return String
     */
    public String getLastStatusValueToEnv(String environment) {
        //No environment has set... pre-deploy to any environment

        for (int i = 0; i < this.environmentChangeRcd.size(); i++) {
            SortableChangeHistoryItem item = this.environmentChangeRcd.get(i);
            if ((environment.isEmpty() && item.getFromString().isEmpty())
                    || (environment.equalsIgnoreCase(item.getFromString()))) {
                SortableChangeHistoryItem lastStatusChange = getItemBefore(this.statusChangeRcd, item.getCreated());
                if (lastStatusChange != null) {
                    //There are status change before this environment valuable change. Get the last "TO"
                    return lastStatusChange.getToString();
                } else if (this.statusChangeRcd.size() > 0) {
                    //There is no status change before this environment change, Get the the earliest  
                    return this.statusChangeRcd.get(this.statusChangeRcd.size() - 1).getFromString();
                } else {
                    //This is no status change at all, then just Get the current status.
                    return this.issueStatus;
                }
            }
        }
        return null;
    }

    /**
     * @return Set<String>
     */
    public Set<String> getDeployedEnvironments() {
        Set<String> envHash = new HashSet<String>();
        for (int i = 0; i < activityRcd.size(); i++) {
            envHash.add(activityRcd.get(i).getEnvironment());
        }
        return envHash;
    }

    /**
     * @return boolean
     */
    public boolean isEverDeployed() {
        return (activityRcd != null) && (activityRcd.size() > 0);
    }
}
