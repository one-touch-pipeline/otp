<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
<title><g:message code="configureAlignment.title" args="[projectName]"/></title>
<asset:javascript src="pages/configureAlignment/index/configureAlignment.js"/>
</head>
<body>
    <div class="body">
        <g:if test="${hasErrors}">
            <div class="errors"> <li>${message}</li></div>
        </g:if>
        <g:elseif test="${message}">
            <div class="message">${message}</div>
        </g:elseif>
        <g:else>
            <div class="empty"><br></div>
        </g:else>
        <g:form controller="ConfigureAlignment" action="index" params='[projectName: projectName]'>
            <table>
                <tr>
                    <td></td>
                    <td class="alignment"></td>
                    <td>Default values</td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="configureAlignment.decider"/></td>
                    <td><g:select name="decider" from="${deciders}" value="${decider}"/></td>
                    <td>PanCan Alignment</td>
                </tr>
                <tr class="default">
                    <td class="myKey"><g:message code="configureAlignment.genome"/></td>
                    <td><g:select class="genome" name="referenceGenome" from="${referenceGenomes}" value="${referenceGenome}"/></td>
                    <td>hs37d5</td>
                </tr>
                <tr class="pancan">
                    <td class="myKey"><g:message code="configureAlignment.plugin"/></td>
                    <td><g:textField name="plugin" value="${plugin}"/></td>
                    <td>1.0.182</td>
                </tr>
                <tr class="pancan">
                    <td class="myKey"><g:message code="configureAlignment.statSizeFileName"/></td>
                    <td><select id="statSizeFileNames" name="statSizeFileName"/></td>
                    <td>realChromosomes</td>
                </tr>
                <tr class="pancan">
                    <td class="myKey"><g:message code="configureAlignment.unixGroup"/></td>
                    <td><g:textField name="unixGroup" value="${unixGroup}"/></td>
                    <td>GROUP</td>
                </tr>
                <tr class="pancan">
                    <td class="myKey"><g:message code="configureAlignment.mergeTool"/></td>
                    <td><g:select name="mergeTool" from="${mergeTools}" value="${mergeTool}"/></td>
                    <td>Picard</td>
                </tr>
                <tr class="pancan">
                    <td class="myKey"><g:message code="configureAlignment.config"/></td>
                    <td><g:textField name="config" value="${config}"/></td>
                    <td>-</td>
                </tr>
                <tr>
                    <td class="myKey"></td>
                    <td><g:submitButton name="submit" value="Submit"/></td>
                </tr>
            </table>
        </g:form>
    </div>
    <asset:script>
        $(function() {
            $.otp.configureAlignment.register();
        });
    </asset:script>
</body>
</html>