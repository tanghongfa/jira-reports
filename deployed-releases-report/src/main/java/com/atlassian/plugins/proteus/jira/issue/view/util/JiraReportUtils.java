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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

/**
 * The purpose of this class is for all the utility APIs
 */
public class JiraReportUtils {

    private final static String JIRA_JSON_CONFIGURATION_ITEM_CUSTOM_FILED_DEPLOYMENT_ENV = "deployEnviornmentField";
    private final static String JIRA_JSON_CONFIGURATION_FILE_LOCATION = "";

    public final static String JIRA_CUSTOM_FILED_DEPLOYMENT_TRACKER = "_deployment_tracker";

    /**
     * @param issueType
     * @return String
     */
    public static String getDeployEnvironmentCustomFieldName(String issueType) {
        //        try {
        //            //TODO: fix up this part later on ... read up the configuration file and get the configuration for it
        //            //            String content = new Scanner(new File("filename")).useDelimiter("\\Z").next();
        //            //            JSONObject obj = new JSONObject(content);
        //
        //            return "DVN2 Environment";
        //
        //        } catch (Exception e) {
        //            // TODO Auto-generated catch block
        //            e.printStackTrace();
        //        }
        //        return null;

        return "DVN2 Environment";
    }

    public static String getDeployEnvironmentCustomFieldName(List<String> issueTypes) {
        //        try {
        //            //TODO: fix up this part later on ... read up the configuration file and get the configuration for it
        //            //            String content = new Scanner(new File("filename")).useDelimiter("\\Z").next();
        //            //            JSONObject obj = new JSONObject(content);
        //
        //            return "DVN2 Environment";
        //
        //        } catch (Exception e) {
        //            // TODO Auto-generated catch block
        //            e.printStackTrace();
        //        }
        //        return null;

        return "DVN2 Environment";
    }

    /**
     * @param customFieldName
     * @return List<String>
     */
    public static List<String> getCustomFieldOptionValues(String customFieldName) {
        CustomField customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customFieldName);
        Options options = ComponentAccessor.getOptionsManager().getOptions(
                customField.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < options.size(); i++) {
            result.add(options.get(i).getValue());
        }

        return result;
    }

    /**
     * It will return all the release issue types. Only issue types
     * that are configured in the JIRA configuration JSON file will be
     * returned (they will be treated as Release Issue)
     * 
     * @param project
     * @return List<String>
     */
    public static List<String> getProjectReleaseIssueTypes(Project project) {
        //TODO: Fix it up by checking if the issue type is configured in the configuration json file
        List<String> result = new ArrayList<String>();
        Collection<IssueType> issueTypes = project.getIssueTypes();
        for (IssueType type : issueTypes) {
            result.add(type.getName());
        }
        return result;
    }

    public static List<String> getDeployedEnvironments(Project project) {
        //Right now, we will assume all the issues within one project will use same target environment configuration (Even though one project may have different workflows etc.)
        String envCustomFieldName = JiraReportUtils.getDeployEnvironmentCustomFieldName(JiraReportUtils
                .getProjectReleaseIssueTypes(project).get(0));
        return JiraReportUtils.getCustomFieldOptionValues(envCustomFieldName);
    }

    /**
     * @param millis
     * @return String
     */
    public static String getDurationBreakdown(long millis) {
        long time = millis;
        if (time < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long days = TimeUnit.MILLISECONDS.toDays(time);
        time -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(time);
        time -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
        time -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time);

        StringBuilder sb = new StringBuilder(64);
        if (days > 0) {
            sb.append(days);
            sb.append("d");
        }
        if (hours > 0) {
            sb.append(hours);
            sb.append("h");
        }
        if (minutes > 0) {
            sb.append(minutes);
            sb.append("m");
        }
        if (seconds > 0) {
            sb.append(seconds);
            sb.append("s");
        }

        return (sb.toString());
    }

    public static Integer sum(List<Integer> list) {
        Integer result = 0;
        for (Integer it : list) {
            result += it;
        }
        return result;
    }

}
