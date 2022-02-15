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
package de.dkfz.tbi.otp.administration

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Entity

/**
 * ShutdownInformation holds information about a regular shutdown.
 *
 * It stores the when, who and why about the regular shutdown. It is
 * possible to cancel a shutdown process in which case this is stored as well.
 */
@ManagedEntity
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
            return !(info.succeeded == null && info.canceled == null &&
                    CollectionUtils.atMostOneElement(ShutdownInformation.findAllBySucceededIsNullAndCanceledIsNull()) != info)
        })
        canceledBy(nullable: true, validator: { User user, ShutdownInformation info ->
            if (!user) {
                return true
            }
            if (info.succeeded) {
                // canceled may not be set if info has succeeded
                return false
            }
            // if it's canceled we need a date
            return info.canceled != null
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
            // initiated has to be before canceled date
            return info.initiated <= date
        })
    }
}
