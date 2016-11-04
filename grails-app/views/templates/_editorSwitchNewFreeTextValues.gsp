<div class="edit-switch edit-switch-new-free-text-values" style="display:inline-block">
    <span class="edit-switch-editor" style="display: none">
        <input type="hidden" name="target" value="${link}"/>
        <g:each var="field" in="${fields}">
            ${field}: <input type="text" name="${field}"/>
        </g:each>
        <g:each var="checkBox" in="${checkBoxes}">
            <g:if test="${!checkBox.getValue()}">
                ${checkBox.getKey()}:
                <input type="checkbox" name="${checkBox.getKey()}"/>
            </g:if>
        </g:each>
        <g:each var="dDown" in="${dropDowns}">
            ${dDown.getKey()}:
            <g:select name="${dDown.getKey()}" from="${dDown.getValue()}"/>
        </g:each>
        <button class="buttons save"><g:message code="default.button.save.label"/></button>
        <button class="buttons cancel"><g:message code="default.button.cancel.label"/></button>
    </span>
    <span class="edit-switch-label"><button class="add js-edit"><g:message code="default.new"/></button>
    </span>
</div>
