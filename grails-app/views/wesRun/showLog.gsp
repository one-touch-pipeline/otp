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

<html>
<head>
    <title>${title}</title>
</head>

<body>
<div class="container-fluid otp-main-container">
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item">
                <g:link controller="workflowRunList" action="index"
                        params="['workflow.id': nav.workflow?.id, state: nav.states?.join(','), name: nav.name]">
                    ${g.message(code: "workflow.navigation.list")}
                </g:link>
            </li>
            <li class="breadcrumb-item">
                <g:link controller="workflowRunDetails" action="index"
                        params="['workflow.id': nav.workflow?.id, state: nav.states?.join(','), name: nav.name]"
                        id="${nav.workflowRun?.id}">
                    ${g.message(code: "workflow.navigation.details")} (${nav.workflowRun?.id})
                </g:link>
            </li>
            <li class="breadcrumb-item active" aria-current="page">
                ${title} (${wesRun.id})
            </li>
        </ol>
    </nav>

    <h1>${title} (<g:link action="show" id="${wesRun.id}">${wesRun.id}</g:link>)</h1>

    ${wesRun.workflowStep.beanName} of ${raw(wesRun.workflowStep.workflowRun.displayName.replace("\n", "<br>"))}

    <pre>${filePath}</pre>
    <hr>
    <pre>${content}</pre>
</div>

</body>
</html>
