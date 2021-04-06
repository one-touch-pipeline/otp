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

<div class="card card-body">
    <g:uploadForm action="addTransfer" useToken="true">
        <input type="hidden" name="dataTransferAgreement.id" value="${dta.id}">

        <div class="row">
            <label for="requestedByInput" class="col-sm-2 col-form-label">
                <g:message code="dataTransfer.dta.transfer.requestedBy"/>
            </label>

            <div class="col-sm-10">
                <input id="requestedByInput" value="${cachedTransferCmd?.requester ?: ""}" type="text" name="requester"
                       class="form-control form-control-sm" required/>
            </div>
        </div>

        <div class="row">
            <label for="ticketIdInput" class="col-sm-2 col-form-label">
                <g:message code="dataTransfer.dta.transfer.ticketId"/>
            </label>

            <div class="col-sm-10">
                <input id="ticketIdInput" value="${cachedTransferCmd?.ticketID ?: ""}" type="text" name="ticketID"
                       class="form-control form-control-sm" required/>
            </div>
        </div>

        <div class="row">
            <label for="peerPersonInput" class="col-sm-2 col-form-label">
                <g:message code="dataTransfer.dta.transfer.new.peerPerson"/>
            </label>

            <div class="col-sm-10">
                <input id="peerPersonInput" value="${cachedTransferCmd?.peerPerson ?: ""}" type="text" name="peerPerson"
                       class="form-control form-control-sm" required/>
            </div>
        </div>

        <div class="row">
            <label for="peerAccountInput" class="col-sm-2 col-form-label">
                <g:message code="dataTransfer.dta.transfer.new.peerAccount"/>
            </label>

            <div class="col-sm-10">
                <input id="peerAccountInput" value="${cachedTransferCmd?.peerAccount ?: ""}" type="text" name="peerAccount"
                       class="form-control form-control-sm"/>
            </div>
        </div>

        <div class="row">
            <label for="directionInput" class="col-sm-2 col-form-label">
                <g:message code="dataTransfer.dta.transfer.new.direction"/>
            </label>

            <div class="col-sm-10">
                <select id="directionInput" class="custom-select custom-select-sm" name="direction" required>
                    <g:each in="${directions}">
                        <g:if test="${selectedDirection == it}">
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
            <label for="transferModeInput" class="col-sm-2 col-form-label">
                <g:message code="dataTransfer.dta.transfer.transferMode"/>
            </label>

            <div class="col-sm-10">
                <select id="transferModeInput" class="custom-select custom-select-sm" name="transferMode" required>
                    <g:each in="${transferModes}">
                        <g:if test="${selectedTransferMode == it}">
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
            <label for="transferStartedInput" class="col-sm-2 col-form-label">
                <g:message code="dataTransfer.dta.transfer.new.transferStarted"/>
            </label>

            <div class="col-sm-10">
                <input id="transferStartedInput" value="${(cachedTransferCmd?.transferDate ?: new Date()).format("yyyy-MM-dd")}"
                       type="date" name="transferDateInput" class="form-control form-control-sm" required/>
            </div>
        </div>

        <div class="row">
            <label for="completionDateInput" class="col-sm-2 col-form-label">
                <g:message code="dataTransfer.dta.transfer.completionDate"/>
            </label>

            <div class="col-sm-10">
                <input id="completionDateInput" value="${cachedTransferCmd?.completionDate?.format("yyyy-MM-dd") ?: ""}"
                       type="date" name="completionDateInput" class="form-control form-control-sm"/>
            </div>
        </div>

        <div class="row">
            <label for="transferCommentInput" class="col-sm-2 col-form-label">
                <g:message code="dataTransfer.upload.comment"/>
            </label>

            <div class="col-sm-10">
                <textarea id="transferCommentInput"
                          type="text" name="comment"
                          class="form-control form-control-sm">${cachedTransferCmd?.comment ?: ""}</textarea>
            </div>
        </div>

        <br>

        <div class="row">
            <label for="transferFileInput-${dta.id}" class="col-sm-2 col-form-label">
                <g:message code="dataTransfer.upload.path"/>
            </label>

            <div class="col-sm-10 custom-file">
                <input id="transferFileInput-${dta.id}" onchange="updateFileNameOfFileInput('#transferFileInput-${dta.id}')" type="file" name="files"
                       class="custom-file-input form-control-file form-control-sm" required multiple/>
                <label for="transferFileInput-${dta.id}" class="custom-file-label col-form-label-sm" style="margin: 0 15px 0 15px">
                    <g:message code="dataTransfer.upload.files.placeholder"/>
                </label>
            </div>
        </div>
        <br>
        <button type="submit" class="btn btn-sm btn-outline-primary float-right">
            <i class="bi bi-truck"></i>
            ${g.message(code: "dataTransfer.dta.transfer.new.submit")}
        </button>
    </g:uploadForm>
</div>
