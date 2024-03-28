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

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="de.dkfz.tbi.otp.ngsdata.metadatavalidation.metadatasource.MetaDataFileSourceEnum; de.dkfz.tbi.util.spreadsheet.validation.LogLevel; de.dkfz.tbi.util.spreadsheet.validation.Problems" %>
<%@ page import="de.dkfz.tbi.util.spreadsheet.validation.LogLevel" %>
<%@ page import="de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructureBeanName" %>
<html>
<head>
    <meta name="layout" content="metadataLayout"/>
    <meta name="contextPath" content="${request.contextPath}">
    <title>
    <g:message code="metadataImport.title"/>
    <sec:ifAllGranted roles="ROLE_OPERATOR">
        <g:message code="metadataImport.titleOperator"/>
    </sec:ifAllGranted>
    </title>
    <asset:javascript src="modules/defaultPageDependencies.js"/>
    <asset:javascript src="pages/metadataImport/index/metadataImportDataTable.js"/>
    <asset:javascript src="pages/metadataImport/index/metadataImportUpload.js"/>
    <asset:javascript src="common/MultiInputField.js"/>
    <asset:javascript src="common/toaster.js"/>
    <asset:javascript src="modules/application.js"/>
    <asset:stylesheet src="modules/application.css"/>
</head>

<body>
<div id="context-content" class="m-2">
    <!-- this is rendered by the javascript in metadataImportUpload.js -->
</div>

