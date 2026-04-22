%{--
  - Copyright 2011-2026 The OTP authors
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

<ul class="nav nav-tabs tab-menu">
    <li class="nav-item">
        <g:link controller="workflowSelection" action="index" class="nav-link ${controllerName == "workflowSelection" ? 'active fw-bold' : 'text-black'}">
            <g:message code="config.tabMenu.workflowSelection"/>
        </g:link>
    </li>
    <li class="nav-item">
        <g:link controller="sampleCategory" action="index" class="nav-link ${controllerName == "sampleCategory" ? 'active fw-bold' : 'text-black'}">
            <g:message code="config.tabMenu.sampleCategory"/>
        </g:link>
    </li>
    <li class="nav-item">
        <g:link controller="qcThreshold" action="projectConfiguration" class="nav-link ${controllerName == "qcThreshold" ? 'active fw-bold' : 'text-black'}">
            <g:message code="config.tabMenu.qcThresholds"/>
        </g:link>
    </li>
    <li class="nav-item">
        <g:link controller="cellRangerConfiguration" action="index" class="nav-link ${controllerName == "cellRangerConfiguration" ? 'active fw-bold' : 'text-black'}">
            <g:message code="config.tabMenu.cellRanger"/>
        </g:link>
    </li>
</ul>
