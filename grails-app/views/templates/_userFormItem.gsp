%{--
  - Copyright 2011-2022 The OTP authors
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

<!-- Needs the following inputs
   - user: The user that is currently selected
   - emptyForm: boolean if the userFormItem should be empty
   - index: The index to identify the id's and names, if not set a default name and id is chosen
   - accordionItem?: Set true, to remove the additional card class from bootstrap
   - checkboxes?: An array containing the used checkboxes. Is a subset of [fileAccess, otpAccess, manageUsers, manageUsersAndDelegate, receivesNotifications]
   - availableRoles!: The roles to select from the roles select.
 -->

<asset:javascript src="common/userForm.js"/>
<asset:javascript src="common/UserAutoComplete.js"/>

<g:set var="listMode" value="${index || index == 0}"/>

<div class="${accordionItem ? '' : 'card'}">
    <div class="card-body pb-1">
        <div class="mb-3 row">
            <g:set var="description" value="${listMode ? "users[${index}].username" : "username"}"/>
            <label class="col-sm-2 col-form-label"
                   for="${description}">${g.message(code: "projectUser.addMember.username")}</label>

            <div class="user-auto-complete col-sm-10">
                <input class="username-input input-field autocompleted form-control"
                       name="${description}"
                       id="${description}"
                       value="${user?.username}"
                       autocomplete="off"/>
            </div>
        </div>

        <div class="mb-3 row">
            <g:set var="description" value="${listMode ? "users[${index}].projectRoles" : "projectRoles"}"/>
            <label class="col-sm-2 col-form-label"
                   for="${description}">${g.message(code: "projectUser.addMember.role")}</label>

            <div class="col-sm-10">
                <g:set var="select2Variant" value="${(user || emptyForm) ? "use-select-2" : "use-select-2-after-clone"}"/>
                <select class="project-role-select input-field form-control ${select2Variant}" name="${description}"
                        id="${description}"
                        multiple="multiple">
                    <g:each in="${availableRoles}" var="role">
                        <g:set var="selected" value="${role in user?.projectRoles ? "selected" : ""}"/>
                        <option value="${role.id}" ${selected}>${role.name}</option>
                    </g:each>
                </select>
            </div>
        </div>

        <div class="mb-3 row">
            <g:if test="${checkboxes.contains('otpAccess')}">
                <g:set var="description" value="${listMode ? "users[${index}].accessToOtp" : "accessToOtp"}"/>
                <div class="col-sm-2">
                    <g:checkBox class="input-field" name="${description}"
                                id="${description}_checkbox"
                                value="${user ? user?.accessToOtp : true}"/>
                    <label class="col-form-label"
                           for="${description}_checkbox">${g.message(code: "projectUser.addMember.accessToOtp")}</label>
                </div>
            </g:if>

            <g:if test="${checkboxes.contains('fileAccess')}">
                <g:set var="description" value="${listMode ? "users[${index}].accessToFiles" : "accessToFiles"}"/>
                <div class="col-sm-2">
                    <g:checkBox class="set-for-BIOINFORMATICIAN set-for-LEAD_BIOINFORMATICIAN input-field" name="${description}"
                                id="${description}_checkbox"
                                value="${user?.accessToFiles}"/>
                    <label class="col-form-label"
                           for="${description}_checkbox">${g.message(code: "projectUser.addMember.accessToFiles")}</label>
                </div>
            </g:if>

            <g:if test="${checkboxes.contains('manageUsers')}">
                <g:set var="description" value="${listMode ? "users[${index}].manageUsers" : "manageUsers"}"/>
                <g:hiddenField class="hidden-manage-users-field" id="${description}_hiddenField" name="${description}" value="true" disabled="true"/>
                <div class="col-sm-2">
                    <g:checkBox class="set-and-block-for-PI set-and-block-for-COORDINATOR input-field"
                                name="${description}"
                                id="${description}_checkbox"
                                value="${user?.manageUsers}"/>
                    <label class="col-form-label"
                           for="${description}_checkbox">${g.message(code: "projectUser.addMember.manageUsers")}</label>
                </div>
            </g:if>

            <g:if test="${checkboxes.contains('manageUsersAndDelegate')}">
                <g:set var="description" value="${listMode ? "users[${index}].manageUsersAndDelegate" : "manageUsersAndDelegate"}"/>
                <div class="col-sm-3">
                    <g:checkBox class="set-and-block-for-PI input-field" name="${description}"
                                id="${description}_checkbox"
                                value="${user?.manageUsersAndDelegate}"/>
                    <label class="col-form-label"
                           for="${description}_checkbox">${g.message(code: "projectUser.addMember.manageUsersAndDelegate")}</label>
                </div>
            </g:if>

            <g:if test="${checkboxes.contains('receivesNotifications')}">
                <g:set var="description" value="${listMode ? "users[${index}].receivesNotifications" : "receivesNotifications"}"/>
                <div class="col-sm-3">
                    <g:checkBox class="input-field" name="${description}"
                                id="${description}_checkbox"
                                value="${true}"/>
                    <label class="col-form-label"
                           for="${description}_checkbox">${g.message(code: "projectUser.addMember.receivesNotifications")}</label>
                </div>
            </g:if>

        </div>
    </div>
</div>

