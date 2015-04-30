package com.atlassian.plugins.proteus.jira.reports;

import java.util.ArrayList;
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
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ParameterUtils;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugins.proteus.jira.issue.view.util.DeploymentActivityRecord;
import com.atlassian.plugins.proteus.jira.issue.view.util.IssueInfo;
import com.atlassian.plugins.proteus.jira.issue.view.util.IssueInfoMapperHitCollector;
import com.atlassian.plugins.proteus.jira.issue.view.util.JiraReportUtils;
import com.atlassian.query.Query;

/**
 * This is main report implementation class
 */
public class ProductsDeployedReleasesReport extends AbstractReport {
    private static final Logger log = Logger.getLogger(ProductsDeployedReleasesReport.class);

    private final SearchProvider searchProvider;
    private final ProjectManager projectManager;
    private final DateTimeFormatter dateTimeFormatter;

    private final static int ONE_DAY_IN_MILLIONS = 1 * 24 * 60 * 60 * 1000;

    /**
     * Creates a new instance of
     * <code>ProductsDeployedReleasesReport</code>.
     * 
     * @param searchProvider
     * @param dateTimeFormatter
     * @param projectManager
     */
    public ProductsDeployedReleasesReport(SearchProvider searchProvider, DateTimeFormatter dateTimeFormatter,
            ProjectManager projectManager) {
        this.searchProvider = searchProvider;
        this.dateTimeFormatter = dateTimeFormatter;
        this.projectManager = projectManager;
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

        Project project = projectManager.getProjectObj(projectId);
        List<String> releaseIssueTypes = JiraReportUtils.getProjectReleaseIssueTypes(project);

        //There is NO release issue type for this project, the report is NOT applicable and will not do anything
        if ((releaseIssueTypes == null) || (releaseIssueTypes.size() <= 0)) {
            return new ArrayList<IssueInfo>();
        }

        JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
        Query query = queryBuilder.where().createdBetween(startDate, endDate).and().project(projectId).and()
                .issueType(releaseIssueTypes.toArray(new String[0])).buildQuery();

        List<IssueInfo> data = new ArrayList<IssueInfo>();

        final FieldableDocumentHitCollector hitCollector = new IssueInfoMapperHitCollector(data,
                ComponentAccessor.getIssueFactory()) {

            @Override
            protected void writeIssue(Issue issue, List<IssueInfo> result) {

                ChangeHistoryManager historyManager = ComponentAccessor.getChangeHistoryManager();
                List<ChangeItemBean> changes = historyManager.getChangeItemsForField(issue,
                        JiraReportUtils.JIRA_CUSTOM_FILED_DEPLOYMENT_TRACKER);
                List<DeploymentActivityRecord> changeRcd = new ArrayList<DeploymentActivityRecord>();

                for (ChangeItemBean change : changes) {
                    changeRcd.add(new DeploymentActivityRecord(change.getToString(), change.getCreated()));
                }

                IssueInfo info = new IssueInfo();
                info.setIssueNo(issue.getId()).setIssueTitle(issue.getSummary()).setIssueKey(issue.getKey())
                        .setActivityRcd(changeRcd);

                result.add(info);
            }

        };

        searchProvider.searchAndSort(query, remoteUser, hitCollector, PagerFilter.getUnlimitedFilter());

        return data;

    }

    private String generateReportHtml(ProjectActionSupport action, Map params, String view, boolean isExcel)
            throws Exception {
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
        List<String> envList = JiraReportUtils.getDeployedEnvironments(projectManager.getProjectObj(projectId));

        // Pass the issues to the velocity template
        Map<String, Object> velocityParams = new HashMap<String, Object>();
        velocityParams.put("startDate", startDate);
        velocityParams.put("endDate", endDate);
        velocityParams.put("deploymentResult", params.get("deploymentResult"));
        velocityParams.put("projectName", projectManager.getProjectObj(projectId).getName());
        velocityParams.put("dateFormatter", dateTimeFormatter.withStyle(DateTimeStyle.DATE_PICKER).forLoggedInUser());
        velocityParams.put("dateTimeFormatter", dateTimeFormatter.withStyle(DateTimeStyle.COMPLETE).forLoggedInUser());
        velocityParams.put("environments", envList);
        velocityParams.put("issues", data);
        velocityParams.put("today", new Date());
        velocityParams.put("isExcel", isExcel);

        return descriptor.getHtml(view, velocityParams);
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
