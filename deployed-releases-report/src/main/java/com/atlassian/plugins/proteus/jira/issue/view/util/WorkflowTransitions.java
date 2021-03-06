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

import com.atlassian.jira.issue.history.ChangeItemBean;

/**
 * The purpose of this class is for workflow transition
 */
public class WorkflowTransitions implements Comparable<WorkflowTransitions> {

    private String fromStatus;
    private String toStatus;
    private int stepRating; //Decide the rough position of this transition in workflow

    /**
     * Creates a new instance of <code>WorkflowTransitions</code>.
     * 
     * @param item
     * @param stepRating
     */
    public WorkflowTransitions(ChangeItemBean item, int stepRating) {
        this.fromStatus = item.getFromString();
        this.toStatus = item.getToString();
        this.stepRating = stepRating;
    }

    /**
     * Creates a new instance of <code>WorkflowTransitions</code>.
     * 
     * @param fromStatus
     * @param toStatus
     * @param stepRating
     */
    public WorkflowTransitions(String fromStatus, String toStatus, int stepRating) {
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.stepRating = stepRating;
    }

    /**
     * @return String
     */
    public String getFromStatus() {
        return fromStatus;
    }

    /**
     * @param fromStatus
     * @return WorkflowTransitions
     */
    public WorkflowTransitions setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
        return this;
    }

    /**
     * @return String
     */
    public String getToStatus() {
        return toStatus;
    }

    /**
     * @param toStatus
     * @return WorkflowTransitions
     */
    public WorkflowTransitions setToStatus(String toStatus) {
        this.toStatus = toStatus;
        return this;
    }

    /**
     * @return int
     */
    public int getStepRating() {
        return stepRating;
    }

    /**
     * @param stepRating
     * @return WorkflowTransitions
     */
    public WorkflowTransitions setStepRating(int stepRating) {
        this.stepRating = stepRating;
        return this;
    }

    @Override
    public int compareTo(WorkflowTransitions arg0) {
        return arg0.stepRating - this.stepRating;
    }

    @Override
    public boolean equals(Object object2) {
        if ((object2 != null) && (object2 instanceof ChangeItemBean)) {
            return this.fromStatus.equalsIgnoreCase(((ChangeItemBean) object2).getFromString())
                    && this.toStatus.equalsIgnoreCase(((ChangeItemBean) object2).getToString());
        }
        return false;
    }

    /**
     * @return String
     */
    public String getUniqueId() {
        return this.fromStatus.toLowerCase() + this.toStatus.toLowerCase();
    }
}
