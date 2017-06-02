<div class="edit-switch edit-switch-text-area">
    <p class="edit-switch-editor" style="display: none">
        <input type="hidden" name="target" value="${link}"/>
        <textarea name="value" class="edit-switch-input" rows="5" cols="150" style="width: auto;">${value}</textarea>
        <button class="buttons save"><g:message code="default.button.update.label"/></button>
        <button class="buttons cancel"><g:message code="default.button.cancel.label"/></button>
    </p>
    <p class="edit-switch-label">
        <span class="wordBreak keep-whitespace">${value}</span>
        <button class="edit-button-left js-edit">&nbsp;</button>
    </p>
</div>
