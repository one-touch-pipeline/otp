<script type="text/javascript">
    var _paq = _paq || [];
    (function() {
        var u="[REDACTED]/";
        _paq.push(['setTrackerUrl', u+'piwik.php']);
        _paq.push(['setSiteId', '[REDACTED]']);
        _paq.push(['setCustomVariable', 1, 'Role','${grails.plugin.springsecurity.SpringSecurityUtils.ifAllGranted("ROLE_ADMIN") ? "Admin" : "User"}', 'page']);
        _paq.push(['enableLinkTracking']);
        _paq.push(['trackPageView']);
        var d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];
        g.type='text/javascript'; g.async=true; g.defer=true; g.src=u+'piwik.js'; s.parentNode.insertBefore(g,s);
    })();
</script>
<noscript><p><img src="[REDACTED]/piwik.php?idsite=[REDACTED]" style="border:0;" alt=""/></p></noscript>
