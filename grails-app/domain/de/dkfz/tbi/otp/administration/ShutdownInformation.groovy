package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.Entity

/**
 * ShutdownInformation holds information about a regular shutdown.
 *
 * It stores the when, who and why about the regular shutdown. It is
 * possible to cancel a shutdown process in which case this is stored as well.
 */
class ShutdownInformation implements Entity {
    /**
     * The User who initiated the Shutdown process
     */
    User initiatedBy
    /**
     * When the Shutdown process was started
     */
    Date initiated
    /**
     * A reason why the shutdown was initiated.
     */
    String reason
    /**
     * When the Shutdown process finished. That timestamp reflects the shutdown point
     * as close as possible. It is null till the server shutdown is finally started.
     */
    Date succeeded
    /**
     * The User who canceled the shutdown process. Canceling the shutdown is optional.
     * If the shutdown succeeded it is not possible to cancel the process any more
     * and vice versa.
     */
    User canceledBy
    /**
     * The timepoint when the Shutdown process had been canceled by the canceledBy User.
     */
    Date canceled

    static constraints = {
        initiatedBy(nullable: false)
        initiated(nullable: false)
        succeeded(nullable: true, validator: { Date date, ShutdownInformation info ->
            if (!date) {
                return true
            }
            if (info.canceled || info.canceledBy) {
                // succeeded may not be set if info is canceled
                return false
            }
            if (info.initiated > date) {
                // initiated has to be before succeeded date
                return false
            }
            return true
        })
        canceledBy(nullable: true, validator: { User user, ShutdownInformation info ->
            if (!user) {
                return true
            }
            if (info.succeeded) {
                // canceled may not be set if info has succeeded
                return false
            }
            if (!info.canceled) {
                // if it's canceled we need a date
                return false
            }
            return true
        })
        canceled(nullable: true, validator: { Date date, ShutdownInformation info ->
            if (!date) {
                return true
            }
            if (info.succeeded) {
                // canceled may not be set if info has succeeded
                return false
            }
            if (!info.canceledBy) {
                // if it's canceled we need a user
                return false
            }
            if (info.initiated > date) {
                // initiated has to be before canceled date
                return false
            }
            return true
        })
    }
}
