<div class="showChangeLog">
    <input type="hidden" value="${link}"/>
    <button title="${g.message(code: 'showChangeLog.button.title')}">&nbsp;</button>
    <div style="display: none;" title="${g.message(code: 'showChangeLog.dialog.title')}">
        <table>
            <thead>
            <tr>
                <th><g:message code="showChangeLog.table.head.timestamp"/></th>
                <th><g:message code="showChangeLog.table.head.from"/></th>
                <th><g:message code="showChangeLog.table.head.to"/></th>
                <th><g:message code="showChangeLog.table.head.source"/></th>
                <th><g:message code="showChangeLog.table.head.comment"/></th>
            </tr>
            </thead>
            <tbody></tbody>
        </table>
    </div>
</div>
