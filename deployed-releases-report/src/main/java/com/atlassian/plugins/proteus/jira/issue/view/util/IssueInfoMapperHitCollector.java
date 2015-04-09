package com.atlassian.plugins.proteus.jira.issue.view.util;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.statistics.util.FieldableDocumentHitCollector;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;

import java.util.List;

public abstract class IssueInfoMapperHitCollector extends FieldableDocumentHitCollector {
	protected final List<IssueInfo> data;
	private final IssueFactory issueFactory;

	public IssueInfoMapperHitCollector(List<IssueInfo> data, IssueFactory issueFactory) {
		this.data = data;
		this.issueFactory = issueFactory;
	}

	public void collect(Document d) {
		Issue issue = issueFactory.getIssue(d);
		writeIssue(issue, data);
	}

	protected abstract void writeIssue(Issue issue, List<IssueInfo> data);
	
	@Override
	protected FieldSelector getFieldSelector() {
		return null;
	}
}