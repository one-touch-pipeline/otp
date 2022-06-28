%{--
  - Copyright 2011-2019 The OTP authors
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

<tr>
    <th></th>
    <th><g:message code="projectUser.table.name"/></th>
    <th><g:message code="projectUser.table.username"/></th>
    <th><g:message code="projectUser.table.department"/></th>
    <th><g:message code="projectUser.table.mail"/></th>
    <th><g:message code="projectUser.table.role"/></th>
    <g:if test="${mode == 'enabled'}">
        <th title="${g.message(code: 'projectUser.table.otpAccess.tooltip')}"><g:message code="projectUser.table.otpAccess"/></th>
        <th title="${g.message(code: 'projectUser.table.fileAccess.tooltip')}"><g:message code="projectUser.table.fileAccess"/></th>
        <th title="${g.message(code: 'projectUser.table.manageUsers.tooltip')}"><g:message code="projectUser.table.manageUsers"/></th>
        <th title="${g.message(code: 'projectUser.table.manageUsersAndDelegate.tooltip')}"><g:message code="projectUser.table.manageUsersAndDelegate"/></th>
        <th title="${g.message(code: 'projectUser.table.receivesNotifications.tooltip')}"><g:message code="projectUser.table.receivesNotifications"/></th>
    </g:if>
    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
        <th><g:message code="projectUser.table.projectAccess"/></th>
    </sec:access>
</tr>
