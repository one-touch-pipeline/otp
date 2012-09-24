<div class="sample-identifiers-field">
    <p class="sample-identifiers-field-editor" style="display: none">
        <input type="hidden" name="target" value="${link}"/>
    </p>
    <table class="newSample">
        <tr class="sample">
            <td valign="top" class="value ${hasErrors(bean:individual,field:'sampleIdentifier','errors')}">
                <input type="text" name="sampleIdentifier" />
            </td>
            <td>
                <div class="newSampleIdentifier">
                    <button class="buttons plus"><g:message code="individual.insert.newSampleIdentifier"/></button>
                    <button class="buttons minus" style="display: none"><g:message code="individual.insert.removeSampleIdentifier"/></button>
                </div>
            </td>
        </tr>
    </table>
    <p class="sample-identifiers-field-label"><span class="wordBreak">${value}</span><button class="edit">&nbsp;</button></p>
</div>
