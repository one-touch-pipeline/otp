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
<g:applyLayout name="common">
    <head>
        <title><g:layoutTitle default="OTP"/></title>
        <script type="text/javascript" src="${otp.serverUrl()}/webjars/jquery/${otp.assetVersion([name: 'jquery'])}/jquery.js"></script>
        <script type="text/javascript" src="${otp.serverUrl()}/webjars/chart.js/4.4.2/dist/chart.umd.js"></script>
        <script type="text/javascript" src="${otp.serverUrl()}/webjars/chartjs-plugin-datalabels/2.2.0/dist/chartjs-plugin-datalabels.js"></script>
        <script type="text/javascript" src="${otp.serverUrl()}/webjars/datatables/1.13.5/js/jquery.dataTables.js"></script>
        <script type="text/javascript" src="${otp.serverUrl()}/webjars/datatables/1.13.5/js/dataTables.bootstrap5.js"></script>
        <script type="text/javascript" src="${otp.serverUrl()}/webjars/datatables-plugins/1.13.6/pagination/select.js"></script>
        <script type="text/javascript" src="${otp.serverUrl()}/webjars/bootstrap/5.3.3/js/bootstrap.bundle.js"></script>
        <asset:javascript src="modules/application.js"/>
        <script type="text/javascript" src="${otp.serverUrl()}/webjars/datatables.net-scroller/js/dataTables.scroller.js"></script>
        <asset:stylesheet src="modules/application.css"/>
        <script type="text/javascript" src="${otp.serverUrl()}/webjars/datatables-buttons/2.4.1/js/dataTables.buttons.js"></script>
        <script type="text/javascript" src="${otp.serverUrl()}/webjars/datatables-buttons/2.4.1/js/buttons.bootstrap5.js"></script>
        <script type="text/javascript" src="${otp.serverUrl()}/webjars/datatables-buttons/2.4.1/js/buttons.html5.js"></script>
        <asset:javascript src="dataTable.js"/>
        <g:layoutHead/>
    </head>

    <body>
        <g:render template="/layouts/messages"/>
        <g:layoutBody/>
    </body>
</g:applyLayout>
