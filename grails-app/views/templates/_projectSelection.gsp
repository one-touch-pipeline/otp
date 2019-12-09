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

<g:form style="width: min-content" class="project-selection-container" controller="projectSelection" action="select">
    <g:hiddenField name="displayName" value=""/>
    <g:hiddenField name="type" value="PROJECT"/>
    <g:hiddenField name="redirect" value="${request.forwardURI - request.contextPath}"/>
    <div class="selected-project-label">
        <strong><g:message code="home.projectFilter.project"/>:</strong>
    </div>
    <div class="selected-project-value">
        <strong>${project?.name}</strong>
    </div>
    <div class="select-label">
        <g:message code="home.projectFilter.select"/>:
    </div>
    <div class="project-dropdown">
        <g:select id="project" name='id' from='${projects}' value='${project?.id}'
                  optionKey='id' optionValue='displayName'
                  autocomplete="off" onChange='submit();'/>
    </div>
</g:form>
