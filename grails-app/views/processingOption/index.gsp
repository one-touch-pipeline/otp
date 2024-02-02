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

<%@ page import="de.dkfz.tbi.otp.qcTrafficLight.TableCellValue" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title><g:message code="processingOption.title"/></title>
    <asset:stylesheet src="pages/processingOption/styles.less"/>
    <asset:javascript src="pages/processingOption/index/functions.js"/>
</head>
<body>

<div class="container-fluid otp-main-container">
    <h3><g:message code="processingOption.title"/></h3>
    <p><g:message code="processingOption.subtitle"/></p>

    <table class="table table-sm table-striped table-hover">
        <thead>
        <tr>
            <th scope="col">#</th>
            <th scope="col"><g:message code="processingOption.list.headers.name"/></th>
            <th scope="col"><g:message code="processingOption.list.headers.type"/></th>
            <th scope="col"><g:message code="processingOption.list.headers.value"/></th>
            <th scope="col"><g:message code="processingOption.list.headers.dateCreated"/></th>
            <th scope="col"><g:message code="processingOption.list.headers.project"/></th>
        </tr>
        </thead>
        <tbody>
        <g:each status="rowIndex" in="${options}" var="option">
            <tr class="processing-options-row">
                <th scope="row">${rowIndex+1}</th>
                <td data-bs-toggle="tooltip" data-placement="top" title="${option.name?.tooltip}">${option.name.value}</td>
                <td class="${option.type.warnColor == TableCellValue.WarnColor.ERROR ? 'table-danger' : ''}">${option.type.value}</td>
                <td id="value-cell-${rowIndex}" class="${option.value.warnColor == TableCellValue.WarnColor.ERROR ? 'table-danger' : ''}">
                    <div class="input-group">
                        <g:if test="${option.allowedValues}">
                            <select class="form-control form-control-sm custom-select custom-select-sm" disabled id="value-${rowIndex}" aria-label="Select a processing option">
                                <g:if test="${!option.value.value}">
                                    <option selected disabled>${g.message(code: "processingOption.placeholder.choose")}</option>
                                </g:if>
                                <g:each in="${option.allowedValues}">
                                    <g:if test="${option.value.value == it}">
                                        <option selected value="${it}">${it}</option>
                                    </g:if>
                                    <g:else>
                                        <option value="${it}">${it}</option>
                                    </g:else>
                                </g:each>
                            </select>
                        </g:if>
                        <g:elseif test="${option.multiline}">
                            <g:textArea disabled="true" id="value-${rowIndex}" class="form-control form-control-sm otp-table-textarea-sm"  placeholder="${g.message(code: "processingOption.placeholder.noValue")}" name="value">${option.value.tooltip}</g:textArea>
                        </g:elseif>
                        <g:else>
                            <input disabled type="text" id="value-${rowIndex}" class="form-control form-control-sm" placeholder="${option.defaultValue ? option.defaultValue + " " + g.message(code: "processingOption.placeholder.suffix") : g.message(code: "processingOption.placeholder.noValue")}" aria-label="Processing option value" value="${option.value.value}">
                        </g:else>
                        <g:set var="oldValue" value="123" />
                        <div class="input-group-append">
                            <button class="btn btn-success btn-sm" type="button" id="button-save-${rowIndex}" onclick="onSave(${rowIndex}, '${option.name.value}', '${option.value.value.replace('\n', '&#10;')}', '${option.type.value}', '${option.project?.id}')" style="display: none;">
                                <i class="bi bi-save"></i>
                                <g:message code="processingOption.button.save"/>
                            </button>
                            <button class="btn btn-outline-secondary btn-sm otp-background-white" type="button" id="button-edit-${rowIndex}" onclick="onEdit(${rowIndex})" title="${g.message(code: "processingOption.button.edit")}">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-outline-danger btn-sm otp-background-white" type="button" onclick="onObsolete('${rowIndex}', '${option.name.value}', '${option.type.value}', '${option.project?.id}')" title="${g.message(code: "processingOption.button.obsolete")}">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    </div>
                </td>
                <td>${option.dateCreated}</td>
                <td>${option.project?.name}</td>
            </tr>
        </g:each>
        </tbody>
    </table>
</div>
</body>
</html>
