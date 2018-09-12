import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

class PrivacyPolicyController {
    static allowedMethods = [
            index : "GET",
            accept: "POST",
    ]

    UserService userService


    def index() {
        String contactDataDataPrivacyOfficer = ProcessingOptionService.findOptionSafe(GUI_CONTACT_DATA_POSTAL_ADDRESS_DATA_PROTECTION_OFFICER, null, null)

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
            flash.message = g.message(code: "privacyPolicy.message.fail")
            flash.errors = g.message(code: "privacyPolicy.message.error")
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
