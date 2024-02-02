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

<html>
<head>
    <meta name="layout" content="main" />
    <title><g:message code="default.no.project"/></title>
</head>

<body>
    <div class="body">
        <sec:ifNotGranted roles="ROLE_ADMIN">
            <h1><g:message code="default.no.project"/></h1>
            <p>${g.message(code: "default.no.project.explain")}</p>
            <ul>
                ${raw(gainAccess)}
            </ul>
        </sec:ifNotGranted>

        <sec:ifAnyGranted roles="ROLE_ADMIN">
            <p><g:link controller="projectCreation">${g.message(code: "default.no.project.admin")}</g:link></p>
        </sec:ifAnyGranted>
    </div>
</body>
</html>
