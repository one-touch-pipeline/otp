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

<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="projectProgress.progress.title" /></title>
    <asset:javascript src="pages/projectProgress/progress/progress.js"/>
</head>
<body>
  <div class="body">
    <form class="rounded-page-header-box">
        <span class="blue_label">
            <g:message code="search.from.date"/>:<input type="text" class="datePicker" id="startDate" value="${startDate}">
        </span>
        <br>
        <span class="blue_label">
            <g:message code="search.to.date"/>:<input type="text" class="datePicker" id="endDate" value="${endDate}">
        </span>
        <g:select class="projectSelectMultiple blue_label"
            name="projects"
            value="${availableProjects}"
            from="${availableProjects}"
            optionKey="name"
            multiple="true"
        />

        <input id="display" type="button" class="blue_label" name="progress" value=" Display "/>
    </form>
  <div class="otpDataTables" >
        <otp:dataTable codes="${[
                    'projectProgress.progress.runs',
                    'projectProgress.progress.center',
                    'projectProgress.progress.samples', ]}"
                    id="progressId"/>
</div>
</div>

<asset:script type="text/javascript">
        $(function() {
            $.otp.projectProgressTable.registerProjectProgressId();
    });
</asset:script>
</body>
</html>
