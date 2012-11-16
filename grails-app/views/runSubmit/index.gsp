<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="runSubmit.title"/></title>
</head>
<body>
  <div class="body">
     <h1><g:message code="runSubmit.title"/></h1>

    <g:form controller="runSubmit" action="submit">
    <table>
        <tr>
            <td class="myKey"><g:message code="runSubmit.runName"/></td>
            <td><g:textField name="runName"size="30"/></td>
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
            <td><g:textField name="dataPath" size="80" /></td>
        </tr>
        <tr>
             <td class="myKey"><g:message code="runSubmit.formatInitialData"/></td>
             <td><g:select name="initialFormat" from="${de.dkfz.tbi.otp.ngsdata.RunSegment.DataFormat}"/></td>
        </tr>
        <tr>
            <td></td><td><g:submitButton name="submit" value="Submit" /></td>
        </tr>

    </table>
    </g:form>
  </div>
</body>
</html>