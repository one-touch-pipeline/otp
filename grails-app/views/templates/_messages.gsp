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

<%@ page import="de.dkfz.tbi.otp.FlashMessage; java.text.MessageFormat; org.springframework.validation.Errors" %>
%{--
This template shows messages stored in the `flash` map.
To use it, store a `FlashMessage` object in `flash.message`.

--}%
<g:if test="${flash.message && flash.message instanceof FlashMessage}">
    <div id="infoBox" class="info-box"><div class="${(flash.message.errorObject || flash.message.errorList) ? "errors" : "message"}">
        <div class="close-info-box"><button onclick="$(this).parent().parent().remove();"></button></div>
        <div>
            ${flash.message.message}<br>
            <g:if test="${flash.message.errorObject}">
                <g:if test="${(flash.message.errorObject as Errors).errorCount == 1}">
                    ${g.message(code: "default.message.error")}
                </g:if>
                <g:else>
                    ${g.message(code: "default.message.errors", args: [(flash.message.errorObject as Errors).errorCount])}
                </g:else>
                <br>
                <ul>
                    <g:each in="${(flash.message.errorObject as Errors).allErrors}" var="err">
                        <li>${g.message(error: err)}</li>
                    </g:each>
                </ul>
            </g:if>
            <g:elseif test="${flash.message.errorList}">
                <ul>
                    <g:each in="${flash.message.errorList}" var="error">
                        <li>${error}</li>
                    </g:each>
                </ul>
            </g:elseif>
        </div>

        <div style="clear: both;"></div>
    </div>

    </div>
</g:if>
