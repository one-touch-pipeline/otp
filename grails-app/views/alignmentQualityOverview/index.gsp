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
<title><g:message code="alignment.quality.title" args="${[project, seqType]}"/></title>
    <asset:javascript src="pages/alignmentQualityOverview/index/dataTable.js"/>
    <asset:javascript src="pages/alignmentQualityOverview/index/alignmentQualityOverview.js"/>
</head>

<body>
    <div class="body">
    <g:render template="/templates/messages"/>

    <g:if test="${projects}">
        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
        <g:if test="${seqType}">
            <div class="blue_label" id="projectsGroupbox">
                <span class="blue_label"><g:message code="alignment.quality.seqType"/> :</span>
                <form style="display: inline">
                    <g:select class="criteria" id="seqType" name='seqType'
                              from='${seqTypes}' value='${seqType.id}' optionKey='id' optionValue='displayNameWithLibraryLayout' onChange='submit();'/>
                </form>
            </div>
        </g:if>
        <div class="otpDataTables alignmentQualityOverviewTable">
            <otp:dataTable
                codes="${header}"
                id="overviewTableProcessedMergedBMF"/>
        </div>
    <asset:script type="text/javascript">
        $(function() {
            $.otp.alignmentQualityOverviewTable.register();
        });
    </asset:script>
    </g:if>
    <g:else>
        <g:render template="/templates/noProject"/>
    </g:else>
    </div>
</body>
</html>
