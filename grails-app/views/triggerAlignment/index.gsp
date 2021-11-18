%{--
  - Copyright 2011-2021 The OTP authors
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
    <title><g:message code="triggerAlignment.title"/></title>
</head>

<body>
    <div class="container-fluid otp-main-container">
        <nav class="navbar navbar-light bg-light">
            <span class="navbar-brand mb-0 h1"><g:message code="triggerAlignment.title"/></span>
        </nav>

        <div class="mt-3">
            <g:render template="./components/inputArea" model="[tabs: ['project', 'pid', 'lane', 'ilse'], seqTypes: seqTypes]"/>
        </div>

        <div class="mt-3">
            <g:render template="./components/checkArea"/>
        </div>

        <div class="mt-3">
            <g:render template="./components/warnArea"/>
        </div>

        <nav class="navbar navbar-light bg-light mt-3">
            <button class="btn btn-primary"><g:message code="triggerAlignment.triggerButton"/></button>
        </nav>

        <div class="mt-3">
            <g:render template="./components/resultArea"/>
        </div>
    </div>
</body>
</html>
