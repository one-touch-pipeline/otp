<%@ page import="de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService" %>

<g:set var="PIWIK_URL" value="${ProcessingOptionService.findOption("PIWIK_URL", null, null)}"/>
<g:set var="SITE_ID" value="${(ProcessingOptionService.findOption("SITE_ID", null, null)) as Integer}"/>

<g:if test="${(ProcessingOptionService.findOption("TRACKING", null, null))?.toBoolean()}">
    <script type="text/javascript">
        var _paq = _paq || [];
        (function () {
            _paq.push(['setTrackerUrl', '${PIWIK_URL}piwik.php']);
            _paq.push(['setSiteId', ${SITE_ID}]);
            _paq.push(['setCustomVariable', 1, 'Role', '${grails.plugin.springsecurity.SpringSecurityUtils.ifAllGranted("ROLE_ADMIN") ? "Admin" : "User"}', 'page']);
            _paq.push(['enableLinkTracking']);
            _paq.push(['trackPageView']);
            var d = document, g = d.createElement('script'), s = d.getElementsByTagName('script')[0];
            g.type = 'text/javascript';
            g.async = true;
            g.defer = true;
            g.src = '${PIWIK_URL}piwik.js';
            s.parentNode.insertBefore(g, s);
        })();
    </script>
    <noscript><p><img src="${PIWIK_URL}piwik.php?idsite=${SITE_ID}" style="border:0;" alt=""/></p></noscript>
</g:if>
