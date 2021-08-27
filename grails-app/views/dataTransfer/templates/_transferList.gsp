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

<ul class="transfer-listing">
    <hr>
    <g:each var="transfer" in="${dta.transfersSortedByDateCreatedDesc}">
        <li class="${transfer.completionDate ? "completed" : "ongoing"}">
            <g:message code="dataTransfer.dta.transfer.transfer"/>
            ${transfer.id}, ${transfer.direction.adjective} <strong>${transfer.peerPerson} (${transfer.peerAccount ?: "N/A"})</strong> via ${transfer.transferMode}
            <br>
            <g:message code="dataTransfer.dta.transfer.requestedBy"/>
            ${transfer.requester} via <a ${transfer.ticketLinkable ? "href=${transfer.ticketLink}" : ""}>${transfer.ticketID}</a>
            <br>
            <g:message code="dataTransfer.dta.transfer.new.handledBy"/> ${transfer.performingUser.realName} (${transfer.performingUser.username})
            <br>
            <g:message code="dataTransfer.dta.transfer.new.direction"/>: ${transfer.direction}
            <br>
            <span><g:message code="dataTransfer.upload.files"/>:</span>
            <ul id="transferDocuments-${transfer.id}">
                <g:each var="transferDocument" in="${transfer.dataTransferDocuments}">
                    <li>
                        <g:link action="downloadDataTransferDocument" params='["dataTransferDocument.id": transferDocument.id]'>
                            ${transferDocument.fileName}</g:link>
                    </li>
                </g:each>
            </ul>
            <button type="button" class="btn btn-sm btn-primary alignment-add-files-btn" data-toggle="collapse" data-target="#addFileToTransfer-${transfer.id}"
                    aria-expanded="false" aria-controls="addFileToTransfer-${transfer.id}">
                <i class="bi bi-folder-plus"></i> Add
            </button>

            <div class="collapse container-sm left-aligned" id="addFileToTransfer-${transfer.id}">
                <g:render template="templates/uploadFiles" model="[id: transfer.id, parentName: 'dataTransfer.id', parentValue: transfer.id, formAction: 'addFilesToTransfer']"/>
            </div>
            <br>
            <g:message code="dataTransfer.dta.transfer.started"/> ${transfer.transferDate}
            <g:if test="${transfer.completionDate}">
                <g:message code="dataTransfer.dta.transfer.completionDate"/> ${transfer.completionDate}
            </g:if>
            <g:else>
                <g:message code="dataTransfer.dta.transfer.completionDate.none"/>
                <g:form action="markTransferAsCompleted" useToken="true" style="display: inline" onSubmit="confirmCompleteTransfer(event);">
                    <input type="hidden" name="dataTransfer.id" value="${transfer.id}"/>
                    <button type="submit" class="btn btn-sm btn-outline-primary">
                        <i class="bi bi-check-all"></i>
                        ${g.message(code: "dataTransfer.dta.transfer.new.complete")}
                    </button>
                </g:form>
            </g:else>
            <br>
            <g:message code="dataTransfer.upload.comment"/>:
            <div class="comment-box-wrapper">
                <otp:editorSwitch
                        roles="ROLE_OPERATOR" template="textArea" rows="3" cols="100"
                        link="${g.createLink(controller: 'dataTransfer', action: 'updateDataTransferComment', params: ["dataTransfer.id": transfer.id])}"
                        value="${transfer.comment}"/>
            </div>
        </li>
        <hr>
    </g:each>
</ul>
