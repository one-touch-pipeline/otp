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

<g:set var="VIEWABLE" value="VIEWABLE"/>
<g:set var="APPROVABLE" value="APPROVABLE"/>
<g:if test="${projectRequests}">
    <table>
        <tr>
            <th>${g.message(code: "project.name")}</th>
            <th>${g.message(code: "projectRequest.dateCreated")}</th>
            <th>${g.message(code: "projectRequest.lastUpdated")}</th>
            <th>${g.message(code: "projectRequest.status")}</th>
            <th>${g.message(code: "projectRequest.requester")}</th>
            <g:if test="${mode == "${VIEWABLE}"}">
                <th>${g.message(code: "projectRequest.pi")}</th>
            </g:if>
            <g:elseif test="${mode == "${APPROVABLE}"}">
                <th>${g.message(code: "projectRequest.viewApproveEdit")}</th>
            </g:elseif>
        </tr>
        <g:each var="request" in="${projectRequests}">
            <tr>
                <td>${request.name}</td>
                <td>${request.dateCreated.format("yyyy-MM-dd HH:mm")}</td>
                <td>${request.lastUpdated.format("yyyy-MM-dd HH:mm")}</td>
                <td>${request.status}</td>
                <td>${request.requester}</td>
                <g:if test="${mode == "${VIEWABLE}"}">
                    <td>${request.pi}</td>
                </g:if>
                <g:elseif test="${mode == "${APPROVABLE}"}">
                    <td><g:link action="view" id="${request.id}">${g.message(code: "projectRequest.viewApproveEdit")}</g:link></td>
                </g:elseif>
            </tr>
        </g:each>
    </table>
</g:if>
<g:else>
    <ul>
        <li>${g.message(code: "projectRequest.none")}</li>
    </ul>
</g:else>
