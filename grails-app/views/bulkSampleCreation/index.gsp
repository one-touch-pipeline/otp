%{--
  - Copyright 2011-2024 The OTP authors
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
    <meta name="layout" content="main"/>
    <title><g:message code="bulk.sample.creation.title"/></title>
    <asset:javascript src="pages/bulkSampleCreation/index/bulkSampleCreation.js"/>
</head>

<body>
<div class="body" id="bulk-sample-creation">
    <g:render template="/templates/messages"/>
    <g:render template="/templates/projectSelection"/>

    <h1><g:message code="bulk.sample.creation.title"/></h1>
    <otp:annotation type="info">
        <g:message code="bulk.sample.creation.description"/>
    </otp:annotation>

    <div>
        <table>
            <tbody>
            <tr>
                <td><span class="table-column-header"><g:message code="individual.insert.project"/></span></td>
                <td>${selectedProject}</td>
                <td><b><g:message code="bulk.sample.creation.multipleProjects"/></b></td>
            </tr>
            <tr>
                <td><span class="table-column-header"><g:message code="bulk.sample.creation.file.upload"/></span></td>
                <td>
                <g:uploadForm action="upload">
                    <input type="file" name="content"/>
                      <g:submitButton name="upload"/>
                </g:uploadForm>
                </td>
                <td>
                    <g:message code='bulk.sample.creation.file.upload.info'
                               args="${header}"/>
                </td>
            </tr>
            <tr><td colspan="3">&nbsp;</td></tr>
            <g:uploadForm action="submit">
                <tr>
                    <td><span class="table-column-header"><g:message code="bulk.sample.creation.delimiter"/></span></td>
                    <td>
                        <g:select name="delimiter" class="use-select-2" style="width: 24ch;"
                                  from="${delimiters}" value="${delimiter}" optionValue="displayName"
                                  noSelection="['': 'Choose a Delimiter']"/>
                    </td>
                    <td></td>
                </tr>
                <tr>
                    <td><span class="table-column-header"><g:message code="bulk.sample.creation.text.upload"/></span></td>
                    <td>
                        <g:textArea name="sampleText" id="sampleText" style="min-width: 500px;"
                                    rows="25" cols="100" value="${sampleText}"/>
                    </td>
                    <td>
                        <g:message code='bulk.sample.creation.text.upload.info'
                                   args="${header}"/>
                        <p><pre><g:message code='bulk.sample.creation.text.upload.example'
                                           args="${header}"/></pre></p>
                    </td>
                </tr>
                <tr>
                    <td><span class="table-column-header"><g:message code="bulk.sample.creation.referenceGenomeSource"/></span></td>
                    <td>
                        <label onclick="$.otp.bulkSampleCreation.toggleEnable('createMissingSampleTypes', 'referenceGenomeSource', false)">
                            <g:checkBox checked="${createMissingSampleTypes}" name="createMissingSampleTypes" value="${createMissingSampleTypes}" style="vertical-align: middle"/>
                            <g:message code="bulk.sample.creation.referenceGenomeSource.createSamples"/>
                        </label><br>
                        <g:select name="referenceGenomeSource" class="use-select-2"
                                  from="${referenceGenomeSources}" value="${referenceGenomeSource}"
                                  disabled="${!createMissingSampleTypes}"/>
                    </td>
                    <td><g:message code="bulk.sample.creation.referenceGenomeSource.info"/></td>
                </tr>
                <tr>
                    <td colspan="3">
                        <g:submitButton name="Submit"/>
                    </td>
                </tr>
            </g:uploadForm>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
