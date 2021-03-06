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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * The purpose of this class is for all the utility APIs
 */
public class JiraReportUtils {
    private static final Logger log = Logger.getLogger(JiraReportUtils.class);

    private final static String JIRA_JSON_CONFIGURATION_CUSTOM_FIELDS_CONFIGURATION_ITEM = "customFields";
    private final static String JIRA_JSON_CONFIGURATION_ITEM_CUSTOM_FILED_DEPLOYMENT_ENV = "deployEnvironmentField";

    /**
     * <code>JIRA_CUSTOM_FILED_DEPLOYMENT_TRACKER</code>
     */
    public final static String JIRA_CUSTOM_FILED_DEPLOYMENT_TRACKER = "_deployment_tracker";

    private static String getJiraJsonConfigurationFileName(String issueType) {
        String baseFolder = "/opt/atlassian/jira/scripts/conf/";
        String osName = System.getProperty("os.name");
        if ((osName != null) && osName.toLowerCase().contains("windows")) {
            baseFolder = "C:\\jira_reports\\scripts\\conf\\";
        }
        return baseFolder + issueType.replaceAll(" ", "_") + ".json";
    }

    /**
     * @param issueType
     * @return String
     */
    public static String getDeployEnvironmentCustomFieldName(String issueType) {
        Scanner fileScanner = null;
        try {
            fileScanner = new Scanner(new File(getJiraJsonConfigurationFileName(issueType)));
            String content = fileScanner.useDelimiter("\\Z").next();

            JSONObject issueTypeConfiguration = new JSONObject(content);
            JSONObject customFieldConfiguraiton = issueTypeConfiguration
                    .getJSONObject(JIRA_JSON_CONFIGURATION_CUSTOM_FIELDS_CONFIGURATION_ITEM);
            return customFieldConfiguraiton.getString(JIRA_JSON_CONFIGURATION_ITEM_CUSTOM_FILED_DEPLOYMENT_ENV);
        } catch (JSONException e) {
            //Ignore -- It's quite normal
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (fileScanner != null) {
                fileScanner.close();
            }
        }
        return null;
    }

    /**
     * @param issueTypes
     * @return String
     */
    public static String getDeployEnvironmentCustomFieldName(List<String> issueTypes) {
        for (String issueType : issueTypes) {
            String fieldName = getDeployEnvironmentCustomFieldName(issueType);
            if (fieldName != null) {
                return fieldName;
            }
        }
        return null;
    }

    private static List<String> getReleaseIssueTypes(List<String> issueTypes) {
        List<String> result = new ArrayList<String>();
        try {
            for (String issueType : issueTypes) {
                File file = new File(getJiraJsonConfigurationFileName(issueType));
                //If configuration file exists, then it will be recognized as Release Issue Type (For other issue types, they shouldn't have configuration file there)
                if (file.exists()) {
                    result.add(issueType);
                }
            }
            //Ignore -- It's quite normal
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return result;
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
        List<String> result = new ArrayList<String>();
        Collection<IssueType> issueTypes = project.getIssueTypes();
        for (IssueType type : issueTypes) {
            result.add(type.getName());
        }
        result = getReleaseIssueTypes(result);
        if (result.size() == 0) {
            log.error("No release issue type is configured for project:" + project.getName());
        }
        return result;
    }

    /**
     * @param project
     * @return List<String>
     */
    public static List<String> getDeployedEnvironments(Project project) {
        //Right now, we will assume all the issues within one project will use same target environment configuration (Even though one project may have different workflows etc.)
        String envCustomFieldName = JiraReportUtils.getDeployEnvironmentCustomFieldName(JiraReportUtils
                .getProjectReleaseIssueTypes(project));
        if (null == envCustomFieldName) {
            log.error("Deployment Environment Custom Field is NOT configured for the release workflows for project:"
                    + project.getName());
            return new ArrayList<String>();
        }
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

    /**
     * @param list
     * @return Integer
     */
    public static Integer sum(List<Integer> list) {
        Integer result = 0;
        for (Integer it : list) {
            result += it;
        }
        return result;
    }

}
