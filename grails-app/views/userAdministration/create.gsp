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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <title><g:message code="user.administration.createUser"/></title>
        <asset:javascript src="modules/userAdministration"/>
    </head>
<body>
    <div class="body">
        <ul>
            <li class="button"><g:link action="index"><g:message code="user.administration.backToOverview"/></g:link></li>
        </ul>
        <form id="create-user-form" action="createUser">
        <table class="form">
            <tbody>
                <tr>
                    <td><label for="create_user_name"><g:message code="user.administration.ui.username"/></label></td>
                    <td><input type="text" name="username" id="create_user_name"/></td>
                    <td>&nbsp;</td>
                </tr>
                <tr>
                    <td><label for="create_user_email"><g:message code="user.administration.ui.email"/></label></td>
                    <td><input type="text" name="email" id="create_user_email"/></td>
                    <td>&nbsp;</td>
                </tr>
            </tbody>
        </table>
        <h2><g:message code="user.administration.ui.heading.rolesAndGroups"/></h2>
        <ul>
            <g:each in="${roles}">
                <li><input type="checkbox" name="role" value="${it.id}"/>${it.authority}</li>
            </g:each>
        </ul>
        <div class="buttons">
            <input type="submit" value="${g.message(code: 'user.administration.createUser')}"/>
        </div>
        </form>
    </div>
    <asset:script type="text/javascript">
        $(function () {
            $.otp.userAdministration.create.register();
        });
    </asset:script>
</body>
</html>
