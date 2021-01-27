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
    <asset:stylesheet src="pages/shutdown/index/styles.less"/>
    <title><g:message code="serverShutdown.title"/></title>
</head>
<body>
    <div class="container-fluid otp-main-container">
        <g:render template="/templates/messages"/>

        <h3><g:message code="serverShutdown.title"/></h3>
        <p><g:message code="serverShutdown.description"/></p>
        <div class="card plan-shutdown-card">
            <div class="card-body">
                <g:if test="${shutdownSucceeded}">
                    <g:message code="serverShutdown.successful"/>
                </g:if>
                <g:else>
                    <g:form action="planShutdown">
                        <div class="form-group">
                            <label for="reasonInput"><g:message code="serverShutdown.reasonLabel"/></label>
                            <input type="text" name="reason" class="form-control" id="reasonInput" aria-describedby="reasonHelp">
                            <small id="reasonHelp" class="form-text text-muted"><g:message code="serverShutdown.reason"/></small>
                        </div>
                        <button type="submit" class="btn btn-primary"><g:message code="serverShutdown.planButton"/></button>
                    </g:form>
                </g:else>
            </div>
        </div>
    </div>
</body>
</html>
