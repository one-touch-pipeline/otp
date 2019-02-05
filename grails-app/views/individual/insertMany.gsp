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

<%@ page import="de.dkfz.tbi.otp.ngsdata.IndividualController" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="individual.insertMany.title"/></title>
</head>

<body>
<div class="body" id="bulk-sample-creation">
    <g:if test="${projects}">
    <h1><g:message code="individual.insertMany.title"/></h1>

    <p>
        <g:message code="individual.insertMany.description"/>
    </p>

    <g:if test="${messageType == 'error'}">
        <asset:script>
            $.otp.warningMessage('${message}')
        </asset:script>
    </g:if>
    <g:elseif test="${messageType == 'info'}">
        <asset:script>
            $.otp.infoMessage('${message}')
        </asset:script>
    </g:elseif>
    <g:uploadForm action="insertMany">
        <div class="dialog">
            <table>
                <tbody>
                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="project">
                            <g:message code="individual.insert.project"/>
                        </label>
                    </td>
                    <td valign="top" class="value">
                        <g:select name="project" from="${projects}" id="project" value="${oldProject}"
                                  noSelection="['': '']"/>
                    </td>
                    <td>
                    </td>
                </tr>
                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="delimiter">
                            <g:message code="individual.insertMany.delimiter"/>
                        </label>
                    </td>
                    <td valign="top" class="value">
                        <g:select name="delimiter" from="${delimiters}" id="delimiter" value="${oldDelimiter}"
                                  noSelection="['': '']"/>
                    </td>
                    <td>
                    </td>
                </tr>
                <tr><td colspan="3">&nbsp;</td></tr>
                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="sampleFile">
                            <g:message
                                    code="individual.insertMany.file.upload"/>
                        </label>
                    </td>
                    <td valign="top" class="value">
                        <input type="file" name="sampleFile" id="sampleFile"/>
                    </td>
                    <td>
                        <g:message code='individual.insertMany.file.upload.info'
                                   args="${[IndividualController.PID, IndividualController.SAMPLE_TYPE, IndividualController.SAMPLE_IDENTIFIER]}"/>
                    </td>
                </tr>
                <tr><td colspan="3">&nbsp;</td></tr>
                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="sampleText">
                            <g:message code="individual.insertMany.text.upload"/>
                        </label>
                    </td>
                    <td valign="top" class="value">
                        <g:textArea name="sampleText" id="sampleText" style="min-width: 500px;"
                                    value="${oldContent}"/>
                    </td>
                    <td>
                        <g:message code='individual.insertMany.text.upload.info'
                                   args="${[IndividualController.PID, IndividualController.SAMPLE_TYPE, IndividualController.SAMPLE_IDENTIFIER]}"/>
                        <p><pre><g:message code='individual.insertMany.text.upload.example'
                                           args="${[IndividualController.PID, IndividualController.SAMPLE_TYPE, IndividualController.SAMPLE_IDENTIFIER]}"/></pre></p>
                    </td>
                </tr>
                </tbody>
            </table>
            <g:submitButton name="submit"/>
        </div>
    </g:uploadForm>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
</div>
</body>
</html>
