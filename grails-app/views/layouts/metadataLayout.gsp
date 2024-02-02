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

<!doctype html>
<html lang="en" class="${otp.environmentName()}">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title><g:layoutTitle default="OTP"/></title>
    <script type="text/javascript" src="${otp.serverUrl()}/webjars/jquery/${otp.assetVersion([name: 'jquery'])}/jquery.js"></script>
    <script type="text/javascript" src="${otp.serverUrl()}/webjars/datatables/1.13.5/js/jquery.dataTables.js"></script>
    <script type="text/javascript" src="${otp.serverUrl()}/webjars/bootstrap/5.3.0/js/bootstrap.bundle.js"></script>
    <asset:javascript src="modules/defaultPageDependencies.js"/>
    <asset:stylesheet src="modules/metadataImport.css"/>
    <g:layoutHead/>
</head>
<body>
    <g:layoutBody/>
    <asset:deferredScripts/>
</body>
</html>
