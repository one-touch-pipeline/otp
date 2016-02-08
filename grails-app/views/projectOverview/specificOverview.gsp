<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="projectOverview.title" args="[project]"/></title>
    <asset:javascript src="modules/graph"/>
    <asset:javascript src="pages/projectOverview/index/datatable.js"/>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
    <div class="body">
        <form class="blue_label" id="projectsGroupbox">
            <span class="blue_label"><g:message code="home.projectfilter"/> :</span>
            <g:select class="criteria" id="project" name='project'
                from='${projects}' value='${project}' onChange='submit();'></g:select>
        </form>
        <div id="projectOverviewDates">
            <table>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.creationDate"/></td>
                    <td id="creation-date"></td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.lastDate"/></td>
                    <td id="last-received-date"></td>
                </tr>
            </table>
        </div>
        <div class="otpDataTables">
            <h3 class="statisticTitle">
                <g:message code="projectOverview.contactPerson.headline" />
            </h3>
            <otp:dataTable
                codes="${[
                        'projectOverview.contactPerson.name',
                        'otp.blank',
                        'projectOverview.contactPerson.email',
                        'otp.blank',
                        'projectOverview.contactPerson.aspera',
                        'otp.blank',
                        'otp.blank'
                    ] }"
                    id="listContactPerson" />
        </div>
        <p>
            <otp:editorSwitch roles="ROLE_OPERATOR"
                    template="newFreeTextValues"
                    fields="${["Name","E-Mail","Aspera"]}"
                    link="${g.createLink(controller: "projectOverview", action: "createContactPersonOrAddProject", id: project)}"
                    value=""/>
        </p>
    </div>
    <asset:script>
        $(function() {
            $.otp.projectOverviewTable.specificOverview();
        });
    </asset:script>
</body>
</html>
