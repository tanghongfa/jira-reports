package com.atlassian.plugins.proteus.jira.issue.view.util;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.statistics.util.DocumentHitCollector;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

import java.util.List;
import java.util.Map;

public abstract class IssueInfoMapperHitCollector extends DocumentHitCollector {
	protected final Map<IssueInfo, List<DeploymentActivityRecord>> data;
	private final IssueFactory issueFactory;

	public IssueInfoMapperHitCollector(IndexSearcher searcher, Map<IssueInfo, List<DeploymentActivityRecord>> data, IssueFactory issueFactory) {
		super(searcher);
		this.data = data;
		this.issueFactory = issueFactory;
	}

	public void collect(Document d) {
		Issue issue = issueFactory.getIssue(d);
		writeIssue(issue, data);
	}

	protected abstract void writeIssue(Issue issue, Map<IssueInfo, List<DeploymentActivityRecord>> data);
}