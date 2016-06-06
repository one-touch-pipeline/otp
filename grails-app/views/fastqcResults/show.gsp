<%@ page contentType="text/html;charset=UTF-8" %>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="fastqc.show.title" args="[pid, runName, laneId, mateNumber]"/></title>
  </head>
  <body>
    <div class="body">
      <div class="fastqc">
        <h1><g:message code="fastqc.show.fastqcReport" args="[pid, runName, laneId, mateNumber]"/></h1>
        ${raw(html)}
      </div>
    </div>
  </body>
</html>
