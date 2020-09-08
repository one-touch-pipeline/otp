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

<g:set var="selector" value="${selector as de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigSelector}"/>
<div style="border: 1px black solid">
    <div>
        <b>${selector.basePriority} (${selector.fineTuningPriority}) <g:link controller="workflowConfig" action="configure" params="${['selector.id': selector.id]}">${selector.name}</g:link></b>
        <otp:expandable collapsed="expanded">
            <table>
                <tr>
                    <td>${g.message(code: "workflowConfig.selector.type")}</td>
                    <td>${selector.selectorType}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "workflowConfig.selector.workflows")}</td>
                    <td>${selector.workflows*.name.join(", ") ?: g.message(code: "workflowConfig.selector.all")}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "workflowConfig.selector.versions")}</td>
                    <td>${selector.workflowVersions*.version.join(", ") ?: g.message(code: "workflowConfig.selector.all")}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "workflowConfig.selector.projects")}</td>
                    <td>${selector.projects*.name.join(", ") ?: g.message(code: "workflowConfig.selector.all")}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "workflowConfig.selector.seqTypes")}</td>
                    <td>${selector.seqTypes*.name.join(", ") ?: g.message(code: "workflowConfig.selector.all")}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "workflowConfig.selector.referenceGenomes")}</td>
                    <td>${selector.referenceGenomes*.name.join(", ") ?: g.message(code: "workflowConfig.selector.all")}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "workflowConfig.selector.libPrepKits")}</td>
                    <td>${selector.libraryPreparationKits*.name.join(", ") ?: g.message(code: "workflowConfig.selector.all")}</td>
                </tr>
            </table>
        </otp:expandable>
    </div>

    <div>
        <b>${selector.externalWorkflowConfigFragment.name}</b>
        <otp:expandable collapsed="expanded">
            <pre>${grails.converters.JSON.parse(selector.externalWorkflowConfigFragment.configValues).toString(2)}</pre>
        </otp:expandable>
    </div>
</div>
