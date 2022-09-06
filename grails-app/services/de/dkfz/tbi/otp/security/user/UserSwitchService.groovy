/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.security.user

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityUtils

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.PseudoEnvironment
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils

@Transactional
class UserSwitchService {

    ConfigService configService
    UserService userService

    /**
     * Returns the true current user, so if the user is switched it returns not the switched user but
     * the user that switched to the current user.
     */
    User getUserSwitchInitiator() {
        return isSwitched() ? CollectionUtils.exactlyOneElement(
                User.findAllByUsername(SpringSecurityUtils.switchedUserOriginalUsername)
        ) : userService.currentUser
    }

    boolean isSwitched() {
        return SpringSecurityUtils.isSwitched()
    }

    List<PseudoEnvironment> getWhitelistedEnvironments() {
        return PseudoEnvironment.values() - PseudoEnvironment.PRODUCTION
    }

    boolean isToBeBlockedBecauseOfSwitchedUser() {
        return switched && !(configService.pseudoEnvironment in whitelistedEnvironments)
    }

    void ensureNotSwitchedUser() throws SwitchedUserDeniedException {
        if (isToBeBlockedBecauseOfSwitchedUser()) {
            throw new SwitchedUserDeniedException("User switching is not allowed for switched users")
        }
    }
}
