package com.atlassian.plugins.proteus.jira.reports;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.search.IndexSearcher;

import com.atlassian.core.util.DateUtils;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.DefaultIssueFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchProviderFactory;
import com.atlassian.jira.issue.search.SearchProviderFactoryImpl;
import com.atlassian.jira.issue.statistics.util.DocumentHitCollector;
import com.atlassian.jira.issue.views.util.IssueWriterHitCollector;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.ParameterUtils;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.web.util.OutlookDate;
import com.atlassian.jira.web.util.OutlookDateManager;
import com.atlassian.plugins.tutorial.jira.reports.CreationReport;
import com.atlassian.query.Query;
import com.atlassian.plugins.proteus.jira.issue.view.util.DeploymentActivityRecord;
import com.atlassian.plugins.proteus.jira.issue.view.util.IssueInfoMapperHitCollector;


public class ProductsDeployedReleasesReport extends AbstractReport
{
    private static final Logger log = Logger.getLogger(CreationReport.class);

    // The max height for each bar in the histogram
    private static final int MAX_HEIGHT = 200;
    // Default interval value
    private Long DEFAULT_INTERVAL = new Long(7);

    // The highest issue count encountered in a search
    private long maxCount = 0;
    // A collection of issue open counts
    private Collection<Long> openIssueCounts = new ArrayList<Long>();
    // A collection of interval start dates - correlating with the openIssueCount collection.
    private Collection<Date> dates = new ArrayList<Date>();

    private final SearchProvider searchProvider;
    private final OutlookDateManager outlookDateManager;
    private final ProjectManager projectManager;

    public ProductsDeployedReleasesReport(SearchProvider searchProvider, OutlookDateManager outlookDateManager, ProjectManager projectManager)
    {
        this.searchProvider = searchProvider;
        this.outlookDateManager = outlookDateManager;
        this.projectManager = projectManager;
    }

    // Generate the report
    public String generateReportHtml(ProjectActionSupport action, Map params) throws Exception
    {
        User remoteUser = action.getRemoteUser();
        I18nHelper i18nBean = new I18nBean(remoteUser);

        // Retrieve the project parameter
        Long projectId = ParameterUtils.getLongParam(params, "selectedProjectId");
        // Retrieve the start and end dates and the time interval specified by the user
        Date startDate = ParameterUtils.getDateParam(params, "startDate", i18nBean.getLocale());
        Date endDate = ParameterUtils.getDateParam(params, "endDate", i18nBean.getLocale());
        Long interval = ParameterUtils.getLongParam(params, "interval");

        // Ensure that the interval is valid
        if (interval == null || interval.longValue() <= 0)
        {
            interval = DEFAULT_INTERVAL;
            log.error(action.getText("report.issuecreation.default.interval"));
        }

        getIssueCount(startDate, endDate, interval, remoteUser, projectId);
        
        List<DeploymentActivityRecord> data = loadIssueData(startDate, endDate, interval, remoteUser, projectId);
        log.error(data.toString());

        List<Number> normalCount = new ArrayList<Number>();

        // Normalise the counts for the max height
        if (maxCount != MAX_HEIGHT && maxCount > 0)
        {
            for (Long asLong : openIssueCounts)
            {
                Float floatValue = new Float((asLong.floatValue() / maxCount) * MAX_HEIGHT);
                // Round it back to an integer
                Integer newValue = new Integer(floatValue.intValue());

                normalCount.add(newValue);
            }
        }

        if (maxCount < 0)
            action.addErrorMessage(action.getText("report.issuecreation.error"));

        // Pass the issues to the velocity template
        Map<String, Object> velocityParams = new HashMap<String, Object>();
        velocityParams.put("startDate", startDate);
        velocityParams.put("endDate", endDate);
        velocityParams.put("openCount", openIssueCounts);
        velocityParams.put("normalisedCount", normalCount);
        velocityParams.put("dates", dates);
        velocityParams.put("maxHeight", new Integer(MAX_HEIGHT));
        velocityParams.put("outlookDate", outlookDateManager.getOutlookDate(i18nBean.getLocale()));
        velocityParams.put("projectName", projectManager.getProjectObj(projectId).getName());
        velocityParams.put("interval", interval);

        return descriptor.getHtml("view", velocityParams);
    }
    
