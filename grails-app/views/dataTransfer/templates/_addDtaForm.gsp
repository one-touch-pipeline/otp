%{--
  - Copyright 2011-2021 The OTP authors
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
<%@ page import="de.dkfz.tbi.util.TimeFormats" %>
<div class="container-sm left-aligned">
    <div class="card">
        <div class="card-header">
            <g:message code="dataTransfer.dta.header.upload"/>
        </div>

        <div class="card-body">
            <g:uploadForm action="addDataTransferAgreement" useToken="true">
                <div class="row">
                    <label for="dtaFileInput" class="col-sm-2 col-form-label">
                        <g:message code="dataTransfer.upload.path"/>
                    </label>

                    <div class="col-sm-10 custom-file">
                        <input id="dtaFileInput" onchange="updateFileNameOfFileInput('#dtaFileInput')" type="file" name="files"
                               class="custom-file-input form-control-file form-control-sm" required multiple/>
                        <label for="dtaFileInput" class="custom-file-label col-form-label-sm" style="margin: 0 15px 0 15px"><g:message code="dataTransfer.upload.files.placeholder"/></label>
                    </div>
                </div>

                <div class="row">
                    <label for="partnerInstitutionInput" class="col-sm-2 col-form-label">
                        <g:message code="dataTransfer.dta.transfer.peerInstitution"/>
                    </label>

                    <div class="col-sm-10">
                        <input id="partnerInstitutionInput" value="${docDtaCmd?.peerInstitution}" type="text" name="peerInstitution"
                               class="form-control form-control-sm" required/>
                    </div>
                </div>

                <div class="row">
                    <label for="legalBasisInput" class="col-sm-2 col-form-label">
                        <g:message code="dataTransfer.dta.transfer.legalBasis"/>
                    </label>

                    <div class="col-sm-10">
                        <select id="legalBasisInput" class="custom-select custom-select-sm" name="legalBasis">
                            <g:each in="${legalBases}">
                                <g:if test="${selectedLegalBasis == it}">
                                    <option selected value="${it}">${it}</option>
                                </g:if>
                                <g:else>
                                    <option value="${it}">${it}</option>
                                </g:else>
                            </g:each>
                        </select>
                    </div>
                </div>

                <div class="row">
                    <label for="validityDateInput" class="col-sm-2 col-form-label">
                        <g:message code="dataTransfer.dta.transfer.validityDate"/>
                    </label>

                    <div class="col-sm-10">
                        <input id="validityDateInput" value="${TimeFormats.DATE.getFormattedDate(docDtaCmd?.validityDate as Date)}" type="date" name="validityDateInput"
                               class="form-control form-control-sm"/>
                    </div>
                </div>

                <div class="row">
                    <label for="dtaIdInput" class="col-sm-2 col-form-label">
                        <g:message code="dataTransfer.dta.transfer.dtaId"/>
                    </label>

                    <div class="col-sm-10">
                        <input id="dtaIdInput" value="${docDtaCmd?.dtaId}" type="text" name="dtaId"
                               class="form-control form-control-sm"/>
                    </div>
                </div>

                <div class="row">
                    <label for="commentInput" class="col-sm-2 col-form-label">
                        <g:message code="dataTransfer.upload.comment"/>
                    </label>

                    <div class="col-sm-10">
                        <textarea id="commentInput" type="text" name="comment"
                                  class="form-control form-control-sm">${docDtaCmd?.comment}</textarea>
                    </div>
                </div>
                <br>
                <button type="submit" class="btn btn-primary float-right">
                    <i class="bi bi-folder-plus"></i>
                    ${g.message(code: "dataTransfer.upload.add")}
                </button>
            </g:uploadForm>
        </div>
    </div>
</div>
