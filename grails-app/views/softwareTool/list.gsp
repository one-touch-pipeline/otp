<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <title><g:message code="softwareTool.list.title"/></title>
        <r:require module="editorSwitch"/>
    </head>
<body>
    <div class="body_grow">
        <div class="tableBlock">
            <table>
                <thead>
                    <tr>
                        <th><div class="software-tool-table-header"><g:message code="softwareTool.list.tool"/></div></th>
                        <th><div class="software-tool-table-header"><g:message code="softwareTool.list.version"/></div></th>
                        <th><div class="software-tool-table-header"><g:message code="softwareTool.list.aliases"/></div></th>
                    </tr>
                </thead>
                <tbody>
                    <g:each var="softwareTool" in="${softwareTools}">
                        <tr>
                            <td class="software-tool-program-name-label" colspan="3">${softwareTool.programName}</td>
                        </tr>
                        <g:each var="version" in="${softwareTool.versions}">
                            <tr  class="software-tool-row">
                                <td> </td>
                                <td>
                                    <div class="software-tool-version-container">
                                        <otp:editorSwitch
                                            roles="ROLE_OPERATOR"
                                            link="${g.createLink(controller: 'softwareTool', action: 'updateSoftwareTool', id: version.id)}"
                                            value="${version.programVersion}"/>
                                    </div>
                                </td>
                                <td>
                                    <g:each var="softwareToolIdentifier" in="${version.softwareToolIdentifiers}">
                                        <otp:editorSwitch
                                            roles="ROLE_OPERATOR"
                                            link="${g.createLink(controller: 'softwareTool', action: 'updateSoftwareToolIdentifier', id: softwareToolIdentifier.id)}"
                                            value="${softwareToolIdentifier.name}"/>
                                    </g:each>
                                    <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        template="newFreeTextValue"
                                        link="${g.createLink(controller: 'softwareTool', action: 'createSoftwareToolIdentifier', id: version.id)}"
                                        value=""/>
                                </td>
                            </tr>
                        </g:each>
                    </g:each>
                </tbody>
            </table>
        </div>
    </div>
</body>
<r:script>
    $(function() {
        $.otp.growBodyInit(240);
    });
</r:script>
</html>
