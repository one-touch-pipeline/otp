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

<div class="card clone-remove-target">
    <div class="card-header pt-1 pb-1">
        <g:hiddenField name="users[${i}].projectRequestUser.id" value="${user?.projectRequestUser?.id}"/>

        <div class="row align-items-center">
            <a class="col-sm-9" id="user-form-title-${i}" data-toggle="collapse" data-target="#collapse-${i}" aria-expanded="true" aria-controls="collapse-${i}">
                New User
            </a>

            <div class="clone-remove-container col-sm-3">
                <button class="clone-remove btn btn-danger user-delete-button" type="button">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        </div>

    </div>

    <div id="collapse-${i}" class="collapse show" aria-labelledby="heading-${i}" data-parent="#accordion">
        <div class="card-body pb-1">
            <div class="form-group row">
                <label class="col-sm-2 col-form-label" for="users[${i}].username">${g.message(code: "projectUser.addMember.username")}</label>

                <div class="user-auto-complete col-sm-10">
                    <input class="username-input autocompleted form-control"
                           name="users[${i}].username"
                           id="users[${i}].username"
                           value="${user?.username}"
                           autocomplete="off"/>
                </div>
            </div>

            <div class="form-group row">
                <label class="col-sm-2 col-form-label" for="users[${i}].projectRoles">${g.message(code: "projectUser.addMember.role")}</label>

                <div class="col-sm-10">
                    <g:set var="select2Variant" value="${(user || emptyForm) ? "use-select-2" : "use-select-2-after-clone"}"/>
                    <select class="project-role-select form-control ${select2Variant}" name="users[${i}].projectRoles" id="users[${i}].projectRoles" multiple="multiple">
                        <g:each in="${availableRoles}" var="role">
                            <g:set var="selected" value="${role in user?.projectRoles ? "selected" : ""}"/>
                            <option value="${role.id}" ${selected}>${role.name}</option>
                        </g:each>
                    </select>
                </div>
            </div>

            <div class="form-group row">
                <div class="col-sm-2">
                    <g:checkBox name="users[${i}].accessToOtp"
                                id="users[${i}].accessToOtp"
                                value="${user ? user?.accessToOtp : true}"/>
                    <label class="col-form-label" for="users[${i}].accessToOtp">${g.message(code: "projectUser.addMember.accessToOtp")}</label>

                </div>



                <div class="col-sm-2">
                    <g:checkBox name="users[${i}].accessToFiles"
                                id="users[${i}].accessToFiles"
                                value="${user?.accessToFiles}"/>
                    <label class="col-form-label" for="users[${i}].accessToFiles">${g.message(code: "projectUser.addMember.accessToFiles")}</label>
                </div>

                <div class="col-sm-2">
                    <g:checkBox class="set-for-authority"
                                name="users[${i}].manageUsers"
                                id="users[${i}].manageUsers"
                                value="${user?.manageUsers}"/>
                    <label class="col-form-label" for="users[${i}].manageUsers">${g.message(code: "projectUser.addMember.manageUsers")}</label>
                </div>
            </div>
        </div>
    </div>
</div>

