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
    <title><g:message code="user.administration.index.title"/></title>
    <meta name="layout" content="main" />
    <asset:javascript src="pages/userAdministration/index/index.js"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>

        <h1><g:message code="user.administration.index.header"/></h1>
        <div>
            <form id="switch-user-form" action="${request.contextPath}/login/impersonate" method="POST">
                <input type="hidden" name="username"/>
            </form>
            <div class="otpDataTables">
                <otp:dataTable codes="${[
                    'user.administration.user.fields.username',
                    'user.administration.user.fields.realName',
                    'user.administration.user.fields.email',
                    'user.administration.user.fields.plannedDeactivationDate',
                    'user.administration.user.fields.enabled',
                    'user.administration.user.fields.acceptedPrivacyPolicy',
                    'otp.blank'
                ]}" id="userTable"/>
            </div>
        </div>
    </div>
    <asset:script type="text/javascript">
        $(function() {
            $.otp.userAdministration.loadUserList();
        });
    </asset:script>
</body>
</html>
