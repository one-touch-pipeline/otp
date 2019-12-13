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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <title><g:message code="softwareTool.list.title"/></title>
        <asset:javascript src="modules/editorSwitch"/>
    </head>
<body>
    <div class="body metaDataFields">
        <g:render template="/templates/messages"/>
        <g:render template="/metaDataFields/linkBanner"/>

        <h3><g:message code="softwareTool.list.header"/></h3>
        <g:form controller="softwareTool" action="createSoftwareTool" method="POST">
            <table style="width: 50%">
                <tbody>
                    <tr>
                        <td><g:message code="softwareTool.list.tool"/></td>
                        <td><input name="programName" id="type" type="text" value="${cmd?.programName}"/></td>
                    </tr>
                    <tr>
                        <td><g:message code="softwareTool.list.version"/></td>
                        <td><input name="programVersion" id="version" type="text" value="${cmd?.programVersion}"></td>
                    </tr>
                </tbody>
            </table>
            <g:submitButton name="${g.message(code: "softwareTool.list.confirmation")}"/>
        </g:form>
        <br>
        <span class="annotation"><g:message code="dataFields.title.caseInsensitive"/></span>
        <table class="software-table">
            <thead>
                <tr>
                    <th><g:message code="softwareTool.list.version"/></th>
                    <th><g:message code="softwareTool.list.aliases"/></th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
            <g:each var="programName" in="${(softwareToolPerProgramName.keySet() as List<String>).sort { it.toLowerCase() }}">
                <tr>
                    <td colspan="3" class="software-table-name-row">${programName}</td>
                </tr>
                <g:each var="softwareTool" in="${softwareToolPerProgramName[programName]}">
                     <tr>
                         <td>
                             <otp:editorSwitch
                                     roles="ROLE_OPERATOR"
                                     link="${g.createLink(controller: 'softwareTool', action: 'updateSoftwareTool', id: softwareTool.id)}"
                                     value="${softwareTool.programVersion}"/>
                         </td>
                         <td>
                             <g:each var="identifier" in="${identifierPerSoftwareTool[softwareTool]}">
                                 <otp:editorSwitch
                                         roles="ROLE_OPERATOR"
                                         link="${g.createLink(controller: 'softwareTool', action: 'updateSoftwareToolIdentifier', id: identifier.id)}"
                                         value="${identifier.name} "/>
                             </g:each>
                         </td>
                         <td>
                             <otp:editorSwitch
                                     roles="ROLE_OPERATOR"
                                     template="newFreeTextValue"
                                     link="${g.createLink(controller: 'softwareTool', action: 'createSoftwareToolIdentifier', id: softwareTool.id)}"
                                     value=""/>
                         </td>
                     </tr>
                </g:each>
            </g:each>
            </tbody>
        </table>
    </div>
</body>
</html>
