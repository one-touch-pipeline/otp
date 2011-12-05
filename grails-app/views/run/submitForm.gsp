<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Insert title here</title>
</head>
<body>
  <div class="body">
    
    <h1>Submitting a run</h1>

    <g:uploadForm controller="run" action="submit">
    <table>
        <tr>
            <td class="myKey">run name</td>
            <td><g:textField name="runName"size="30"/></td>
        </tr>
        <tr>
            <td class="myKey">sequencing center</td>
            <td><g:select name="center" from="${centers}" /></td>
        </tr>
        <tr>
            <td class="myKey">sequencing platform</td>
            <td><g:select name="seqTech" from="${seqTechs}" /></td>
        </tr>
        <tr>
            <td class="myKey">data location</td>
            <td><g:textField name="dataPath"size="80" /></td>
        </tr>
        <tr>
            <td class="myKey">meta-data location</td>
            <td><g:textField name="mdPath" size="80" /></td>
        </tr>
        <tr>
            <td></td>
            <td><g:submitButton name="submit" value="Submit" /></td>
        </tr>

    </table>
    </g:uploadForm>

  </div>
</body>
</html>