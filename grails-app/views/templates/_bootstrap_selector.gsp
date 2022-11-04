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

<g:set var="selector" value="${selector as de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigSelector}"/>
<g:each in="${relatedSelectors}" var="selector">
    <div data-type="${selector.selectorType}">
        <div id="${selector.id}" class="card">

            <div class="card-header" id="heading${selector.id}">
                <a class="stretched-link plain-link" data-toggle="collapse" href="#collapse${selector.id}" role="button" aria-expanded="true"
                   data-target="#collapse${selector.id}" aria-controls="collapse${selector.id}">
                    <i class="bi bi-chevron-down float-right"></i>
                    ${selector.priority}
                    <g:link class="above-stretched-link" controller="workflowConfig" action="index" fragment="workflowConfigModal"
                            params="${['selector.id': selector.id]}">${selector.name}</g:link>
                </a>
            </div>

            <div id="collapse${selector.id}" class="collapse show" aria-labelledby="heading${selector.id}">
                <div class="card-body">
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
                            <td>${selector.workflowVersions*.workflowVersion.join(", ") ?: g.message(code: "workflowConfig.selector.all")}</td>
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


                    <div id="code${selector.id}" class="code">
                        <g:if test="${selector.externalWorkflowConfigFragment.name}">
                            <p>${selector.externalWorkflowConfigFragment.name}</p>
                        </g:if>
                        <g:else>
                            <p class="text-secondary">${g.message(code: "workflowConfig.fragment.name.undefined")}</p>
                        </g:else>
                        <pre class="collapse"
                             id="collapse-code${selector.id}">${grails.converters.JSON.parse(selector.externalWorkflowConfigFragment.configValues).toString(2)}</pre>
                        <a role="button" class="collapsed" data-toggle="collapse" href="#collapse-code${selector.id}" aria-expanded="false"
                           aria-controls="collapse-code${selector.id}"></a>
                    </div>

                </div>
            </div>

        </div>
    </div>
</g:each>
<g:if test="${!relatedSelectors}">
    ${g.message(code: "workflowConfig.config.related.none")}
</g:if>
