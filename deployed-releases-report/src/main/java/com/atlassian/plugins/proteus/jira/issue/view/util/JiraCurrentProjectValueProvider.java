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

import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;

import webwork.action.ActionContext;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.component.ComponentAccessor;

public class JiraCurrentProjectValueProvider implements ValuesGenerator {
    private static final Logger log = Logger.getLogger(JiraCurrentProjectValueProvider.class);

    @Override
    public Map getValues(Map arg0) {
        Map allValues = new ListOrderedMap();

        String[] ids = (String[]) ActionContext.getParameters().get("selectedProjectId");
        String pid = ids[0];

        String projectName = ComponentAccessor.getProjectManager().getProjectObj(Long.parseLong(pid)).getName();
        allValues.put(pid, projectName);

        return allValues;
    }
}
