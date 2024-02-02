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

<%@ page import="de.dkfz.tbi.util.TimeFormats" %>
<html>
<head>
    <title></title>
</head>

<body>
<g:if test="${projectRequests}">
    <table class="table table-sm table-striped table-hover">
        <thead>
        <tr>
            <th>${g.message(code: "project.name")}</th>
            <th>current State</th>
            <th>currently edited by</th>
            <th>created by</th>
            <th>opened date</th>
            <th>last updated</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="projectRequest" in="${projectRequests}">
            <tr>
                <td>
                    <g:link action="view" id="${projectRequest.id}">${projectRequest.name}</g:link>
                </td>
                <td>
                    ${g.message(code: projectRequest.stateDisplayName)}
                </td>
                <td>
                    <g:if test="${projectRequest.currentOwner}">
                        ${projectRequest.currentOwner}
                    </g:if>
                    <g:else>
                        -
                    </g:else>
                </td>
                <td>
                    ${projectRequest.requester}
                </td>
                <td>
                    ${projectRequest.dateCreated}
                </td>
                <td>
                    ${projectRequest.lastUpdated}
                </td>
            </tr>
        </g:each>
        </tbody>
    </table>
</g:if>
<g:else>
    <ul>
        <li>${g.message(code: "projectRequest.none")}</li>
    </ul>
</g:else>
</body>
</html>
