<%@ page import="de.dkfz.tbi.otp.ngsdata.IndividualController" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="individual.insertMany.title"/></title>
</head>

<body>
<div class="body" id="bulk-sample-creation">
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
</div>
</body>
</html>
