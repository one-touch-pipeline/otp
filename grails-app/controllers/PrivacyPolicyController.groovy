import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

class PrivacyPolicyController {
    static allowedMethods = [
            index : "GET",
            accept: "POST",
    ]

    UserService userService
    ProcessingOptionService processingOptionService


    def index() {
        String contactDataDataPrivacyOfficer = processingOptionService.findOptionAsString(GUI_CONTACT_DATA_POSTAL_ADDRESS_DATA_PROTECTION_OFFICER)

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