    private List<DeploymentActivityRecord> loadIssueData(Date startDate, Date endDate, Long interval, User remoteUser, Long projectId) throws SearchException {
    	 JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
         Query query = queryBuilder.where().createdBetween(startDate, endDate).and().project(projectId).buildQuery();
         
         SearchProviderFactory searchProviderFactory = new SearchProviderFactoryImpl();
         IssueFactory issueFactory = ComponentAccessor.getIssueFactory();
        
         
         IndexSearcher searcher = searchProviderFactory.getSearcher(SearchProviderFactory.ISSUE_INDEX);
         List<DeploymentActivityRecord> data = new ArrayList<DeploymentActivityRecord>();
         
         final DocumentHitCollector hitCollector = new IssueInfoMapperHitCollector(searcher, data, issueFactory)
         {

			@Override
			protected void writeIssue(Issue issue, List<DeploymentActivityRecord> data) {
				log.error(issue.getSummary());
                ChangeHistoryManager historyManager = ComponentAccessor.getChangeHistoryManager();                 
                List<ChangeItemBean> changes = historyManager.getChangeItemsForField(issue, "_deployment_tracker");
                
                for(ChangeItemBean change : changes) {
                	String dataStr = "hongfa..." + change.getFromString() + "," + change.getToString();
               	 	log.error(dataStr);
               	 	data.add(new DeploymentActivityRecord(change.getToString(), change.getCreated()));
                }
			}
         };
         
         searchProvider.searchAndSort(query, remoteUser, hitCollector, PagerFilter.getUnlimitedFilter());
         
         return data;
         
    }
    

    // Retrieve the issues opened during the time period specified.
    private long getOpenIssueCount(User remoteUser, Date startDate, Date endDate, Long projectId) throws SearchException
    {
        JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
        Query query = queryBuilder.where().createdBetween(startDate, endDate).and().project(projectId).buildQuery();

        return searchProvider.searchCount(query, remoteUser);
    }

    private void getIssueCount(Date startDate, Date endDate, Long interval, User remoteUser, Long projectId) throws SearchException
    {
        // Calculate the interval value in milliseconds
        long intervalValue = interval.longValue() * DateUtils.DAY_MILLIS;
        Date newStartDate;
        long count = 0;

        // Split the specified time period by the interval value
        while (startDate.before(endDate))
        {
            newStartDate = new Date(startDate .getTime() + intervalValue);

            // Retrieve the issues opened within the time interval
            if (newStartDate.after(endDate))
                count = getOpenIssueCount(remoteUser, startDate, endDate, projectId);
            else
                count = getOpenIssueCount(remoteUser, startDate, newStartDate, projectId);

            // Store the highest count for normalisation of results
            if (maxCount < count)
                maxCount = count;

            // Store the count and the start date for this period
            openIssueCounts.add(new Long(count));
            dates.add(startDate);

            // Move start date to next period
            startDate = newStartDate;
        }
    }

    // Validate the parameters set by the user.
    public void validate(ProjectActionSupport action, Map params)
    {
        User remoteUser = action.getRemoteUser();
        I18nHelper i18nBean = new I18nBean(remoteUser);

        Date startDate = ParameterUtils.getDateParam(params, "startDate", i18nBean.getLocale());
        Date endDate = ParameterUtils.getDateParam(params, "endDate", i18nBean.getLocale());
        Long interval = ParameterUtils.getLongParam(params, "interval");
        Long projectId = ParameterUtils.getLongParam(params, "selectedProjectId");

        OutlookDate outlookDate = outlookDateManager.getOutlookDate(i18nBean.getLocale());

        if (startDate == null || !outlookDate.isDatePickerDate(outlookDate.formatDMY(startDate)))
            action.addError("startDate", action.getText("report.issuecreation.startdate.required"));

        if (endDate == null || !outlookDate.isDatePickerDate(outlookDate.formatDMY(endDate)))
            action.addError("endDate", action.getText("report.issuecreation.enddate.required"));

        if (interval == null || interval.longValue() <= 0)
            action.addError("interval", action.getText("report.issuecreation.interval.invalid"));

        if (projectId == null)
            action.addError("selectedProjectId", action.getText("report.issuecreation.projectid.invalid"));

        // The end date must be after the start date
        if (startDate != null && endDate != null && endDate.before(startDate))
        {
            action.addError("endDate", action.getText("report.issuecreation.before.startdate"));
        }
    }
    
    public boolean isExcelViewSupported() {
    	return true;
    }
    
    public String generateReportExcel(ProjectActionSupport action, @SuppressWarnings("rawtypes") Map params) throws Exception {
    	String result = "<table><tr><td>Jill</td><td>Smith</td><td>50</td></tr><tr><td>Eve</td><td>Jackson</td><td>94</td></tr><tr><td>John</td><td>Doe</td><td>80</td></tr></table>";
    	return result;
    }
}
