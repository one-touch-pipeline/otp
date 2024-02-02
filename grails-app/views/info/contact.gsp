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
    <meta name="layout" content="info"/>
    <title><g:message code="info.contact.title" /></title>
</head>

<body>
    <h1><g:message code="info.contact.title" /></h1>
    <p>${g.message(code: "info.contact.operatedBy")}</p>
    <p class="keep-whitespace">${contactDataOperatedBy}</p>
    <h2>${g.message(code: "info.contact.personInCharge")}</h2>
    <p class="keep-whitespace">${contactDataPersonInCharge}</p>
    <h2>${g.message(code: "info.contact.postalAddress")}</h2>
    <p class="keep-whitespace">${contactDataPostalAddress}</p>
    <h2>${g.message(code: "info.contact.support")}</h2>
    <p class="keep-whitespace"><a href="mailto:${contactDataSupportEmail}">${contactDataSupportEmail}</a></p>
</body>
</html>
