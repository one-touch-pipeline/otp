<div id="refreshBox">
    <span class="enable refreshButton" style="display: ${enabled ? 'none' : 'inline' }"><g:link controller="refresh" action="enable"><g:message code="otp.refresh.enable"/></g:link></span>
    <span class="disable refreshButton" style="display: ${enabled ? 'inline' : 'none' }"><g:link controller="refresh" action="disable"><g:message code="otp.refresh.disable"/></g:link></span>
</div>
<r:script>
$(function () {
    $.otp.autorefresh.setup(${enabled});
});
</r:script>