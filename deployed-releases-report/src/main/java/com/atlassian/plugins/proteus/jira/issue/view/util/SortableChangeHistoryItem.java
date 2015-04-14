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
 * The purpose of this class is add sorting feature for the
 * history/change item
 */
public class SortableChangeHistoryItem extends ChangeItemBean implements Comparable<SortableChangeHistoryItem> {

    /**
     * Creates a new instance of
     * <code>SortableChangeHistoryItem</code>.
     * 
     * @param item
     */
    public SortableChangeHistoryItem(ChangeItemBean item) {
        super(item.getFieldType(), item.getField(), item.getFrom(), item.getFromString(), item.getTo(), item
                .getToString(), item.getCreated());
    }

    @Override
    public int compareTo(SortableChangeHistoryItem anotherItem) {
        return anotherItem.getCreated().compareTo(this.getCreated());
    }

}
