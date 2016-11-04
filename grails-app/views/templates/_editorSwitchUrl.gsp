<div class="edit-switch edit-switch-url">
    <p class="edit-switch-editor" style="display: none">
        <input type="hidden" name="target" value="${link}"/>
        <input type="text" name="value" class="edit-switch-input" value="${value}"/>
        <button class="buttons save"><g:message code="default.button.update.label"/></button>
        <button class="buttons cancel"><g:message code="default.button.cancel.label"/></button>
    </p>
    <p class="edit-switch-label">
        <span class="wordBreak"><g:link url="${url}">${value}</g:link></span>
        <button class="edit-button-left js-edit">&nbsp;</button>
    </p>
</div>
