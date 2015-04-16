/*------------------------------------------------------------------------------
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *----------------------------------------------------------------------------*/
package com.atlassian.plugins.proteus.jira.reports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryItem;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.statistics.util.FieldableDocumentHitCollector;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ParameterUtils;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugins.proteus.jira.issue.view.util.IssueInfo;
import com.atlassian.plugins.proteus.jira.issue.view.util.IssueInfoMapperHitCollector;
import com.atlassian.plugins.proteus.jira.issue.view.util.SortableChangeHistoryItem;
import com.atlassian.query.Query;

/**
 * The purpose of this class is for Open work flow status report
 */
public class ProductsOpenReleaseWorkflowStatusReport extends AbstractReport {
    private static final Logger log = Logger.getLogger(ProductsOpenReleaseWorkflowStatusReport.class);

    private final SearchProvider searchProvider;
    private final ProjectManager projectManager;
    private final DateTimeFormatter dateTimeFormatter;

    private final static String JIRA_CUSTOM_FILED_DEPLOYED_ENVIRONMENT = "DVN2 Environment";
    private final static String JIRA_FILED_STATUS = "status";

    /**
     * Creates a new instance of
     * <code>ProductsDeployedReleasesReport</code>.
     * 
     * @param searchProvider
     * @param dateTimeFormatter
     * @param projectManager
     */
    public ProductsOpenReleaseWorkflowStatusReport(SearchProvider searchProvider, DateTimeFormatter dateTimeFormatter,
            ProjectManager projectManager) {
        this.searchProvider = searchProvider;
        this.dateTimeFormatter = dateTimeFormatter;
        this.projectManager = projectManager;
    }

    private String generateReportHtml(ProjectActionSupport action, Map params, String view, boolean isExcel)
            throws Exception {
        ApplicationUser remoteUser = action.getLoggedInApplicationUser();
        Long projectId = ParameterUtils.getLongParam(params, "selectedProjectId");

        CustomField dployedEnvField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
                JIRA_CUSTOM_FILED_DEPLOYED_ENVIRONMENT);

        // Load all the required data
        List<IssueInfo> data = loadIssueData(remoteUser, projectId);
        Collections.sort(data);

        // Get all the deployed environment information
        Options options = ComponentAccessor.getOptionsManager().getOptions(
                dployedEnvField.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());
        List<String> envList = new ArrayList<String>();
        for (int i = 0; i < options.size(); i++) {
            envList.add(options.get(i).getValue());
        }

        // Pass the issues to the velocity template
        Map<String, Object> velocityParams = new HashMap<String, Object>();
        velocityParams.put("projectName", projectManager.getProjectObj(projectId).getName());
        velocityParams.put("dateTimeFormatter", dateTimeFormatter.withStyle(DateTimeStyle.COMPLETE).forLoggedInUser());
        velocityParams.put("environments", envList);
        velocityParams.put("issues", data);
        velocityParams.put("today", new Date());
        velocityParams.put("isExcel", isExcel);

        return descriptor.getHtml(view, velocityParams);
    }

    /**
     * This function will search the JIRA database and get the
     * filtered issue result set and then extract/transform the
     * information for deployment/roll back activities. TDOO: This
     * function could be reused by other reports.If that is the case,
     * just move this function out.
     * 
     * @param remoteUser
     * @param projectId
     * @return List<IssueInfo> data
     * @throws SearchException
     */
    private List<IssueInfo> loadIssueData(ApplicationUser remoteUser, Long projectId) throws SearchException {
        JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
        Query query = queryBuilder.where().not().statusCategory(StatusCategory.COMPLETE).and().project(projectId)
                .buildQuery();

        List<IssueInfo> data = new ArrayList<IssueInfo>();

        final FieldableDocumentHitCollector hitCollector = new IssueInfoMapperHitCollector(data,
                ComponentAccessor.getIssueFactory()) {

            private List<SortableChangeHistoryItem> getFieldSortableChangeHistory(ChangeHistoryManager historyManager,
                    Issue issue, String fieldName) {
                List<ChangeItemBean> changes = historyManager.getChangeItemsForField(issue, fieldName);
                List<SortableChangeHistoryItem> changeRcd = new ArrayList<SortableChangeHistoryItem>();

                for (ChangeItemBean change : changes) {
                    changeRcd.add(new SortableChangeHistoryItem(change));
                }

                log.error(changeRcd);

                return changeRcd;
            }

            @Override
            protected void writeIssue(Issue issue, List<IssueInfo> result) {

                log.error("hongfa..." + issue.getSummary());

                IssueInfo info = new IssueInfo();
                info.setIssueNo(issue.getId()).setIssueTitle(issue.getSummary()).setIssueKey(issue.getKey())
                        .setIssueStatus(issue.getStatusObject().getName());

                CustomField dployedEnvField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(
                        JIRA_CUSTOM_FILED_DEPLOYED_ENVIRONMENT);
                Object customFieldValue = issue.getCustomFieldValue(dployedEnvField);
                if (customFieldValue != null) {
                    info.setIssueDeployEnv(customFieldValue.toString());
                    log.error("hongfa... current env..." + info.getIssueDeployEnv());
                }

                ChangeHistoryManager historyManager = ComponentAccessor.getChangeHistoryManager();

                List<ChangeHistoryItem> tmp = historyManager.getAllChangeItems(issue);
                for (ChangeHistoryItem tmpItem : tmp) {
                    log.error(tmpItem.getField());
                }

                info.setStatusChangeRcd(this.getFieldSortableChangeHistory(historyManager, issue, JIRA_FILED_STATUS));
                info.setEnvironmentChangeRcd(this.getFieldSortableChangeHistory(historyManager, issue,
                        JIRA_CUSTOM_FILED_DEPLOYED_ENVIRONMENT));

                result.add(info);
            }

        };

        searchProvider.searchAndSort(query, remoteUser, hitCollector, PagerFilter.getUnlimitedFilter());

        return data;

    }

    // Validate the parameters set by the user.
    @Override
    public void validate(ProjectActionSupport action, Map params) {
        Long projectId = ParameterUtils.getLongParam(params, "selectedProjectId");

        if (projectId == null) {
            action.addError("selectedProjectId", "Please choose a valid project ID");
        }
    }

    @Override
    public String generateReportHtml(ProjectActionSupport action, Map params) throws Exception {
        return generateReportHtml(action, params, "view", false);
    }

    @Override
    public boolean isExcelViewSupported() {
        return true;
    }

    @Override
    public String generateReportExcel(ProjectActionSupport action, @SuppressWarnings("rawtypes") Map params)
            throws Exception {
        return generateReportHtml(action, params, "view", true);
    }
}
