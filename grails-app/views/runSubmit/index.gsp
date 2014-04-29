<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="runSubmit.title"/></title>
</head>
<body>
  <div class="body">
    <g:hasErrors bean="${cmd}">
      <div class="errors">
        ${message(code: "runSubmit.error")}
        <ul>
          <g:eachError var="error" bean="${cmd}">
            <li>${message(error: error)}</li>
          </g:eachError>
        </ul>
      </div>
    </g:hasErrors>
    <g:form controller="runSubmit" action="submit">
    <table class="runSubmit_table">
        <tr>
            <td class="myKey"><g:message code="runSubmit.runName"/></td>
            <td><g:textField name="name" size="100" value="${cmd?.name}" /></td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="runSubmit.sequencingCenter"/></td>
            <td><g:select name="seqCenter" from="${centers}" value="${cmd?.seqCenter}" /></td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="runSubmit.sequencingPlatform"/></td>
            <td><g:select name="seqPlatform" from="${seqPlatform}" value="${cmd?.seqPlatform}" /></td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="runSubmit.dataLocation"/></td>
            <td><g:textField name="dataPath" size="120" value="${cmd?.dataPath}" /></td>
        </tr>
        <tr>
             <td class="myKey"><g:message code="runSubmit.formatInitialData"/></td>
             <td><g:select name="initialFormat" from="${de.dkfz.tbi.otp.ngsdata.RunSegment.DataFormat}" value="${cmd?.initialFormat}" /></td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="runSubmit.align"/></td>
            <td><g:checkBox name="align" checked="${cmd == null || cmd.align}" /></td>
        </tr>
        <tr>
            <td></td><td><g:submitButton name="submit" value="Submit" ondblclick="return false;"/></td>
        </tr>

    </table>
    </g:form>
  </div>
</body>
</html>
