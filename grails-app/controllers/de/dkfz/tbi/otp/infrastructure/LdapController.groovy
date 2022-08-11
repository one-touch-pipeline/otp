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
package de.dkfz.tbi.otp.infrastructure

import grails.converters.JSON
import org.springframework.security.access.annotation.Secured

import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails

@Secured('isFullyAuthenticated()')
class LdapController {

    static allowedMethods = [
            getUserSearchSuggestions: "GET",
    ]

    UserService userService
    IdentityProvider identityProvider

    /**
     * Return a json list of all users from the ldap system which fit the search criteria.
     * At the top of the list are those ldap users which are also already otp users.
     *
     * @param cmd search criteria
     * @return list of all users from the ldap system which fit the search criteria
     */
    def getUserSearchSuggestions(UserSearchSuggestionsCommand cmd) {
        if (cmd.searchString.length() < 3) {
            return render([[minLength: true]] as JSON)
        }

        List<IdpUserDetails> idpUsers = identityProvider.getListOfIdpUserDetailsBySearchString(cmd.searchString)

        Set<String> otpUserNames = userService.getAllUserNamesOfOtpUsers(idpUsers*.username)

        List<IdpUserDetails> otpIdpUsers = idpUsers.findAll { IdpUserDetails usrDetails ->
            usrDetails.username in otpUserNames
        }

        idpUsers.removeAll(otpIdpUsers)
        idpUsers.addAll(0, otpIdpUsers)

        render(idpUsers as JSON)
    }
}

class UserSearchSuggestionsCommand {
    String searchString

    void setValue(String value) {
        this.searchString = value
    }
}
