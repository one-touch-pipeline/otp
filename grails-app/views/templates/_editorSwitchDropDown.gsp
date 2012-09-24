<div class="edit-switch-drop-down">
    <p class="edit-switch-drop-down-editor" style="display: none">
        <input type="hidden" name="target" value="${link}"/>
        <g:select name="dropdown" from="${values}" value="${value}" class="dropDown" />
        <button class="save"><g:message code="default.button.update.label"/></button>
        <button class="cancel"><g:message code="default.button.cancel.label"/></button>
    </p>
    <p class="edit-switch-drop-down-label"><span class="wordBreak">${value}</span><button class="edit">&nbsp;</button></p>
</div>
