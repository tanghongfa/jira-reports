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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.statistics.util.FieldableDocumentHitCollector;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ParameterUtils;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugins.proteus.jira.issue.view.util.IssueInfo;
import com.atlassian.plugins.proteus.jira.issue.view.util.IssueInfoMapperHitCollector;
import com.atlassian.plugins.proteus.jira.issue.view.util.IssueWorkflowTransitionRcd;
import com.atlassian.plugins.proteus.jira.issue.view.util.SortableChangeHistoryItem;
import com.atlassian.plugins.proteus.jira.issue.view.util.WorkflowTransitions;
import com.atlassian.query.Query;

/**
 * The purpose of this class is for Product Release workflow time
 * spent on each activities report
 */
public class ProductsReleaseWorkflowTimeStaticsReport extends AbstractReport {
    private static final Logger log = Logger.getLogger(ProductsReleaseWorkflowTimeStaticsReport.class);

    private final SearchProvider searchProvider;
    private final ProjectManager projectManager;
    private final DateTimeFormatter dateTimeFormatter;

    private final static String JIRA_FILED_STATUS = "status";

    private final static int ONE_DAY_IN_MILLIONS = 1 * 24 * 60 * 60 * 1000;

    /**
     * Creates a new instance of
     * <code>ProductsDeployedReleasesReport</code>.
     * 
     * @param searchProvider
     * @param dateTimeFormatter
     * @param projectManager
     */
    public ProductsReleaseWorkflowTimeStaticsReport(SearchProvider searchProvider, DateTimeFormatter dateTimeFormatter,
            ProjectManager projectManager) {
        this.searchProvider = searchProvider;
        this.dateTimeFormatter = dateTimeFormatter;
        this.projectManager = projectManager;
    }

    /**
     * Try to get all the identical transitions that happened to all
     * the involved issues. It could be possibly analysis workflow(s)
     * associated with the issues to get this result, but that could
     * be a huge list since couple of the status could be from changed
     * "all" (e.g. "Close" status)
     * 
     * @param data
     * @return List<WorkflowTransitions>
     */
    private List<WorkflowTransitions> getIdenticalTransitions(List<IssueInfo> data) {
        Map<String, WorkflowTransitions> resultMap = new HashMap<String, WorkflowTransitions>();
        for (IssueInfo issue : data) {
            for (int i = 0; i < issue.getStatusChangeRcd().size(); i++) {
                WorkflowTransitions transition = new WorkflowTransitions(issue.getStatusChangeRcd().get(i), i);
                if (resultMap.get(transition.getUniqueId()) == null) {
                    resultMap.put(transition.getUniqueId(), transition);
                }
            }
        }
        WorkflowTransitions[] list = resultMap.values().toArray(new WorkflowTransitions[0]);
        List<WorkflowTransitions> result = Arrays.asList(list);
        Collections.sort(result);
        return result;
    }

    private List<List<IssueWorkflowTransitionRcd>> getTransitionDurationData(List<IssueInfo> data,
            List<WorkflowTransitions> transitions) {

        List<List<IssueWorkflowTransitionRcd>> result = new ArrayList<List<IssueWorkflowTransitionRcd>>();

        List<IssueWorkflowTransitionRcd> averageDataRow = new ArrayList<IssueWorkflowTransitionRcd>();
        for (int i = 0; i < transitions.size(); i++) {
            averageDataRow.add(new IssueWorkflowTransitionRcd(transitions.get(i), null, new ArrayList<Integer>()));
        }
        result.add(averageDataRow); // First Row is the average data

        for (IssueInfo issue : data) {
            List<IssueWorkflowTransitionRcd> oneRow = new ArrayList<IssueWorkflowTransitionRcd>();
            for (int i = 0; i < transitions.size(); i++) {
                List<Integer> timeSpend = issue.getTimeSpentOnTransition(transitions.get(i));
                oneRow.add(new IssueWorkflowTransitionRcd(transitions.get(i), issue, timeSpend));
                averageDataRow.get(i).appendActualData(timeSpend);
            }
            result.add(oneRow);
        }

        return result;
    }

