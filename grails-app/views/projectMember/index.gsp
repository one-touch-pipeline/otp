<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="projectMember.title" args="[project?.name]"/></title>
    <asset:javascript src="pages/projectMember/index/functions.js"/>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
    <div class="body">
    <g:if test="${projects}">
        <g:if test="${hasErrors == true}">
            <div class="errors"> <li>${message}</li></div>
        </g:if>
        <g:elseif test="${message}">
            <div class="message">${message}</div>
        </g:elseif>

        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />

        <div class="otpDataTables">
            <h3>
                <g:message code="projectOverview.contactPerson.headline" />
            </h3>
            <table>
                <tr>
                    <th><g:message code="projectOverview.contactPerson.name"/></th>
                    <th><g:message code="projectOverview.contactPerson.email"/></th>
                    <th><g:message code="projectOverview.contactPerson.aspera"/></th>
                    <th><g:message code="projectOverview.contactPerson.role"/></th>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <th></th>
                    </sec:ifAllGranted>
                </tr>
                <g:each in="${projectContactPersons}" var="projectContactPerson">
                    <tr>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectMember', action: 'updateName', params: ["contactPerson.id": projectContactPerson.contactPerson.id])}"
                                    value="${projectContactPerson.contactPerson.fullName}"
                                    values="${projectContactPersons}"/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectMember', action: 'updateEmail', params: ["contactPerson.id": projectContactPerson.contactPerson.id])}"
                                    value="${projectContactPerson.contactPerson.email}"
                                    values="${projectContactPersons}"/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectMember', action: 'updateAspera', params: ["contactPerson.id": projectContactPerson.contactPerson.id])}"
                                    value="${projectContactPerson.contactPerson.aspera}"
                                    values="${projectContactPersons}"/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="dropDown"
                                    link="${g.createLink(controller: 'projectMember', action: 'updateRole', params: ["projectContactPerson.id": projectContactPerson.id])}"
                                    value="${projectContactPerson.contactPersonRole?.name ?: ''} "
                                    values="${roleDropDown}"/>
                        </td>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <td>
                                <input type="button" class="deletePerson" value="Delete" data-id="${projectContactPerson.id}"/>
                            </td>
                        </sec:ifAllGranted>
                    </tr>
                </g:each>
            </table>
        </div>
        <p>
            <otp:editorSwitchNewValues
                    roles="ROLE_OPERATOR"
                    labels="${["Name", "E-Mail", "Aspera Account", "Role"]}"
                    textFields="${["name", "email", "aspera"]}"
                    dropDowns="${[role: roleDropDown]}"
                    link="${g.createLink(controller: 'projectMember', action: "createContactPersonOrAddToProject", params: ['project.id': project.id])}"
            />
        </p>
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <br>
            <div>
                <h3><g:message code="projectOverview.accessPerson.headline" /></h3>
                <a id="toggleLink" href="javascript:void(0)" onclick="$.otp.projectMember.toggle('controlElement', 'toggleLink')">Show list</a>
                <ul id="controlElement" style="display: none">
                    <g:each in="${accessPersons}">
                        <li>
                            ${it}
                        </li>
                    </g:each>
                </ul>
            </div>
        </sec:ifAllGranted>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
    <asset:script>
        $(function() {
            $.otp.projectMember.deleteUser();
        });
    </asset:script>
</body>
</html>
