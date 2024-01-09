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

<asset:stylesheet src="pages/projectRequest/styles.less"/>

<table class="table table-sm table-striped table-hover mb-0">
    <thead>
    <tr>
        <th>${g.message(code: "projectUser.addMember.username")}</th>
        <th>${g.message(code: "projectUser.addMember.role")}</th>
        <th>${g.message(code: "projectUser.addMember.accessToOtp")}</th>
        <th>${g.message(code: "projectUser.addMember.accessToFiles")}</th>
        <th>${g.message(code: "projectUser.addMember.manageUsers")}</th>
    </tr>
    </thead>
    <tbody>
    <g:each var="user" in="${users}" status="i">
        <g:hiddenField name="projectRequest.user[${i}].id" value="${user?.id}"/>
        <tr>
            <td style="text-align: start">
                <strong>${user.user}</strong>
            </td>
            <td style="padding-left: 0.1em">
                ${user.projectRolesAsSemanticString}
            </td>
            <td>
                <span class="icon-${user.accessToOtp}"></span>
            </td>
            <td>
                <span class="icon-${user.accessToFiles}"></span>
            </td>
            <td>
                <span class="icon-${user.manageUsers}"></span>
            </td>
        </tr>
    </g:each>
    <g:each var="departmentHead" in="${departmentHeads}">
        <tr>
            <td style="text-align: start">
                <strong>${departmentHead}</strong>
            </td>
            <td style="padding-left: 0.1em">
                PI
            </td>
            <td>
                <span class="icon-true"></span>
            </td>
            <td>
                <span class="icon-false"></span>
            </td>
            <td>
                <span class="icon-true"></span>
            </td>
        </tr>
    </g:each>
    </tbody>
</table>


