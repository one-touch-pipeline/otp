<%@ page import="de.dkfz.tbi.otp.FlashMessage; java.text.MessageFormat; org.springframework.validation.Errors" %>
%{--
This template shows messages stored in the `flash` map.
To use it, store a `FlashMessage` object in `flash.message`.

--}%
<g:if test="${flash.message && flash.message instanceof FlashMessage}">
    <div id="infoBox"><div class="${(flash.message.errorObject || flash.message.errorList) ? "errors" : "message"}">
        <div class="close"><button onclick="$(this).parent().parent().remove();"></button></div>
        <div>
            ${flash.message.message}<br>
            <g:if test="${flash.message.errorObject}">
                There are ${(flash.message.errorObject as Errors).errorCount} error(s):<br>
                <ul>
                    <g:each in="${(flash.message.errorObject as Errors).allErrors}" var="err">
                        <li>${MessageFormat.format(err.defaultMessage, err.arguments)}: ${err.code}</li>
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
