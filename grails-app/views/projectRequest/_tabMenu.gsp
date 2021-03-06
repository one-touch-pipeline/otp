%{--
  - Copyright 2011-2020 The OTP authors
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

<div class="tab-menu">
    <g:link action="index" class="${actionName == "index" ? "active" : ""}"><g:message code="projectRequest.new.tab"/></g:link>
    <g:link action="open" class="${actionName == "open" ? "active" : ""}"><g:message code="projectRequest.open.tab"/> <span class="counter ${actionHighlight}">${unresolved.size()}</span></g:link>
    <g:link action="resolved" class="${actionName == "resolved" ? "active" : ""}"><g:message code="projectRequest.resolved.tab"/> <span class="counter no-work-and-nothing-todo">${resolved.size()}</span></g:link>
    <sec:ifAnyGranted roles="ROLE_OPERATOR">
        <g:link action="all" class="${actionName == "all" ? "active" : ""}"><g:message code="projectRequest.all.tab"/></g:link>
    </sec:ifAnyGranted>
</div>
