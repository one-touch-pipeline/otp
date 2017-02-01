<div class="edit-switch edit-switch-new-free-text-values" style="display:inline-block">
    <span class="edit-switch-editor" style="display: none">
        <input type="hidden" name="target" value="${link}"/>
        <g:set var="i" value="${0}"/>
        <g:each var="field" in="${textFields}">
            <label>${labels[i++]}:
                <input type="text" name="${field}"/>
            </label>
        </g:each>
        <g:each var="checkBox" in="${checkBoxes}">
            <label>${labels[i++]}:
                <g:checkBox name="${checkBox.getKey()}" checked="${checkBox.getValue()}"/>
            </label>
        </g:each>
        <g:each var="dropDown" in="${dropDowns}">
            <label>${labels[i++]}:
                <g:select name="${dropDown.getKey()}" from="${dropDown.getValue()}"/>
            </label>
        </g:each>
        <button class="buttons save"><g:message code="default.button.save.label"/></button>
        <button class="buttons cancel"><g:message code="default.button.cancel.label"/></button>
    </span>
    <span class="edit-switch-label"><button class="add js-edit"><g:message code="default.new"/></button>
    </span>
</div>
