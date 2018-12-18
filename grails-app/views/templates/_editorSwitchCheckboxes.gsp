<div class="edit-switch edit-switch-checkboxes">
    <p class="edit-switch-editor" style="display: none">
        <input type="hidden" name="target" value="${link}"/>
        <g:each in="${availableValues}" var="c" status="i">
            <g:checkBox name="${c}" value="${c}" checked="${selectedValues.contains(c)}" data-checked="${selectedValues.contains(c)}"/><label for="${c}">${c}</label>
        </g:each>
        <button class="save"><g:message code="default.button.update.label"/></button>
        <button class="cancel"><g:message code="default.button.cancel.label"/></button>
    </p>
    <p class="edit-switch-label"><span class="wordBreak">${value ?: g.message(code: "editorSwitch.noneSelected")}</span><button class="edit js-edit">&nbsp;</button></p>
</div>
