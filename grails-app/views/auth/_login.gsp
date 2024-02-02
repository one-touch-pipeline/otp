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

<div id="loginBox" style="position: absolute;">
    <div id="loginFormBox" style="position: absolute;">
        <g:if test="${!oidcEnabled}">
            <form id="loginForm" method="POST" action="${createLink(controller: 'authenticate', action: '')}">

                <input name="username" id="account" oninput="this.value = this.value.toLowerCase()"
                       placeholder="${g.message(code: "security.login.username.label")}" value="${username}" required>
                <input type="password" name="password" id="password" placeholder="${g.message(code: "security.login.password.label")}" required><br/>
                <input type="hidden" name="target" value="${target}"/>
                <input id="loginButton" type="submit" value="${g.message(code: "security.login.button")}"/>
            </form>
        </g:if>
        <g:else>
            <a href="${createLink(uri: "${otp.serverUrl()}/oauth2/authorization/keycloakLogin")}">
                <input id="oidcLoginButton" type="submit" value="${g.message(code: "security.login.button")}"/>
            </a>
        </g:else>
    </div>
</div>
