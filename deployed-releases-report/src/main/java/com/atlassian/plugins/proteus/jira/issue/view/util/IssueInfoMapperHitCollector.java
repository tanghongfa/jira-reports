package com.atlassian.plugins.proteus.jira.issue.view.util;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.statistics.util.DocumentHitCollector;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

import java.util.List;

public abstract class IssueInfoMapperHitCollector extends DocumentHitCollector {
	protected final List<DeploymentActivityRecord> data;
	private final IssueFactory issueFactory;

	public IssueInfoMapperHitCollector(IndexSearcher searcher,
			List<DeploymentActivityRecord> data, IssueFactory issueFactory) {
		super(searcher);
		this.data = data;
		this.issueFactory = issueFactory;
	}

	public void collect(Document d) {
		Issue issue = issueFactory.getIssue(d);
		writeIssue(issue, data);
	}

	protected abstract void writeIssue(Issue issue, List<DeploymentActivityRecord> data);
}