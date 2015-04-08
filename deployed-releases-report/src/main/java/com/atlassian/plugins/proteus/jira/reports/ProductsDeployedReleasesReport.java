package com.atlassian.plugins.proteus.jira.reports;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.atlassian.plugins.proteus.jira.issue.view.util.IssueInfo;
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
        
        Long projectId = ParameterUtils.getLongParam(params, "selectedProjectId");
        Date startDate = ParameterUtils.getDateParam(params, "startDate", i18nBean.getLocale());
        Date endDate = ParameterUtils.getDateParam(params, "endDate", i18nBean.getLocale());
        
        //Load all the required data
        List<IssueInfo> data = loadIssueData(startDate, endDate, remoteUser, projectId);
        Collections.sort(data);       
        
        Set<String> deployedEnvironments = new HashSet<String>();
        for(int i = 0; i < data.size(); i ++) {
        	deployedEnvironments.addAll(data.get(i).getDeployedEnvironments());
        }
        List<String> envList = new ArrayList<String>(deployedEnvironments);
        Collections.sort(envList);
                     
        // Pass the issues to the velocity template
        Map<String, Object> velocityParams = new HashMap<String, Object>();
        velocityParams.put("startDate", startDate);
        velocityParams.put("endDate", endDate);       
        velocityParams.put("projectName", projectManager.getProjectObj(projectId).getName());
        velocityParams.put("environments", envList);
        velocityParams.put("issues", data);
        

        return descriptor.getHtml("view", velocityParams);
    }
    
    private List<IssueInfo> loadIssueData(Date startDate, Date endDate, User remoteUser, Long projectId) throws SearchException {
    	 JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
         Query query = queryBuilder.where().createdBetween(startDate, endDate).and().project(projectId).buildQuery();
         
         SearchProviderFactory searchProviderFactory = new SearchProviderFactoryImpl();
         IssueFactory issueFactory = ComponentAccessor.getIssueFactory();
        
         
         IndexSearcher searcher = searchProviderFactory.getSearcher(SearchProviderFactory.ISSUE_INDEX);
         List<IssueInfo> data = new ArrayList<IssueInfo>();
         
         final DocumentHitCollector hitCollector = new IssueInfoMapperHitCollector(searcher, data, issueFactory)
         {

			@Override
			protected void writeIssue(Issue issue, List<IssueInfo> data) {
				log.error(issue.getSummary());
                ChangeHistoryManager historyManager = ComponentAccessor.getChangeHistoryManager();                 
                List<ChangeItemBean> changes = historyManager.getChangeItemsForField(issue, "_deployment_tracker");
                List<DeploymentActivityRecord> changeRcd = new ArrayList<DeploymentActivityRecord>();
                for(ChangeItemBean change : changes) {
                	String dataStr = "hongfa..." + change.getFromString() + "," + change.getToString();
               	 	log.error(dataStr);
               	 	changeRcd.add(new DeploymentActivityRecord(change.getToString(), change.getCreated()));
                }
                IssueInfo info = new IssueInfo();
                info.setIssueNo(issue.getId()).setIssueTitle(issue.getSummary()).setIssueKey(issue.getKey()).setActivityRcd(changeRcd);
                data.add(info);
			}
         };
         
         searchProvider.searchAndSort(query, remoteUser, hitCollector, PagerFilter.getUnlimitedFilter());
         
         return data;
         
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
