<div class="edit-switch edit-switch-toggle">
    <p class="edit-switch-editor" style="display: none">
        <input type="hidden" name="target" value="${link}"/>
        <input type="hidden" name="value" value="${value}"/>
        <span class="icon-${value}"></span><br>
        <button class="toggle"><g:message code="default.button.toggle.label"/></button><br>
        <button class="cancel"><g:message code="default.button.cancel.label"/></button>
    </p>
    <p class="edit-switch-label"><span class="wordBreak icon-${value}"></span><button class="edit js-edit">&nbsp;</button></p>
</div>
