import groovy.json.JsonSlurper
import java.net.*;
import java.io.*;
import groovy.io.FileType

// Importing JIRA libraries to access fields
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.ComponentManager
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.component.ComponentAccessor

// Importing ModifiedValue & DefaultIsseuChangeHolder class to update fields via the script
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder

import com.opensymphony.workflow.WorkflowContext
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem
import com.atlassian.jira.issue.history.ChangeItemBean
import com.atlassian.jira.issue.util.IssueUpdateBean
import com.atlassian.jira.event.type.EventType
import com.atlassian.jira.event.issue.IssueEventSource
import com.atlassian.core.util.map.EasyMap
import com.atlassian.jira.issue.history.ChangeLogUtils
import com.atlassian.jira.security.JiraAuthenticationContext


// Logging
import org.apache.log4j.Category

import groovy.json.JsonSlurper

log = Category.getInstance("Jira Update Deployment Status Post Action")

final CONFIGURATION_FILE = "/opt/atlassian/jira/scripts/conf/deployment_bambooplan.json"

/*
* Fetch all the configurations for the current issue type
*/
def getConfigurationsForCurIssueType(String fileName, String jiraIssueType) {
    def inputFile = new File(fileName)
    def inputJSON = new JsonSlurper().parseText(inputFile.text)    
    return inputJSON[jiraIssueType]
}

def getCustomField(String fieldName) {
    ComponentManager componentManager = ComponentManager.getInstance()
    CustomFieldManager customFieldManager = componentManager.getCustomFieldManager()
    CustomField temp = customFieldManager.getCustomFieldObjectByName(fieldName)
    def value = issue.getCustomFieldValue(temp).toString()
    return value
}

def setCustomField(String fieldName, String value, curUser = null) {
    ComponentManager componentManager = ComponentManager.getInstance()
    CustomFieldManager customFieldManager = componentManager.getCustomFieldManager()
    def tgtField = customFieldManager.getCustomFieldObjectByName(fieldName)
    FieldLayoutItem fieldLayoutItem = null;
    try {
        fieldLayoutItem = ComponentManager.getInstance().getFieldLayoutManager().getFieldLayout(issue).getFieldLayoutItem(tgtField)
    } catch (e) {
        println e
    } 
    def changeHolder = new DefaultIssueChangeHolder();    
    def modifiedValue = new ModifiedValue(tgtField.getValue(issue), value)
    def changeItemBeans = new ChangeItemBean(ChangeItemBean.CUSTOM_FIELD, fieldName, modifiedValue.getOldValue().toString(), modifiedValue.getNewValue().toString())
    changeHolder.addChangeItem(changeItemBeans)
    issue.setCustomFieldValue(tgtField, modifiedValue.getNewValue())
}



StringWriter outputLogWriter = new StringWriter();
def collectLogging(String newLog, outputLogWriter) {
    String newLine = System.getProperty("line.separator");
    outputLogWriter << newLog
    outputLogWriter << newLine
}

def getCollectedLog(outputLogWriter) {
    return outputLogWriter.toString()    
}

def addComments(curIssure, curUser, String content) {
    ComponentManager componentManager = ComponentManager.getInstance()
    CommentManager commentMgr = componentManager.getCommentManager()
    commentMgr = (CommentManager) componentManager.getComponentInstanceOfType(CommentManager.class)
    commentMgr.create(curIssure, curUser, content, false)
}

//Actual Start Point
String jiraIssueKey = issue.getKey()
String jiraIssueType = issue.getIssueTypeObject().getName()
String currentUser = ((WorkflowContext) transientVars.get("context")).getCaller();

try {

    log.debug("-- current working on JIRA issue: $jiraIssueKey with type: $jiraIssueType")
    def configurations = getConfigurationsForCurIssueType(CONFIGURATION_FILE, jiraIssueType)    
    

    /*
    *Try to update cutom filed to recall all the release deployments
    *The format will be like:
    * <action>,<environment>,<component>:<version>
    *
    * e.g. start_deploy,DEV2,Dimitis_linkmanager:p16.09
    */

    def isSuccess = getCustomField(configurations.customFields.deploymentResultFeild)
    String action = isSuccess.equalsIgnoreCase("Successful") ? "success_deploy" : "fail_deploy"
    def depEnvironment = getCustomField(configurations.customFields.deployEnviornmentField)
   
    String deploymentTag = "$action,$depEnvironment"
    setCustomField("_deployment_tracker", deploymentTag, currentUser)

    collectLogging("#AutoBuild -- Successfully updated deployment tracker, $deploymentTag", outputLogWriter)

} catch (ex) {
    collectLogging("#AutoBuild -- Error Occurred", outputLogWriter)
    StringWriter sw = new StringWriter();
    ex.printStackTrace(new PrintWriter(sw));
    String stacktrace = sw.toString();
    collectLogging(stacktrace, outputLogWriter)    
} finally {
    addComments(issue, currentUser, getCollectedLog(outputLogWriter))
}