    /**
     * Generating the HTML(String) Report
     */
    @Override
    public String generateReportHtml(ProjectActionSupport action, Map params) throws Exception {
        ApplicationUser remoteUser = action.getLoggedInApplicationUser();

        Long projectId = ParameterUtils.getLongParam(params, "selectedProjectId");
        Date startDate = dateTimeFormatter.withStyle(DateTimeStyle.DATE_PICKER).forLoggedInUser()
                .parse((String) params.get("startDate"));
        Date endDate = dateTimeFormatter.withStyle(DateTimeStyle.DATE_PICKER).forLoggedInUser()
                .parse((String) params.get("endDate"));

        // Load all the required data
        List<IssueInfo> data = loadIssueData(startDate, new Date(endDate.getTime() + ONE_DAY_IN_MILLIONS), remoteUser,
                projectId);
        Collections.sort(data);

        // Get all the deployed environment information
        List<WorkflowTransitions> transitionsList = getIdenticalTransitions(data);

        //Get all the transition data
        List<List<IssueWorkflowTransitionRcd>> tableData = getTransitionDurationData(data, transitionsList);
        List<IssueWorkflowTransitionRcd> averageData = tableData.get(0);
        List<List<IssueWorkflowTransitionRcd>> issueRelatedData = tableData.subList(1, tableData.size());

        //Get the top 10 time consuming items
        IssueWorkflowTransitionRcd[] sortedAverageData = averageData.toArray(new IssueWorkflowTransitionRcd[0]);
        Arrays.sort(sortedAverageData, IssueWorkflowTransitionRcd.TransitionAverageSpentTimeComparator);
        //List<IssueWorkflowTransitionRcd> topTimeConsumingTransitions = Arrays.asList(sortedAverageData).subList(ONE_DAY_IN_MILLIONS, ONE_DAY_IN_MILLIONS);

        // Pass the issues to the velocity template
        Map<String, Object> velocityParams = new HashMap<String, Object>();
        velocityParams.put("startDate", startDate);
        velocityParams.put("endDate", endDate);
        velocityParams.put("deploymentResult", params.get("deploymentResult"));
        velocityParams.put("projectName", projectManager.getProjectObj(projectId).getName());
        velocityParams.put("dateFormatter", dateTimeFormatter.withStyle(DateTimeStyle.DATE_PICKER).forLoggedInUser());
        velocityParams.put("dateTimeFormatter", dateTimeFormatter.withStyle(DateTimeStyle.COMPLETE).forLoggedInUser());
        velocityParams.put("transitions", transitionsList);
        velocityParams.put("tableData", issueRelatedData);
        velocityParams.put("summery", averageData);
        velocityParams.put("issues", data);
        velocityParams.put("today", new Date());

        return descriptor.getHtml("view", velocityParams);
    }

    /**
     * This function will search the JIRA database and get the
     * filtered issue result set and then extract/transform the
     * information for deployment/roll back activities. TDOO: This
     * function could be reused by other reports.If that is the case,
     * just move this function out.
     * 
     * @param startDate
     * @param endDate
     * @param remoteUser
     * @param projectId
     * @return List<IssueInfo> data
     * @throws SearchException
     */
    private List<IssueInfo> loadIssueData(Date startDate, Date endDate, ApplicationUser remoteUser, Long projectId)
            throws SearchException {
        JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
        Query query = queryBuilder.where().createdBetween(startDate, endDate).and().project(projectId).buildQuery();

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
                ChangeHistoryManager historyManager = ComponentAccessor.getChangeHistoryManager();

                IssueInfo info = new IssueInfo();
                info.setIssueNo(issue.getId()).setIssueTitle(issue.getSummary()).setIssueKey(issue.getKey())
                        .setIssueCreated(issue.getCreated());

                info.setStatusChangeRcd(this.getFieldSortableChangeHistory(historyManager, issue, JIRA_FILED_STATUS));

                result.add(info);
            }

        };

        searchProvider.searchAndSort(query, remoteUser, hitCollector, PagerFilter.getUnlimitedFilter());

        return data;

    }

    // Validate the parameters set by the user.
    @Override
    public void validate(ProjectActionSupport action, Map params) {
        String startDateParam = (String) params.get("startDate");
        String endDateParam = (String) params.get("endDate");
        Date startDate = null;
        Date endDate = null;

        if (StringUtils.isNotEmpty(startDateParam)) {
            startDate = dateTimeFormatter.withStyle(DateTimeStyle.DATE_PICKER).forLoggedInUser().parse(startDateParam);
        }
        if (StringUtils.isNotEmpty(endDateParam)) {
            endDate = dateTimeFormatter.withStyle(DateTimeStyle.DATE_PICKER).forLoggedInUser().parse(endDateParam);
        }

        Long projectId = ParameterUtils.getLongParam(params, "selectedProjectId");

        if (startDate == null) {
            action.addError("startDate", "Please choose a start date for the report");
        }

        if ((endDate == null) || endDate.before(startDate)) {
            action.addError("endDate", "Please choose a valid end date for the report");
        }

        if (projectId == null) {
            action.addError("selectedProjectId", "Please choose a valid project ID");
        }
    }

    @Override
    public boolean isExcelViewSupported() {
        return true;
    }

    @Override
    public String generateReportExcel(ProjectActionSupport action, @SuppressWarnings("rawtypes") Map params)
            throws Exception {
        String result = generateReportHtml(action, params);//"<table><tr><td>Jill</td><td>Smith</td><td>50</td></tr><tr><td>Eve</td><td>Jackson</td><td>94</td></tr><tr><td>John</td><td>Doe</td><td>80</td></tr></table>";
        return result;
    }
}
