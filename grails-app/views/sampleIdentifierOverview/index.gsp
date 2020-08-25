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
    <title>${g.message(code: "sampleIdentifierOverview.index.title")}</title>
    <asset:javascript src="pages/sampleIdentifierOverview/index/datatable.js"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/projectSelection"/>
        <h1>${g.message(code: "sampleIdentifierOverview.index.title")}</h1>

        <g:if test="${hideSampleIdentifier}">
            <otp:annotation type="warning">
                <g:message code="sampleIdentifierOverview.index.sampleIdentifiersHidden"/>
                <sec:ifAllGranted roles="ROLE_OPERATOR">
                    <g:message code="sampleIdentifierOverview.index.sampleIdentifiersHidden.authorized"/>
                </sec:ifAllGranted>
            </otp:annotation>
        </g:if>

        <div class="otpDataTables">
            <otp:dataTable codes="${[
                    'sampleIdentifierOverview.index.pid',
                    'sampleIdentifierOverview.index.sampleType',
                    'sampleIdentifierOverview.index.seqType',
                    'sampleIdentifierOverview.index.sampleIdentifier',
            ]}" id="sampleIdentifierOverviewTable" />
        </div>
    </div>
</body>
</html>
