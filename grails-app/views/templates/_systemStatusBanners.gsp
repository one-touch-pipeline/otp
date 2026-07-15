%{--
  - Copyright 2011-2026 The OTP authors
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

%{--
  - System status banners shown at the top of every page for administrative users (admins and data managers).
  - The visibility flags are provided by the OptionsInterceptor, so the banners stay hidden for everyone else and
  - on pages that are rendered without a model (e.g. without layout).
  - Bootstrap pages pick up the native `alert alert-warning` style; on non-bootstrap pages those classes are inert
  - and `.otp-system-banner` (systemStatusBanner.less) provides the equivalent styling.
  -
  - A user can dismiss a banner for the current session (remembered in sessionStorage). Since that flag only exists
  - in the browser, the server cannot decide it here; instead the inline script below removes already-dismissed
  - banners synchronously while the page is being parsed, i.e. before they are painted, so there is no flash of a
  - banner that is immediately dismissed again.
  --}%

<g:if test="${autoImportDisabledBannerVisible || workflowSystemDisabledBannerVisible}">
    <div class="otp-system-banner-container">
        <g:if test="${autoImportDisabledBannerVisible}">
            <div class="otp-system-banner alert alert-warning alert-dismissible" role="alert" data-banner-key="autoImport">
                <span class="otp-system-banner-icon">&#x26A0;&#xFE0F;</span>
                <span><g:message code="systemStatusBanner.autoImportDisabled.message"/></span>
                <sec:ifAnyGranted roles="ROLE_ADMIN">
                    <g:link controller="processingOption" action="index" class="otp-system-banner-link alert-link">
                        <g:message code="systemStatusBanner.autoImportDisabled.link"/>
                    </g:link>
                </sec:ifAnyGranted>
                <button type="button" class="otp-system-banner-close"
                        aria-label="${g.message(code: 'systemStatusBanner.close')}">&#x2715;</button>
            </div>
        </g:if>

        <g:if test="${workflowSystemDisabledBannerVisible}">
            <div class="otp-system-banner alert alert-warning alert-dismissible" role="alert" data-banner-key="workflowSystem">
                <span class="otp-system-banner-icon">&#x26A0;&#xFE0F;</span>
                <span><g:message code="systemStatusBanner.workflowSystemDisabled.message"/></span>
                <sec:ifAnyGranted roles="ROLE_ADMIN">
                    <g:link controller="systemStatus" action="index" class="otp-system-banner-link alert-link">
                        <g:message code="systemStatusBanner.workflowSystemDisabled.link"/>
                    </g:link>
                </sec:ifAnyGranted>
                <button type="button" class="otp-system-banner-close"
                        aria-label="${g.message(code: 'systemStatusBanner.close')}">&#x2715;</button>
            </div>
        </g:if>
    </div>

    %{-- Runs inline (not on DOM-ready) so dismissed banners are removed before the browser paints them. --}%
    <script type="text/javascript">
        (function () {
            'use strict';
            var storageKeyPrefix = 'otp.systemStatusBanner.dismissed.';
            var container = document.querySelector('.otp-system-banner-container');
            if (!container) {
                return;
            }

            var removeBanner = function (banner) {
                banner.remove();
                if (container.querySelectorAll('.otp-system-banner').length === 0) {
                    container.remove();
                }
            };

            container.querySelectorAll('.otp-system-banner').forEach(function (banner) {
                var storageKey = storageKeyPrefix + banner.dataset.bannerKey;
                if (window.sessionStorage.getItem(storageKey) === 'true') {
                    removeBanner(banner);
                    return;
                }
                banner.querySelector('.otp-system-banner-close').addEventListener('click', function () {
                    window.sessionStorage.setItem(storageKey, 'true');
                    removeBanner(banner);
                });
            });
        }());
    </script>
</g:if>
