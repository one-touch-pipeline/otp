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
    <title><g:message code="triggerAlignment.title"/></title>
    <asset:javascript src="pages/triggerAlignment/index/functions.js"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <nav class="navbar navbar-light bg-light">
        <span class="navbar-brand mb-0 h1"><g:message code="triggerAlignment.title"/></span>
    </nav>

    <div class="mt-3">
        <g:render template="/triggerAlignment/components/inputArea" model="[tabs: ['project', 'pid', 'seqTrackId', 'ilse', 'multiInput', 'bam'], seqTypes: seqTypes]"/>
    </div>

    <div class="mt-3">
        <g:render template="/triggerAlignment/components/warnArea" model="[warnings: warnings]"/>
    </div>

    <div class="mt-3">
        <g:render template="/triggerAlignment/components/checkArea"/>
    </div>

    <div class="mt-3">
        <g:render template="/triggerAlignment/components/infoArea" model="[info: info]"/>
    </div>

    <nav class="navbar navbar-light bg-light mt-3">
        <div>
            <div class="form-check">
                <input type="checkbox" id="ignoreSeqPlatformGroup">
            <label for="ignoreSeqPlatformGroup"><g:message code="triggerAlignment.input.checkbox.ignoreSeqPlatformGroup"/></label>
            </div>

            <div class="form-check">
                <div class="form-check form-check-inline">
            <input class="form-check-input" type="radio" name="withdrawBamFiles" checked id="withdrawBamFiles2" value="false">
            <label class="form-check-label" for="withdrawBamFiles2">
                        <g:message code="triggerAlignment.input.radio.missingAlignments"/>
                    </label>
                </div>

                <div class="form-check form-check-inline">
                    <input class="form-check-input" type="radio" name="withdrawBamFiles" id="withdrawBamFiles1" value="true">
                    <label class="form-check-label" for="withdrawBamFiles1">
                        <g:message code="triggerAlignment.input.radio.allAlignments"/>
                    </label>
                </div>
            </div>
        </div>
    </nav>
    <nav class="navbar navbar-light bg-light mt-3">
        <div class="nav-item">
            <button id="triggerAlignmentButton" class="btn btn-primary nav-item mr-2" onclick="$.otp.triggerAlignment.trigger(this)">
                <g:message code="triggerAlignment.triggerButton"/>
            </button>
        </div>
    </nav>

    <div class="mt-3">
        <g:render template="/triggerAlignment/components/resultArea"/>
    </div>
</div>
</body>
</html>
