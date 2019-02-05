%{--
  - Copyright 2011-2019 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%

<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils; de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName; de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService" %>

<g:set var="PIWIK_URL" value="${piwikUrl}"/>
<g:set var="SITE_ID" value="${piwikSiteId}"/>

<g:if test="${piwikEnabled}">
    <script type="text/javascript">
        var _paq = _paq || [];
        (function () {
            _paq.push(['setTrackerUrl', '${PIWIK_URL}piwik.php']);
            _paq.push(['setSiteId', ${SITE_ID}]);
            _paq.push(['setCustomVariable', 1, 'Role', '${SpringSecurityUtils.ifAllGranted("ROLE_ADMIN") ? "Admin" : "User"}', 'page']);
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
