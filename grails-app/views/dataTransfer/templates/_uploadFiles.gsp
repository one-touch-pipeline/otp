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
    <g:uploadForm useToken="true">
        <input type="hidden" name="${parentName}" value="${parentValue}">
        <div class="row">
            <label for="dtaFileInput-${id}" class="col-sm-2 col-form-label">
                <g:message code="dataTransfer.upload.path"/>
            </label>

            <div class="col-sm-10 custom-file">
                <input id="dtaFileInput-${id}" onchange="updateFileNameOfFileInput('#dtaFileInput-${id}')" type="file" name="files"
                       class="custom-file-input form-control-file form-control-sm" required multiple/>
                <label for="dtaFileInput-${id}" class="custom-file-label col-form-label-sm" style="margin: 0 15px 0 15px">
                    <g:message code="dataTransfer.upload.files.placeholder"/>
                </label>
            </div>
        </div>
        <br>
        <button type="button" class="btn btn-sm btn-primary float-right" onclick="onUploadFiles(this, ${id}, '${formAction}')">
            <div class="loading-content" style="display: none">
                <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                ${g.message(code: "dataTransfer.upload.loading")}
            </div>
            <div class="action-content">
                <i class="bi bi-folder-plus"></i>
                ${g.message(code: "dataTransfer.upload.add")}
            </div>
        </button>
    </g:uploadForm>
</div>
