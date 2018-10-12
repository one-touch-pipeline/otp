<%@ page import="java.text.MessageFormat; org.springframework.validation.Errors" %>
<g:if test="${flash.message}">
    <div id="infoBox"><div class="${flash.errors ? "errors" : "message"}">
        <div class="close"><button onclick="$(this).parent().parent().remove();"></button></div>
        <div>
            ${flash.message}<br>
            <g:if test="${flash.errors}">
                <g:if test="${flash.errors instanceof Errors}">
                    There are ${(flash.errors as Errors).errorCount} error(s):<br>
                    <ul>
                        <g:each in="${(flash.errors as Errors).allErrors}" var="err">
                            <li>${MessageFormat.format(err.defaultMessage, err.arguments)}: ${err.code}</li>
                        </g:each>
                    </ul>
                </g:if>
                <g:elseif test="${flash.errors instanceof List}">
                    <ul>
                        <g:each in="${flash.errors}" var="error">
                            <li>${error}</li>
                        </g:each>
                    </ul>
                </g:elseif>
                <g:else>
                    <ul>
                        <li>${flash.errors}</li>
                    </ul>
                </g:else>
            </g:if>
        </div>

        <div style="clear: both;"></div>
    </div>

    </div>
</g:if>
