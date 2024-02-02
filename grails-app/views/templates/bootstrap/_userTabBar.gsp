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

<ul class="nav nav-tabs" id="myTab" role="tablist">
    <g:if test="${tabs.contains('pi')}">
        <li class="nav-item" role="presentation">
            <a class="nav-link active" id="pi-tab" data-bs-toggle="tab" href="#pi" role="tab" aria-controls="pi" aria-selected="true">
                <g:message code="projectRequest.piUser.add"/>
            </a>
        </li>
    </g:if>
    <g:if test="${tabs.contains('user')}">
        <li class="nav-item" role="presentation">
            <a class="nav-link" id="user-tab" data-bs-toggle="tab" href="#user" role="tab" aria-controls="user" aria-selected="false"><g:message
                    code="projectRequest.user.add"/>
            </a>
        </li>
    </g:if>
</ul>

<div class="tab-content" id="inputTabsContent">
    <g:if test="${tabs.contains('pi')}">
        <div class="tab-pane fade show active mt-2" id="pi" role="tabpanel" aria-labelledby="pi-tab">
            <div class="container pi-user-form">
                <div class="row">
                    <div class="col-sm-9">
                        <h3>Add PI</h3>

                        <p>${g.message(code: "projectRequest.piUsers.detail")}</p>
                    </div>

                    <div class="col-sm-3">
                        <button class="btn btn-primary pi-user-add-button" id="clone-add-2">
                            <i class="bi bi-plus"></i>
                        </button>
                    </div>
                </div>

                <div class="clone-target-2" id="accordion2" data-highest-index="1">
                    <g:if test="${cmd?.piUsers}">
                        <g:each in="${cmd?.piUsers}" var="piUser" status="i">
                            <g:if test="${piUser}">
                                <div class="clone-remove-target-2">
                                    <g:render template="templates/piUserFormAccordion" model="[index: i, piUser: piUser, availableRoles: availableRoles]"/>
                                </div>
                            </g:if>
                        </g:each>
                    </g:if>
                    <g:else>
                        <div class="clone-remove-target-2">
                            <g:render template="templates/piUserFormAccordion"
                                      model="[index: 0, emptyForm: true, availableRoles: availableRoles, departmentPiFeatureEnabled: departmentPiFeatureEnabled]"/>
                        </div>
                    </g:else>
                </div>
            </div>
        </div>
    </g:if>
    <g:if test="${tabs.contains('user')}">
        <div class="tab-pane fade mt-2" id="user" role="tabpanel" aria-labelledby="user-tab">
            <div class="container user-form">
                <div class="row">
                    <div class="col-sm-9">
                        <h3>Add user</h3>
                        <p>${g.message(code: "projectRequest.users.detail")}</p>
                    </div>
                    <div class="col-sm-3">
                        <button class="btn btn-primary user-add-button" id="clone-add-1">
                            <i class="bi bi-plus"></i>
                        </button>
                    </div>
                </div>
                <div class="clone-target-1" id="accordion" data-highest-index="1">
                    <g:if test="${cmd?.users}">
                        <g:each in="${cmd?.users}" var="user" status="i">
                            <g:if test="${user}">
                                <div class="clone-remove-target-1">
                                    <g:render template="templates/userFormAccordion" model="[index: i, user: user, availableRoles: userRoles]"/>
                                </div>
                            </g:if>
                        </g:each>
                    </g:if>
                    <g:else>
                        <div class="clone-remove-target-1">
                            <g:render template="templates/userFormAccordion" model="[index: 0, emptyForm: true, availableRoles: userRoles]"/>
                        </div>
                    </g:else>
                </div>
            </div>
        </div>
    </g:if>
</div>
