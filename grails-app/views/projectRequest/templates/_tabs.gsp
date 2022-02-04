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

<div>
    <ul class="nav nav-tabs">
        <li class="nav-item">
            <g:link action="index" class="${actionName == "index" ? "active" : ""} nav-link"><g:message code="projectRequest.new.tab"/></g:link>
        </li>
        <li class="nav-item">
            <g:link action="unresolved" class="${actionName == "unresolved" ? "active" : ""} nav-link"><g:message code="projectRequest.unresolved.tab"/></g:link>
        </li>
        <li class="nav-item">
            <g:link action="resolved" class="${actionName == "resolved" ? "active" : ""} nav-link"><g:message code="projectRequest.resolved.tab"/></g:link>
        </li>
        <sec:ifAnyGranted roles="ROLE_OPERATOR">
            <li class="nav-item">
                <g:link action="all" class="${actionName == "all" ? "active" : ""} nav-link"><g:message code="projectRequest.all.tab"/></g:link>
            </li>
        </sec:ifAnyGranted>
    </ul>
    <div class="pb-2"></div>
</div>