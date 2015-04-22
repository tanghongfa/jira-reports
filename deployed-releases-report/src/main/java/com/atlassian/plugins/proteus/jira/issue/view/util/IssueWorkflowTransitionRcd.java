/*------------------------------------------------------------------------------
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *----------------------------------------------------------------------------*/
package com.atlassian.plugins.proteus.jira.issue.view.util;

import java.util.Comparator;
import java.util.List;

public class IssueWorkflowTransitionRcd extends WorkflowTransitions {

    private Integer averageTime;
    private List<Integer> actualData;
    private IssueInfo issue;

    public IssueWorkflowTransitionRcd(WorkflowTransitions transition, IssueInfo issue) {
        super(transition.getFromStatus(), transition.getToStatus(), transition.getStepRating());
        this.issue = issue;
        this.averageTime = null;
    }

    public IssueWorkflowTransitionRcd(WorkflowTransitions transition, IssueInfo issue, List<Integer> actualData) {
        this(transition, issue);
        this.setActualData(actualData);
    }

    public Integer getAverageTime() {
        if (this.averageTime != null) {
            return this.averageTime;
        }
        if ((actualData != null) && (actualData.size() > 0)) {
            this.averageTime = Math.round(JiraReportUtils.sum(actualData) / actualData.size());
        }
        return averageTime;
    }

    public List<Integer> getActualData() {
        return actualData;
    }

    public void setActualData(List<Integer> actualData) {
        this.averageTime = null;
        this.actualData = actualData;
    }

    public void appendActualData(List<Integer> actualData) {
        this.averageTime = null;
        this.actualData.addAll(actualData);
    }

    public IssueInfo getIssue() {
        return issue;
    }

    public void setIssue(IssueInfo issue) {
        this.issue = issue;
    }

    public String getFormattedDuration() {
        Integer data = this.getAverageTime();
        if (data != null) {
            return JiraReportUtils.getDurationBreakdown(data);
        } else {
            return "---";
        }
    }

    public static Comparator<IssueWorkflowTransitionRcd> TransitionAverageSpentTimeComparator = new Comparator<IssueWorkflowTransitionRcd>() {
        public int compare(IssueWorkflowTransitionRcd one, IssueWorkflowTransitionRcd two) {
            if ((two != null) && (one != null)) {
                return two.getAverageTime() - one.getAverageTime();
            }

            if (two == null) {
                return -1;
            }
            if (one == null) {
                return 1;
            }
            return 0;
        }

    };
}
