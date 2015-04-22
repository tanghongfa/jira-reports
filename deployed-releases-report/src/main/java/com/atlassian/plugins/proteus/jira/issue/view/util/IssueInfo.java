package com.atlassian.plugins.proteus.jira.issue.view.util;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
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
    private String issueDeployEnv;
    private Timestamp issueCreated;

    /**
     * Component and Version is matched by order
     */
    private List<String> deployedComponent;
    private List<String> deployedVersion;

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
            java.util.Iterator<Entry<String, String>> it = lastDep.getComponentVersionMap().entrySet().iterator();
            List<String> compLst = new ArrayList<String>();
            List<String> versionLst = new ArrayList<String>();
            while (it.hasNext()) {
                Entry<String, String> oneComp = it.next();
                compLst.add(oneComp.getKey());
                versionLst.add(oneComp.getValue());
            }
            this.deployedComponent = compLst;
            this.deployedVersion = versionLst;
        }

        return this;
    }

    /**
     * @return String
     */
    public List<String> getDeployedComponent() {
        return deployedComponent;
    }

    /**
     * @return String
     */
    public List<String> getDeployedVersion() {
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

    /**
     * @return String
     */
    public String getIssueDeployEnv() {
        return issueDeployEnv;
    }

    /**
     * @param issueDeployEnv
     * @return IssueInfo
     */
    public IssueInfo setIssueDeployEnv(String issueDeployEnv) {
        this.issueDeployEnv = issueDeployEnv;
        return this;
    }

    /**
     * @return Timestamp
     */
    public Timestamp getIssueCreated() {
        return issueCreated;
    }

    /**
     * @param issueCreated
     * @return IssueInfo
     */
    public IssueInfo setIssueCreated(Timestamp issueCreated) {
        this.issueCreated = issueCreated;
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

    public Map<String, DeploymentActivityRecord> getLastDeploymentToEnvironments(List<String> environmentLst) {
        Map<String, DeploymentActivityRecord> result = new HashMap<String, DeploymentActivityRecord>();
        for (String env : environmentLst) {
            DeploymentActivityRecord rcd = getLastDeploymentToEnv(env);
            if (rcd != null) {
                result.put(env, rcd);
            }
        }
        return result;
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

    public Map<String, DeploymentActivityRecord> getLastSuccessDeploymentToEnvironments(List<String> environmentLst) {
        Map<String, DeploymentActivityRecord> result = new HashMap<String, DeploymentActivityRecord>();
        for (String env : environmentLst) {
            DeploymentActivityRecord rcd = getLastSuccessDeploymentToEnv(env);
            if (rcd != null) {
                result.put(env, rcd);
            }
        }
        return result;
    }

    public int countSuccessDeployments() {
        if (activityRcd == null) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < activityRcd.size(); i++) {
            DeploymentActivityRecord activity = activityRcd.get(i);
            if (activity.getAction().equalsIgnoreCase(DeploymentActivityRecord.ACTION_TYPE_SUCCESS_DEPLOYED)) {
                count++;
            }
        }
        return count;
    }

    public int countFailedDeployments() {
        if (activityRcd == null) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < activityRcd.size(); i++) {
            DeploymentActivityRecord activity = activityRcd.get(i);
            if (activity.getAction().equalsIgnoreCase(DeploymentActivityRecord.ACTION_TYPE_FAIL_DEPLOYED)) {
                count++;
            }
        }
        return count;
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

        if (!StringUtils.isEmpty(this.getIssueDeployEnv()) && environment.equalsIgnoreCase(this.getIssueDeployEnv())) {
            return this.getIssueStatus();
        }
        for (int i = 0; i < this.environmentChangeRcd.size(); i++) {
            SortableChangeHistoryItem item = this.environmentChangeRcd.get(i);
            if (environment.equalsIgnoreCase(item.getFromString())) {
                SortableChangeHistoryItem lastStatusChange = getItemBefore(this.statusChangeRcd, item.getCreated());
                if (lastStatusChange != null) {
                    //There are status change before this environment valuable change. Get the last "TO"
                    return lastStatusChange.getToString();
                } else if (this.statusChangeRcd.size() > 0) {
                    //There is no status change before this environment change, Get the the earliest  
                    return this.statusChangeRcd.get(this.statusChangeRcd.size() - 1).getFromString();
                } else {
                    //This is no status change at all, then just Get the current status.
                    return this.getIssueStatus();
                }
            }
        }

        return null;
    }

    /**
     * @return boolean
     */
    public boolean isEverDeployed() {
        return (activityRcd != null) && (activityRcd.size() > 0);
    }

    public boolean isEverChangedStatus() {
        return (statusChangeRcd != null) && (statusChangeRcd.size() > 0);
    }

    /**
     * @param trans
     * @return List<Long>
     */
    public List<Integer> getTimeSpentOnTransition(WorkflowTransitions trans) {
        List<Integer> periods = new ArrayList<Integer>();
        for (int i = 0, length = this.statusChangeRcd.size(); i < length; i++) {
            if (trans.equals(this.statusChangeRcd.get(i))) {
                if (i < (length - 1)) {
                    periods.add((int) (this.statusChangeRcd.get(i).getCreated().getTime() - this.statusChangeRcd
                            .get(i + 1).getCreated().getTime()));
                } else {
                    periods.add((int) (this.statusChangeRcd.get(i).getCreated().getTime() - this.issueCreated.getTime()));
                }
            }
        }
        return periods;
    }
}
