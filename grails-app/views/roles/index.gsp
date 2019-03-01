<html>
<head>
    <meta name="layout" content="main"/>
    <title>%{--
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

<g:message code="otp.menu.roles"/></title>
</head>
<body>
    <div class="body">
        <h3><g:message code="roles.rolesAndGroups.header"/></h3>
        <g:each in="${roleLists}" var="roleList">
            <g:each in="${roleList}" var="roleAndUsers">
                <h4>${roleAndUsers.role.authority}</h4>
                <g:if test="${roleAndUsers.users.isEmpty()}">
                    ${g.message(code: "roles.noMembers")}
                </g:if>
                <g:else>
                    ${g.message(code: "roles.members")}
                    <ul>
                        <g:each in="${roleAndUsers.users}" var="user">
                            <li>${user.username} (${user.realName})</li>
                        </g:each>
                    </ul>
                </g:else>
            </g:each>
            <g:if test="${roleList != roleLists.last()}">
                <hr>
            </g:if>
        </g:each>
    </div>
</body>
</html>
