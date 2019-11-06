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
    <title><g:message code="individual.insert.title"/></title>
    <asset:javascript src="pages/individual/insert/addIndividual.js"/>
</head>
<body>
    <div class="body">
    <g:if test="${projects}">
        <h1><g:message code="individual.insert.title"/></h1>
        <form id="add-individual-form" method="POST">
            <div class="dialog">
                <table>
                    <tbody>
                        <tr class="prop">
                            <td valign="top" class="name">
                                <label for="pid"><g:message code="individual.insert.ichipPid"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:individual,field:'pid','errors')}">
                                <input type="text" id="pid" name="pid" />
                            </td>
                            <td>
                            </td>
                        </tr>
                        <tr class="prop">
                            <td valign="top" class="name">
                                <label for="project"><g:message code="individual.insert.project"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:individual,field:'project','errors')}">
                                <g:select name="project" from="${projects}" optionKey="id" id="project" noSelection="[null: '']" />
                            </td>
                            <td>
                            </td>
                        </tr>
                        <tr class="prop">
                            <td valign="top" class="name">
                                <label for="mockPid"><g:message code="individual.insert.projectPid"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:individual,field:'mockPid','errors')}">
                                <input type="text" id="mockPid" name="mockPid" />
                            </td>
                            <td>
                            </td>
                        </tr>
                        <tr class="prop">
                            <td valign="top" class="name">
                                <label for="mockFullName"><g:message code="individual.insert.mockFullName"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:individual,field:'mockFullName','errors')}">
                                <input type="text" id="mockFullName" name="mockFullName" />
                            </td>
                            <td>
                            </td>
                        </tr>
                        <tr class="prop">
                            <td valign="top" class="name">
                                <label for="internIdentifier"><g:message code="individual.insert.internIdentifier"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:individual,field:'internIdentifier','errors')}">
                                <input type="text" id="internIdentifier" name="internIdentifier" />
                            </td>
                            <td valign="top" class="optional">
                                <g:message code="otp.optional"/>
                            </td>
                        </tr>
                        <tr class="prop">
                            <td valign="top" class="name">
                                <label for="individualType"><g:message code="individual.insert.individualType"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:individual,field:'individualType','errors')}">
                                <g:select name="type" from="${individualTypes}" id="individualType" />
                            </td>
                            <td>
                            </td>
                        </tr>
                        <tr class="sample">
                            <td valign="top" class="name">
                                <label for="sample"><g:message code="individual.insert.sample"/></label>
                            </td>
                            <td valign="top" class="sampleType">
                                <g:select name="sampleType" from="${sampleTypes}" class="dropDown" noSelection="[(null): '']" />
                            </td>
                            <td>
                            </td>
                        </tr>
                        <tr class="sampleIdentifier">
                            <td valign="top" class="name">
                                <label for="sampleIdentifier"><g:message code="individual.insert.sampleIdentifier"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:individual,field:'sampleIdentifier','errors')}">
                                <input type="text" name="sampleIdentifier" class="newSampleIdentifier" />
                                <div class="newSampleIdentifier">
                                    <button class="buttons"><g:message code="individual.insert.newSampleIdentifier"/></button>
                                </div>
                            </td>
                            <td>
                            </td>
                        </tr>
                        <tr class="sample hidden">
                            <td valign="top" class="name">
                                <label for="sample"><g:message code="individual.insert.sample"/></label>
                            </td>
                            <td valign="top" class="sampleType">
                                <g:select name="sampleType" from="${sampleTypes}" class="dropDown" noSelection="[null: '']" />
                            </td>
                            <td>
                            </td>
                        </tr>
                        <tr class="sampleIdentifier hidden">
                            <td valign="top" class="name">
                                <label for="sampleIdentifier"><g:message code="individual.insert.sampleIdentifier"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:individual,field:'sampleIdentifier','errors')}">
                                <input type="text" name="sampleIdentifier" />
                                <div class="newSampleIdentifier">
                                    <button class="buttons"><g:message code="individual.insert.newSampleIdentifier"/></button>
                                </div>
                            </td>
                            <td>
                            </td>
                        </tr>
                        <tr class="newSample">
                            <td>
                                <div class="addSample"><button class="buttons"><g:message code="individual.insert.newSample"/></button></div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div class="buttons">
                <input type="submit" value="${g.message(code: 'individual.insert.save')}"/>
            </div>
        </form>
    <asset:script>
        $(function() {
            $.otp.addIndividual.register();
        });
    </asset:script>
    </g:if>
    <g:else>
        <g:render template="/templates/noProject"/>
    </g:else>
    </div>
</body>
</html>
