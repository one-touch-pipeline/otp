<div class="edit-switch edit-switch-new-value" style="display:inline-block">
    <p class="edit-switch-editor" style="display: none">
        <input type="hidden" name="target" value="${link}"/>
        <g:select name="dropdown" from="${values}" value="${value}" class="dropDown" />
        <button class="buttons save"><g:message code="default.button.save.label"/></button>
        <button class="buttons cancel"><g:message code="default.button.cancel.label"/></button>
    </p>
    <p class="edit-switch-label"><button class="buttons insert js-edit"><g:message code="default.new"/></button></p>
</div>
