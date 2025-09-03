%{--
  - Copyright 2011-2025 The OTP authors
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

<%@ page import="de.dkfz.tbi.otp.ngsdata.BulkSampleCreationController" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title><g:message code="bulk.sample.creation.title"/></title>
    <asset:javascript src="pages/bulkSampleCreation/index/bulkSampleCreation.js"/>
</head>

<body>
<div class="container-fluid otp-main-container" id="bulk-sample-creation">
    <div class="project-selection-header-container mb-4">
        <g:render template="/templates/bootstrap/projectSelection"/>
    </div>

    <h1><g:message code="bulk.sample.creation.title"/></h1>
    <otp:annotation type="info">
        <g:message code="bulk.sample.creation.description"/>
    </otp:annotation>

    <div class="mt-3">
        <table class="table table-sm table-striped table-bordered">
        <tbody>
            <tr>
                <td class="table-column-header"><g:message code="individual.insert.project"/></td>
                <td>${selectedProject}</td>
                <td><b><g:message code="bulk.sample.creation.multipleProjects"/></b></td>
            </tr>

            <tr>
                <td class="table-column-header"><g:message code="bulk.sample.creation.file.upload"/></td>
                <td>
                    <g:uploadForm action="upload" class="d-flex">
                        <input class="form-control me-2" type="file" name="content">
                        <button type="submit" class="btn btn-primary">${g.message(code: "default.button.upload.label")}</button>
                    </g:uploadForm>
                </td>
                <td>
                    <g:message code='bulk.sample.creation.file.upload.info' args="${header}"/>
                </td>
            </tr>

            <tr><td colspan="3">&nbsp;</td></tr>

            <g:uploadForm action="submit">
                <tr>
                    <td class="table-column-header"><g:message code="bulk.sample.creation.delimiter"/></td>
                    <td>
                        <g:select name="delimiter" class="use-select-2" style="width: 24ch;"
                                  from="${delimiters}" value="${delimiter}" optionValue="displayName"
                                  noSelection="['': 'Choose a Delimiter']"/>
                    </td>
                    <td></td>
                </tr>
                <tr>
                    <td class="table-column-header"><g:message code="bulk.sample.creation.text.upload"/></td>
                    <td>
                        <g:textArea name="sampleText" id="sampleText" class="form-control" style="min-width: 500px;"
                                    rows="25" cols="100" value="${sampleText}"/>
                    </td>
                    <td>
                        <g:message code='bulk.sample.creation.text.upload.info' args="${header}"/>
                        <pre class="mt-3"><g:message code='bulk.sample.creation.text.upload.example' args="${header}"/></pre>
                    </td>
                </tr>
                <tr>
                    <td colspan="3">
                        <button type="submit" class="btn btn-primary">${g.message(code: "default.button.submit.label")}</button>
                    </td>
                </tr>
            </g:uploadForm>

        </tbody>
        </table>
    </div>
</div>
</body>
</html>