<g:render template="/templates/messages"/>
<div class="m-2 overflow-hidden">

    <g:form name="metadata-import-form" useToken="true" controller="metadataImport" action="importByPathOrContent" enctype="multipart/form-data">
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <div class="mb-3 row">
                <label for="ticketNumber" class="col-sm-2 col-form-label">
                    <g:message code="metadataImport.ticketNumber"/>
                </label>

                <div class="col-sm-6">
                    <input type="text" name="ticketNumber" id="ticketNumber" size="30" required class="form-control" value="${cmd?.ticketNumber ?: ''}">
                </div>

                <div class="col-sm-3 d-flex align-items-center">
                    <div class="form-check m-0">
                        <input class="form-check-input" name="automaticNotification" type="checkbox" ${cmd?.automaticNotification == false ? '' : 'checked'}
                               id="automaticNotification">
                        <label class="form-check-label" for="automaticNotification">
                            <g:message code="metadataImport.ticket.automaticNotificationFlag"/>
                        </label>
                    </div>
                </div>
            </div>

            <div class="mb-3 row">
                <label for="seqCenterComment" class="col-sm-2 col-form-label">
                    <g:message code="metadataImport.ticket.seqCenter.comment"/>
                </label>

                <div class="col-sm-10">
                    <textarea rows="5" name="seqCenterComment" class="form-control" id="seqCenterComment">${cmd?.seqCenterComment ?: ''}</textarea>
                </div>
            </div>
        </sec:ifAllGranted>

        <div class="mb-3 row" id="path-container">
            <label for="paths" class="col-sm-2 col-form-label">
                <g:message code="metadataImport.path"/>
            </label>

            <div class="col-sm-10 multi-input-field">
                <div class="input-group field">
                    <input type="text" class="form-control" name="paths" id="paths" value="${cmd?.paths?.first() ?: ''}">

                    <div class="input-group-append path-spinner" style="display: none;">
                        <button class="btn btn-outline-secondary" type="button" disabled>
                            <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                            <span class="visually-hidden">Loading...</span>
                        </button>
                    </div>
                    <button class="add-field btn btn-outline-secondary p-0" type="button">+</button>
                </div>
                <g:if test="${cmd?.paths?.size() > 1}">
                    <g:each var="path" in="${cmd.paths.tail()}">
                        <div class="input-group field">
                            <input type="text" class="form-control" name="paths" value="${path}">

                            <div class="input-group-append path-spinner" style="display: none;">
                                <button class="btn btn-outline-secondary" type="button" disabled>
                                    <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                                    <span class="visually-hidden">Loading...</span>
                                </button>
                            </div>
                            <button class="remove-field btn btn-outline-secondary p-0" type="button">-</button>
                        </div>
                    </g:each>
                </g:if>
            </div>

        </div>

        <div class="mb-3 row" id="file-container">
            <label for="contentList" class="col-sm-2 col-form-label">
                <g:message code="metadataImport.contentList"/>
            </label>

            <div class="col-sm-10 multi-input-field">
                <div class="input-group field">
                    <input class="form-control drag-and-drop" type="file" multiple="true" name="contentList" id="contentList">

                    <div class="input-group-append content-spinner" style="display: none;">
                        <button class="btn btn-outline-secondary" type="button" disabled>
                            <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                            <span class="visually-hidden">Loading...</span>
                        </button>
                    </div>
                    <button class="add-field btn btn-outline-secondary p-0" type="button">+</button>
                </div>
            </div>
        </div>

        <div class="mb-3 row">
            <label class="col-sm-2 col-form-label ">
                <g:message code="metadataImport.metadataFileSource"/>
            </label>

            <div class="col-sm-10">
                <g:set var="initialMetadataFileSource" value="${cmd?.metadataFileSource ?: MetaDataFileSourceEnum.PATH.name()}"/>
                <g:each status="i" var="metadataFileSource" in="${metadataFileSources}">
                    <g:set var="checked" value="${(metadataFileSource.name() == initialMetadataFileSource) ? "checked" : ""}"/>
                    <div class="form-check form-check-inline py-2">
                        <input class="form-check-input" type="radio" ${checked} name="metadataFileSource" id="metadataFileSource_${i}"
                               value="${metadataFileSource.name()}">
                        <label class="form-check-label" for="metadataFileSource_${i}">${metadataFileSource.displayName}</label>
                    </div>
                </g:each>
            </div>
        </div>

        <div class="mb-3 row">
            <label class="col-sm-2 col-form-label">
                <g:message code="metadataImport.directoryStructure"/>
            </label>

            <div class="col-sm-10">
                <g:set var="initialDirectoryStructure" value="${cmd?.directoryStructure ?: DirectoryStructureBeanName.ABSOLUTE_PATH.name()}"/>
                <g:each status="i" var="directoryStructure" in="${directoryStructures}">
                    <g:set var="checked"
                           value="${(directoryStructure.name() == initialDirectoryStructure) ? "checked" : ""}"/>
                    <div class="form-check form-check-inline py-2">
                        <input class="form-check-input" type="radio" name="directoryStructure" ${checked} id="directoryStructure_${i}"
                               value="${directoryStructure.name()}">
                        <label class="form-check-label" for="directoryStructure_${i}">
                            ${directoryStructure.displayName}
                        </label>
                    </div>
                </g:each>
            </div>
        </div>

        <div class="mb-3 row">
            <div class="col-sm-2"></div>

            <div class="col-sm-10">
                <div class="form-check" id="ignore-md5-sum-row">
                    <input class="form-check-input" name="ignoreMd5sumError" type="checkbox" id="ignore-md5-sum-input">
                    <label for="ignore-md5-sum-input" class="form-check-label">
                        <g:message code="metadataImport.ignoreMd5sumError.label"/>
                    </label>
                </div>
            </div>
        </div>

        <button class="btn btn-primary" id="validate-btn" type="submit" value="Validate">
            <span id="validate-spinner" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            Validate
        </button>
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <button class="btn btn-primary" id="import-btn" type="submit" value="Import" disabled>
                <span id="import-spinner" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                Import
            </button>
            <label id="ignore-warnings-label">
                <g:checkBox id="ignore-warnings-input" name="ignoreWarnings"/>
                <g:message code="metadataImport.ignore"/>
            </label>
        </sec:ifAllGranted>

        <div id="md5-hidden-container" aria-hidden="true" hidden>
            <!-- The md5 sums get rendered in java script -->
        </div>
    </g:form>

    <h3><g:message code="metadataImport.implementedValidations"/></h3>
    <ul>
        <g:each var="implementedValidation" in="${implementedValidations}">
            <li>${implementedValidation}</li>
        </g:each>
    </ul>
</div>
</body>
</html>
