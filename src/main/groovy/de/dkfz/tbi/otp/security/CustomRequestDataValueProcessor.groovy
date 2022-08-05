/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.security

import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.servlet.support.RequestDataValueProcessor

import javax.servlet.http.HttpServletRequest
import java.util.regex.Pattern

/**
 * This is a copy of {@link org.springframework.security.web.servlet.support.csrf.CsrfRequestDataValueProcessor}
 * but the GET was removed from the DISABLE_CSRF_TOKEN_PATTERN. Otherwise the form do not get the token.
 */
@SuppressWarnings(['UnnecessarySemicolon', 'UnnecessaryPublicModifier', 'UnusedMethodParameter',
        'SpaceAfterComma', 'MissingOverrideAnnotation', 'ExplicitCallToEqualsMethod', 'FieldName',
        'PrivateFieldCouldBeFinal', 'EmptyMethod', 'NoDef', 'MethodName', 'MethodReturnTypeRequired'])
class CustomRequestDataValueProcessor implements RequestDataValueProcessor {
    private Pattern DISABLE_CSRF_TOKEN_PATTERN = Pattern.compile("(?i)^(HEAD|TRACE|OPTIONS)\$");
    private String DISABLE_CSRF_TOKEN_ATTR = "DISABLE_CSRF_TOKEN_ATTR";

    public CsrfRequestDataValueProcessor() {
    }

    public String processAction(HttpServletRequest request, String action) {
        return action;
    }

    public String processAction(HttpServletRequest request, String action, String method) {
        if (method != null && this.DISABLE_CSRF_TOKEN_PATTERN.matcher(method).matches()) {
            request.setAttribute(this.DISABLE_CSRF_TOKEN_ATTR, Boolean.TRUE);
        } else {
            request.removeAttribute(this.DISABLE_CSRF_TOKEN_ATTR);
        }

        return action;
    }

    public String processFormFieldValue(HttpServletRequest request, String name, String value, String type) {
        return value;
    }

    public Map<String, String> getExtraHiddenFields(HttpServletRequest request) {
        if (Boolean.TRUE.equals(request.getAttribute(this.DISABLE_CSRF_TOKEN_ATTR))) {
            request.removeAttribute(this.DISABLE_CSRF_TOKEN_ATTR);
            return Collections.emptyMap();
        }
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.name);
        if (token == null) {
            return Collections.emptyMap();
        }
        Map<String, String> hiddenFields = new HashMap(1);
        hiddenFields.put(token.parameterName, token.token);
        return hiddenFields;
    }

    public String processUrl(HttpServletRequest request, String url) {
        return url;
    }
}
