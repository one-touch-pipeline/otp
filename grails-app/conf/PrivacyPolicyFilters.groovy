import de.dkfz.tbi.otp.administration.UserService

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
                request.setAttribute(FILTER_APPLIED, true)

                if (!userService.isPrivacyPolicyAccepted()) {
                    forward(controller: "privacyPolicy", action: "index")
                }
            }
        }
    }
}
