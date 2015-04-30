#!/bin/bash

#
#--------------------------------------------------------------------------
# Curl Request to Update JIRA for the auto deployment time tracking
#--------------------------------------------------------------------------
#
# Author: Hong Fa
#
#
#


# Constructing and sending Curl request
declare "curlDataParam={\"fields\": {\"customfield_11515\":\"finished_deploy,${bamboo_deploy_env}\"}}"
declare "curlURL=http://10.208.78.38:5010/rest/api/2/issue/${bamboo_jira_issue_no}"

curl -i -H "Authorization: Basic amlyYV9zZXJ2aWNlX3VzZXI6dGVzdDEyMzQ" -X PUT --data "${curlDataParam}" -H "Content-Type: application/json" "${curlURL}"

# Exit 0 because we don't care if this fails
exit 0
