<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Insert title here</title>
</head>
<body>
  <div class="body">
    <g:uploadForm action="upload">
        <g:textField name="identifier" />
        <input name="fileText" type="file" />
        <g:submitButton name="upload" value="Upload" />
    </g:uploadForm>
  </div>
</body>
</html>