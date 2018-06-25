import de.dkfz.tbi.otp.administration.*

class PrivacyPolicyFilters {
    UserService userService

    static final String FILTER_APPLIED = "__otp_privacy_policy_filter_applied"

    def filters = {
        all(invert: true, controller:'privacyPolicy', action:'accept') {
            before = {
                // ensure that filter is only applied once per request
                if (request.getAttribute(FILTER_APPLIED)) {
                    return
                }

                if (!userService.isPrivacyPolicyAccepted()) {
                    request.setAttribute(FILTER_APPLIED, true)
                    forward(controller: "privacyPolicy", action: "index")
                }
            }
        }
    }
}
