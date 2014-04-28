<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="runSubmit.title"/></title>
</head>
<body>
  <div class="body">
    <g:form controller="runSubmit" action="submit">
    <table class="runSubmit_table">
        <tr>
            <td class="myKey"><g:message code="runSubmit.runName"/></td>
            <td><g:textField name="runName"size="100"/></td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="runSubmit.sequencingCenter"/></td>
            <td><g:select name="seqCenter" from="${centers}" /></td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="runSubmit.sequencingPlatform"/></td>
            <td><g:select name="seqPlatform" from="${seqPlatform}" /></td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="runSubmit.dataLocation"/></td>
            <td><g:textField name="dataPath" size="120" /></td>
        </tr>
        <tr>
             <td class="myKey"><g:message code="runSubmit.formatInitialData"/></td>
             <td><g:select name="initialFormat" from="${de.dkfz.tbi.otp.ngsdata.RunSegment.DataFormat}"/></td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="runSubmit.align"/></td>
            <td><g:checkBox name="align" checked="true" /></td>
        </tr>
        <tr>
            <td></td><td><g:submitButton name="submit" value="Submit" ondblclick="return false;"/></td>
        </tr>

    </table>
    </g:form>
  </div>
</body>
</html>
