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

import grails.plugin.springsecurity.annotation.Secured

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

@Secured('permitAll')
class PrivacyPolicyController {
    static allowedMethods = [
            index : "GET",
            accept: "POST",
    ]

    UserService userService
    ProcessingOptionService processingOptionService

    def index() {
        String contactDataDataPrivacyOfficer = processingOptionService.findOptionAsString(OptionName.GUI_CONTACT_DATA_POSTAL_ADDRESS_DATA_PROTECTION_OFFICER)

        boolean disableMenu = true
        boolean disableAccept = true

        if (userService.isPrivacyPolicyAccepted()) {
            disableMenu = false
            disableAccept = false
        }

        return [
                contactDataDataPrivacyOfficer: contactDataDataPrivacyOfficer,
                disableMenu                  : disableMenu,
                disableAccept                : disableAccept,
        ]
    }

    def accept(AcceptCommand cmd) {
        if (cmd.accept) {
            userService.acceptPrivacyPolicy()
        } else {
            flash.message = new FlashMessage(
                    g.message(code: "privacyPolicy.message.fail") as String,
                    [g.message(code: "privacyPolicy.message.error") as String],
            )
        }

        redirect uri: cmd.redirect
    }
}

class AcceptCommand {
    boolean accept
    String redirect

    static constraints = {
        redirect(nullable: false, validator: { String val ->
            val.startsWith("/")
        })
    }
}
