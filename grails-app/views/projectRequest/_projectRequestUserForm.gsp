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
<div class="project-request-user-wrapper clone-remove-target">
    <table class="project-request-user-table">
        <tr>
            <td><label for="users[${i}].username"><g:message code="projectUser.addMember.username"/></label></td>
            <td>
                <span class="user-auto-complete">
                    <input id="users[${i}].username" name="users[${i}].username"
                           type="text" class="inputField autocompleted" autocomplete="off"
                           placeholder="${g.message(code: 'projectUser.addMember.ldapSearchValues')}"
                           value="${user?.username}"/>
                </span>
            </td>
        </tr>
        <tr>
            <td><label for="users[${i}].projectRoles"><g:message code="projectUser.addMember.role"/></label></td>
            <td>
                %{--
                If it is initialized with a user it is about to be rendered: apply select2 normally.
                If it is without a user it is likely used as a template for cloning: use after clone variant.
                --}%
                <g:set var="select2Variant" value="${user ? "use-select-2" : "use-select-2-after-clone"}"/>
                <select id="users[${i}].projectRoles" name="users[${i}].projectRoles"class="project-role-select inputField ${select2Variant}" multiple>
                    <g:each in="${availableRoles}" var="role">
                        <option value="${role.id}" ${role in user?.projectRoles ? "selected" : ""}>${role.name}</option>
                    </g:each>
                </select>
            </td>
        </tr>
        <tr>
            <td><label for="users[${i}].accessToFiles"><g:message code="projectUser.addMember.accessToFiles"/></label></td>
            <td><g:checkBox name="users[${i}].accessToFiles" class="inputField" value="true" checked="${user?.accessToFiles}"/></td>
        </tr>
        <tr>
            <td><label for="users[${i}].manageUsers"><g:message code="projectUser.addMember.manageUsers"/></label></td>
            <td><g:checkBox name="users[${i}].manageUsers" class="inputField set-for-authority" value="true" checked="${user?.manageUsers}"/></td>
        </tr>
    </table>
    <div class="clone-remove-container">
        <button class="clone-remove" title="${g.message(code: "projectRequest.users.remove.tooltip")}">
            <g:message code="projectRequest.users.remove"/>
        </button>
    </div>
</div>
