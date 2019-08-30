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

<html>
<head>
    <meta name="layout" content="info"/>
    <title><g:message code="info.numbers.title" /></title>
    <asset:javascript src="modules/graph"/>
</head>

<body>
<g:if test="${projects}">
    <h2><g:message code="info.numbers.title" /></h2>
    <g:message code="info.numbers.text"/>
    <div class="homeGraph" style="clear: both;" >
        <div class="plotContainer">
            <canvas id="projectCountPerDate" width="530" height="380">[No canvas support]</canvas>
        </div>
        <div class="plotContainer">
            <canvas id="laneCountPerDate" width="530" height="380">[No canvas support]</canvas>
        </div>
    </div>
<asset:script type="text/javascript">
    $(function() {
        $.otp.graph.info.init();
    });
</asset:script>
</g:if>
<g:else>
    <h3><g:message code="default.no.project"/></h3>
</g:else>
</body>
</html>
