<%@ page import="de.dkfz.tbi.otp.job.plan.JobErrorDefinition" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="job.error.definition.header"/></title>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
<div class="body_grow">
    <h1><g:message code="job.error.definition.header"/></h1>
    <form class="blue_label" id="projectsGroupbox">
        <span class="blue_label"><g:message code="job.error.definition.jobDefinitionFilter"/> :</span>
        <g:select class="criteria" id="job_select" name='job'
                  from='${jobDefinitions}' value='${jobDefinition}' onChange='submit();' />
    </form>
    <g:each var="firstContent" in="${jobErrorDefinition}">
        <table>
            <thead>
            <tr>
                <th width="10%"><g:message code="job.error.definition.table.type"/></th>
                <th width="13%"><g:message code="job.error.definition.table.action"/></th>
                <th width="13%"><g:message code="job.error.definition.table.jobs"/></th>
                <th width="64%"><g:message code="job.error.definition.table.errorExpression"/></th>
            </tr>
            </thead>
            <tbody>

            <!--FIRST LEVEL-->
            <tr>
                <td>${firstContent.key.type}</td>
                <td>${firstContent.key.action}
                    <g:if test="${firstContent.key.action == JobErrorDefinition.Action.CHECK_FURTHER}">
                        <br>
                        <otp:editorSwitch
                                roles="ROLE_ADMIN"
                                template="newFreeTextValues"
                                dropDowns="${[Type: typeDropDown, Action: actionDropDown]}"
                                fields="${["Error Expression"]}"
                                link="${g.createLink(controller: "jobErrorDefinition", action: "addJobErrorDefinition", params: ["basedJobErrorDefinition.id": firstContent.key.id, "level": "second level"])}"
                                value=""/>
                    </g:if>
                </td>
                <td><g:each var="jobs" in="${firstContent.key.jobDefinitions}">
                    ${jobs.name}<br/>
                    </g:each>
                    <otp:editorSwitch
                            roles="ROLE_ADMIN"
                            template="newFreeTextValue"
                            link="${g.createLink(controller: 'jobErrorDefinition', action: 'addNewJob', params: ["jobErrorDefinition.id": firstContent.key.id])}"
                            value=""/>
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
                                <otp:editorSwitch
                                        roles="ROLE_ADMIN"
                                        template="newFreeTextValues"
                                        dropDowns="${[Type: typeDropDown, Action: actionDropDown]}"
                                        fields="${["Error Expression"]}"
                                        link="${g.createLink(controller: "jobErrorDefinition", action: "addJobErrorDefinition", params: ["basedJobErrorDefinition.id": secondContent.key.id, "level": "third level"])}"
                                        value=""/>
                            </g:if>
                        </td>
                        <td><g:each var="jobs" in="${secondContent.key.jobDefinitions.sort{it.name}}">
                            <div>${jobs.name}<br/></div>
                            </g:each>
                            <otp:editorSwitch
                                    roles="ROLE_ADMIN"
                                    template="newFreeTextValue"
                                    link="${g.createLink(controller: 'jobErrorDefinition', action: 'addNewJob', params: ["jobErrorDefinition.id": secondContent.key.id])}"
                                    value=""/>
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
                                <td><g:each var="jobs" in="${secondContent.key.jobDefinitions.sort{it.name}}">
                                    <div>${jobs.name}<br/></div>
                                    </g:each>
                                    <otp:editorSwitch
                                            roles="ROLE_ADMIN"
                                            template="newFreeTextValue"
                                            link="${g.createLink(controller: 'jobErrorDefinition', action: 'addNewJob', params: ["jobErrorDefinition.id": secondContent.key.id])}"
                                            value=""/>
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
    <otp:editorSwitch
            roles="ROLE_ADMIN"
            template="newFreeTextValues"
            dropDowns="${[Action: actionDropDown]}"
            fields="${["Error Expression"]}"
            link="${g.createLink(controller: "jobErrorDefinition", action: "addNewJobErrorDefinition")}"
            value=""/>
</div>
</body>
<asset:script>
    $(function() {
        $.otp.growBodyInit(240);
    });
</asset:script>
</html>
