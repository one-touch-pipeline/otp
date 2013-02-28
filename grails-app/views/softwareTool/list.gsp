<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <title><g:message code="softwareTool.list.title"/></title>
        <r:require module="editorSwitch"/>
    </head>
<body>
  <div class="body">
    <h1 id="softwareTool" style="display:inline-block"><g:message code="softwareTool.list.title"/></h1>
    <div class="tableBlock">
      <table>
        <thead>
            <tr>
                <th><g:message code="softwareTool.list.tool"/></th>
                <th><g:message code="softwareTool.list.version"/></th>
                <th><g:message code="softwareTool.list.aliases"/></th>
            </tr>
        </thead>

        <tbody>
            <g:each var="softwareTool" in="${softwareTools}">
                <tr>
                    <td colspan="3">${softwareTool.programName}</td>
                </tr>
                <g:each var="version" in="${softwareTool.versions}">
                    <tr>
                        <td> </td>
                        <td>
                            <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'softwareTool', action: 'updateSoftwareTool', id: version.id)}"
                                value="${version.programVersion}"/>
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
</html>
