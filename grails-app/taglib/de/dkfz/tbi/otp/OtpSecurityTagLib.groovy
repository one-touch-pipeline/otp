/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp

import de.dkfz.tbi.otp.security.SecurityService

/**
 * This tag lib contains helper tags that deal with security related issues in the GUI.
 */
class OtpSecurityTagLib {
    static namespace = "otpsecurity"

    SecurityService securityService

    /**
     * Disables all submit buttons wrapped inside this tag and adds a warning highlight and tooltip.
     *
     * Only disables the buttons in a real production environment. In other environments it only adds the
     * highlight but leaves the buttons enabled.
     *
     * To be used in conjunction with:
     *   - SecurityService.assertNotSwitchedUser in the respective function to prevent direct submissions
     *   - taglib/NoSwitchedUser.js to actually disable the buttons in the GUI
     */
    def noSwitchedUser = { attrs, body ->
        out << "<div "
        if (securityService.toBeBlockedBecauseOfSwitchedUser) {
            out << "class=\"no-switched-user\">"
            out << "<img src=\"${g.assetPath(src: "warning.png")}\"/> "
            out << "${g.message(code: "error.switchedUserDeniedException.description")}"
        } else {
            out << ">"
        }
        out << body()
        out << "</div>"
    }
}
