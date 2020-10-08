%{--
  - Copyright 2011-2019 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%

<%@ page import="de.dkfz.tbi.otp.job.plan.JobErrorDefinition" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="job.error.definition.header"/></title>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>
<body>
<div class="body">
    <h1><g:message code="job.error.definition.header"/></h1>
    <form class="rounded-page-header-box">
        <span><g:message code="job.error.definition.jobDefinitionFilter"/>:</span>
        <g:select class="use-select-2" id="job_select" name='job'
                  from='${jobDefinitions}' value='${jobDefinition}' onChange='submit();' />
    </form>
    <g:each var="firstContent" in="${jobErrorDefinition}">
        <table>
            <thead>
            <tr>
                <th width="10%"><g:message code="job.error.definition.table.type"/></th>
                <th width="12%"><g:message code="job.error.definition.table.action"/></th>
                <th width="35%"><g:message code="job.error.definition.table.jobs"/></th>
                <th width="43%"><g:message code="job.error.definition.table.errorExpression"/></th>
            </tr>
            </thead>
            <tbody>

            <!--FIRST LEVEL-->
            <tr>
                <td>${firstContent.key.type}</td>
                <td>${firstContent.key.action}
                    <g:if test="${firstContent.key.action == JobErrorDefinition.Action.CHECK_FURTHER}">
                        <br>
                        <otp:editorSwitchNewValues
                                roles="ROLE_ADMIN"
                                labels="${["Error Expression", "Type", "Action"]}"
                                textFields="${["errorExpression"]}"
                                dropDowns="${[typeSelect: typeDropDown, actionSelect: actionDropDown]}"
                                link="${g.createLink(controller: "jobErrorDefinition", action: "addJobErrorDefinition", params: ["basedJobErrorDefinition.id": firstContent.key.id, "level": "second level"])}"
                        />
                    </g:if>
                </td>
                <td><g:each var="jobs" in="${firstContent.key.jobDefinitions.collect {"${it.name} - ${it.plan.name} ${it.plan.obsoleted ? ' - obsoleted' : ''}"}.sort()}">
                    ${jobs}<br/>
                    </g:each>
                    <otp:editorSwitchNewValues
                            roles="ROLE_ADMIN"
                            labels="${["Job"]}"
                            dropDowns="${[jobDefinitionString: allJobDefinition]}"
                            link="${g.createLink(controller: 'jobErrorDefinition', action: 'addNewJob', params: ["jobErrorDefinition.id": firstContent.key.id])}"
                    />
                </td>
                <td><otp:editorSwitch
                        roles="ROLE_ADMIN"
                        template="textArea"
                        link="${g.createLink(controller: 'jobErrorDefinition', action: 'updateErrorExpression', params: ["jobErrorDefinition.id": firstContent.key.id])}"
                        value="${firstContent.key.errorExpression}"/></td>
            </tr>

            <!--SECOND LEVEL-->
            <g:if test="${firstContent.value instanceof java.util.Map}">
                <g:each var="secondContent" in="${firstContent.value}">
                    <tr style="text-indent:15px;">
                        <td>${secondContent.key.type}</td>
                        <td>${secondContent.key.action}
                            <g:if test="${secondContent.key.action == JobErrorDefinition.Action.CHECK_FURTHER}">
                                <br>
                                <otp:editorSwitchNewValues
                                        roles="ROLE_ADMIN"
                                        labels="${["Error Expression", "Type", "Action"]}"
                                        textFields="${["errorExpression"]}"
                                        dropDowns="${[typeSelect: typeDropDown, actionSelect: actionDropDown]}"
                                        link="${g.createLink(controller: "jobErrorDefinition", action: "addJobErrorDefinition", params: ["basedJobErrorDefinition.id": secondContent.key.id, "level": "third level"])}"
                                />
                            </g:if>
                        </td>
                        <td><g:each var="jobs" in="${secondContent.key.jobDefinitions.collect {"${it.name} - ${it.plan.name} ${it.plan.obsoleted ? ' - obsoleted' : ''}"}.sort()}">
                            <div>${jobs}<br/></div>
                            </g:each>
                            <otp:editorSwitchNewValues
                                    roles="ROLE_ADMIN"
                                    labels="${["Job"]}"
                                    dropDowns="${[jobDefinitionString: allJobDefinition]}"
                                    link="${g.createLink(controller: 'jobErrorDefinition', action: 'addNewJob', params: ["jobErrorDefinition.id": firstContent.key.id])}"
                            />
                        </td>
                        <td><otp:editorSwitch
                                roles="ROLE_ADMIN"
                                template="textArea"
                                link="${g.createLink(controller: 'jobErrorDefinition', action: 'updateErrorExpression', params: ["jobErrorDefinition.id": secondContent.key.id])}"
                                value="${secondContent.key.errorExpression}"/>
                        </td>
                    </tr>

                    <!--THIRD LEVEL-->
                    <g:if test="${secondContent.value instanceof java.util.Map}" >
                        <g:each var="thirdContent" in="${secondContent.value}">
                            <tr style="text-indent:25px;">
                                <td>${thirdContent.key.type}</td>
                                <td>${thirdContent.key.action}</td>
                                <td><g:each var="jobs" in="${secondContent.key.jobDefinitions.collect {"${it.name} - ${it.plan.name} ${it.plan.obsoleted ? ' - obsoleted' : ''}"}.sort()}">
                                    <div>${jobs}<br/></div>
                                    </g:each>
                                    <otp:editorSwitchNewValues
                                            roles="ROLE_ADMIN"
                                            labels="${["Job"]}"
                                            dropDowns="${[jobDefinitionString: allJobDefinition]}"
                                            link="${g.createLink(controller: 'jobErrorDefinition', action: 'addNewJob', params: ["jobErrorDefinition.id": firstContent.key.id])}"
                                    />
                                </td>
                                <td><otp:editorSwitch
                                        roles="ROLE_ADMIN"
                                        template="textArea"
                                        link="${g.createLink(controller: 'jobErrorDefinition', action: 'updateErrorExpression', params: ["jobErrorDefinition.id": thirdContent.key.id])}"
                                        value="${thirdContent.key.errorExpression}"/></td>
                            </tr>
                        </g:each>
                    </g:if>
                </g:each>
            </g:if>
            </tbody>
        </table>
    </g:each>

    <g:message code="job.error.definition.firstLevel"/>
    <otp:editorSwitchNewValues
            roles="ROLE_ADMIN"
            labels="${["Error Expression", "Action"]}"
            textFields="${["errorExpression"]}"
            dropDowns="${[actionSelect: actionDropDown]}"
            link="${g.createLink(controller: "jobErrorDefinition", action: "addNewJobErrorDefinition")}"
    />
</div>
</body>
</html>
