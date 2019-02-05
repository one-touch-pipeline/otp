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
                                            value="${version.programVersion} "/>
                                    </div>
                                </td>
                                <td>
                                    <g:each var="softwareToolIdentifier" in="${version.softwareToolIdentifiers}">
                                        <otp:editorSwitch
                                            roles="ROLE_OPERATOR"
                                            link="${g.createLink(controller: 'softwareTool', action: 'updateSoftwareToolIdentifier', id: softwareToolIdentifier.id)}"
                                            value="${softwareToolIdentifier.name} "/>
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
<asset:script type="text/javascript">
    $(function() {
        $.otp.growBodyInit(240);
    });
</asset:script>
</html>
