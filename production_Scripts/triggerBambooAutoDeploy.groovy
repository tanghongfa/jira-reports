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

// Logging
import org.apache.log4j.Category

import groovy.json.JsonSlurper

log = Category.getInstance("JiraDeployPostAction")
log.warn ("--- Deploy Action ---")

final JIRA_STASH_COMMIT_USER_NAME = "jira"
final JIRA_STASH_COMMIT_USER_EMAIL = "jira@protus.crop.telstra.com.au"
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
    log.warn("-- get field: $fieldName ,value: $value")
    return value
}

def setCustomField(String fieldName, String value) {
    ComponentManager componentManager = ComponentManager.getInstance()
    CustomFieldManager customFieldManager = componentManager.getCustomFieldManager()
    def tgtField = customFieldManager.getCustomFieldObjectByName(fieldName)
    def changeHolder = new DefaultIssueChangeHolder()
    def oldValue = issue.getCustomFieldValue(tgtField)
    issue.setCustomFieldValue(tgtField, value)
    tgtField.updateValue(null, issue, new ModifiedValue(oldValue, value),changeHolder);
    log.warn("-- update field: $fieldName ,value: $value")
}


def triggerBambooPlan(String authUser, String authPassword, String planRestUrl, String jiraIssueNo, String deployEnv) {
    def request = "curl -X POST --user $authUser:$authPassword -d bamboo.variable.jira.issue.no=$jiraIssueNo -d bamboo.variable.deploy.env=$deployEnv $planRestUrl" + ".json"
    log.warn("-- sending curl command: $request")
    def proc = request.execute();
    proc.waitFor();
    println proc.text;
}

//Actual Start Point
String jiraIssueKey = issue.getKey()
String jiraIssueType = issue.getIssueTypeObject().getName()
String currentUser = ((WorkflowContext) transientVars.get("context")).getCaller();

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

try {
    log.warn("-- current working on JIRA issue: $jiraIssueKey with type: $jiraIssueType")

    def configurations = getConfigurationsForCurIssueType(CONFIGURATION_FILE, jiraIssueType)
    
    String deployEnv = getCustomField(configurations.customFields.deployEnviornmentField)
    def isRollback = getCustomField(configurations.customFields.isRollbackIndicationField)
    
    // Start Bamboo plan
    if(isRollback == "true") {
        log.warn("---triggering rollback plan---")
        triggerBambooPlan(configurations.bambooPuppetAutoRollbackPlan.authUser, 
            configurations.bambooPuppetAutoRollbackPlan.authPwd, 
            configurations.bambooPuppetAutoRollbackPlan.baseUrl + configurations.bambooPuppetAutoRollbackPlan.environmentToPlanIdMapping[deployEnv], jiraIssueKey, deployEnv)
    } else {
        log.warn("---triggering deployment plan---")
        triggerBambooPlan(configurations.bambooPuppetAutoDeployPlan.authUser, 
            configurations.bambooPuppetAutoDeployPlan.authPwd, 
            configurations.bambooPuppetAutoDeployPlan.baseUrl + configurations.bambooPuppetAutoDeployPlan.environmentToPlanIdMapping[deployEnv], jiraIssueKey, deployEnv)
    }

    collectLogging("#AutoDeploy -- Succefully trigged Bamboo Auto Deployment Plan", outputLogWriter)

} catch (ex) {
    collectLogging("#AutoDeploy -- Error Occurred", outputLogWriter)
    StringWriter sw = new StringWriter();
    ex.printStackTrace(new PrintWriter(sw));
    String stacktrace = sw.toString();
    collectLogging(stacktrace, outputLogWriter)    
} finally {
    addComments(issue, currentUser, getCollectedLog(outputLogWriter))
}
