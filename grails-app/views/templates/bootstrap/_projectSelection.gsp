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

<div class="input-group mb-3" style="width: 500px;">
    <div class="input-group-prepend">
        <label class="input-group-text" for="project"><g:message code="projectSelection.project"/></label>
    </div>
    <div class="selected-project-value hidden">
        <strong>${selectedProject.displayName}</strong>
    </div>
    %{-- variables are form ProjectSelectionInterceptor --}%
    <select id="project" class="custom-select use-select-2" onChange='window.location = this.value;'>
        <g:each in="${availableProjects}" var="project">
            <option value="${g.createLink(controller: controllerName, action: actionName, params: [(projectParameter): project.name])}"
                ${selectedProject == project ? "selected" : ""}>
                ${project.displayName}
            </option>
        </g:each>
    </select>
    <div class="input-group-append">
        <button class="btn btn-outline-secondary select2-appended-btn" type="button" onclick="$.otp.copyToClipboard('${selectedProject.displayName}')"
                title="${g.message(code: 'projectSelection.clipboard')}" data-placement="bottom">
            <i class="bi bi-clipboard"></i>
        </button>
    </div>
</div>
