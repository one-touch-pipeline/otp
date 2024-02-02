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
<h2 style="display: inline; padding-right: 20px"><a id="project-anchor-${preparedNotification.project.id}"></a>${preparedNotification.project}</h2>
<a href="#summary-anchor"><g:message code="notification.preparedNotification.return"/></a><br>

<h3><g:message code="notification.preparedNotification.recipients.header" args="[preparedNotification.toBeNotifiedProjectUsers.size()]"/>:</h3>
<div>
    <otp:expandable value="show all recipients" collapsed="true">
        <table style="width: 750px">
            <tr>
                <th><g:message code="notification.preparedNotification.recipients.mail"/></th>
                <th><g:message code="notification.preparedNotification.recipients.username"/></th>
                <th><g:message code="notification.preparedNotification.recipients.realName"/></th>
            </tr>
            <g:each var="projectUser" in="${preparedNotification.toBeNotifiedProjectUsers}">
                <tr>
                    <td>${projectUser.user.email}</td>
                    <td>${projectUser.user.username}</td>
                    <td>${projectUser.user.realName}</td>
                </tr>
            </g:each>
        </table>
    </otp:expandable>
</div>
<div class="basic-flex-box">
    <div class="item basic-right-padding">
        <h3><g:message code="notification.preparedNotification.preparedNotification.mail"/></h3>
        <g:set var="width" value="900px"/>
        <input type="text" value="${preparedNotification.subject}" style="width: ${width}" readonly><br>
        <textarea class="resize-vertical" rows="30" style="width: ${width}" readonly>${preparedNotification.notification}</textarea>
    </div>
    <div class="item basic-right-padding">
        <h3><g:message code="notification.preparedNotification.preparedNotification.status"/></h3>
        <g:render template="processingStatus" model="[processingStatus: preparedNotification.processingStatus]" />
    </div>
</div>
<br><br>
