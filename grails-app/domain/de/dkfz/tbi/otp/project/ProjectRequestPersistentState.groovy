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
package de.dkfz.tbi.otp.project

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.Entity

@ManagedEntity
class ProjectRequestPersistentState implements Entity {

    /** beanNames are provided by the classes implementing {@link de.dkfz.tbi.otp.project.projectRequest.ProjectRequestState} **/
    String beanName
    User currentOwner

    static hasMany = [
            usersThatNeedToApprove: User,
            usersThatAlreadyApproved: User,
    ]

    static Closure mapping = {
        usersThatNeedToApprove column: "project_request_persistent_state_id",
                joinTable: "project_request_persistent_state_users_that_need_to_approve",
                fetch: 'join' //this might be changed to lazy: false in a later grails version
        usersThatAlreadyApproved column: "project_request_persistent_state_id",
                joinTable: "project_request_persistent_state_users_that_already_approved",
                fetch: 'join' //this might be changed to lazy: false in a later grails version
        currentOwner index: "project_request_persistent_state_current_owner_idx"
    }

    static constraints = {
        beanName blank: false
        currentOwner nullable: true
    }
}
