<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Run Registration</title>
</head>
<body>
  <div class="body">
     <h1>Run registration</h1>

    <g:form controller="runSubmit" action="submit">
    <table>
        <tr>
            <td class="myKey">run name</td>
            <td><g:textField name="runName"size="30"/></td>
        </tr>
        <tr>
            <td class="myKey">sequencing center</td>
            <td><g:select name="seqCenter" from="${centers}" /></td>
        </tr>
        <tr>
            <td class="myKey">sequencing platform</td>
            <td><g:select name="seqPlatform" from="${seqPlatform}" /></td>
        </tr>
        <tr>
            <td class="myKey">data location</td>
            <td><g:textField name="dataPath" size="80" /></td>
        </tr>
        <tr>
             <td class="myKey">format of the initial data</td>
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