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

%{--
  - Can be used with an individual choice of available tabs. All available tabs are disabled by default and
  - have to be enabled explicitly.
  -
  - Available tabs:
  - project
  - pid
  - lane
  - ilse
  - multiInput
  -
  - Usage example: <g:render template="/templates/bootstrap/seqTrackSelectionTabBar" model="[tabs: ['project', 'pid', 'lane', 'ilse', 'multiInput']]"/>
  --}%
<ul class="nav nav-tabs" id="myTab" role="tablist">
    <g:if test="${tabs.contains('project')}">
        <li class="nav-item" role="presentation">
            <a class="nav-link active" id="project-tab" data-bs-toggle="tab" data-bs-target="#project" type="button" role="tab" aria-controls="project" aria-selected="true">
                <g:message code="triggerAlignment.input.tab.project"/>
            </a>
        </li>
    </g:if>
    <g:if test="${tabs.contains('pid')}">
        <li class="nav-item" role="presentation">
            <a class="nav-link" id="pid-tab" data-bs-toggle="tab" data-bs-target="#pid" type="button" role="tab" aria-controls="pid" aria-selected="false"
            ><g:message code="triggerAlignment.input.tab.pid"/>
            </a>
        </li>
    </g:if>
    <g:if test="${tabs.contains('lane')}">
        <li class="nav-item" role="presentation">
            <a class="nav-link" id="lane-tab" data-bs-toggle="tab" data-bs-target="#lane" type="button" role="tab" aria-controls="lane" aria-selected="false">
                <g:message code="triggerAlignment.input.tab.lane"/>
            </a>
        </li>
    </g:if>
    <g:if test="${tabs.contains('ilse')}">
        <li class="nav-item" role="presentation">
            <a class="nav-link" id="ilse-tab" data-bs-toggle="tab" data-bs-target="#ilse" type="button" aria-controls="ilse" aria-selected="false">
                <g:message code="triggerAlignment.input.tab.ilse"/>
            </a>
        </li>
    </g:if>
    <g:if test="${tabs.contains('multiInput')}">
        <li class="nav-item" role="presentation">
            <a class="nav-link" id="multi-input-tab" data-bs-toggle="tab" data-bs-target="#multi-input" type="button" role="tab" aria-controls="multi-input" aria-selected="false">
                <g:message code="triggerAlignment.input.tab.multiInput"/>
            </a>
        </li>
    </g:if>
</ul>

<div class="tab-content" id="inputTabsContent">
    <g:if test="${tabs.contains('project')}">
        <div class="tab-pane fade show active mt-2" id="project" role="tabpanel" aria-labelledby="project-tab">
            <g:render template="/templates/bootstrap/seqTrackSelectionTabBar/tabs/projectAndSeqTypeTab" model="[seqTypes: seqTypes, id: 'seqTypeProject']"/>
        </div>
    </g:if>
    <g:if test="${tabs.contains('pid')}">
        <div class="tab-pane fade mt-2" id="pid" role="tabpanel" aria-labelledby="pid-tab">
            <g:render template="/templates/bootstrap/seqTrackSelectionTabBar/tabs/pidAndSeqTypeTab" model="[seqTypes: seqTypes, id: 'seqTypePid']"/>
        </div>
    </g:if>
    <g:if test="${tabs.contains('lane')}">
        <div class="tab-pane fade mt-2" id="lane" role="tabpanel" aria-labelledby="lane-tab">
            <g:render template="/templates/bootstrap/seqTrackSelectionTabBar/tabs/laneIdTab"/>
        </div>
    </g:if>
    <g:if test="${tabs.contains('ilse')}">
        <div class="tab-pane fade mt-2" id="ilse" role="tabpanel" aria-labelledby="ilse-tab">
            <g:render template="/templates/bootstrap/seqTrackSelectionTabBar/tabs/ilseIdTab"/>
        </div>
    </g:if>
    <g:if test="${tabs.contains('multiInput')}">
        <div class="tab-pane fade mt-2" id="multi-input" role="tabpanel" aria-labelledby="multi-input-tab">
            <g:render template="/templates/bootstrap/seqTrackSelectionTabBar/tabs/multiInputTab"/>
        </div>
    </g:if>
</div>
