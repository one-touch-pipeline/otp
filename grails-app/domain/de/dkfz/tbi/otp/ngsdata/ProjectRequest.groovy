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
package de.dkfz.tbi.otp.ngsdata

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.Entity

class ProjectRequest implements ProjectPropertiesGivenWithRequest, Entity {
    User requester
    User pi

    enum Status {
        WAITING_FOR_PI,
        APPROVED_BY_PI_WAITING_FOR_OPERATOR,
        DENIED_BY_PI,
        DENIED_BY_OPERATOR,
        PROJECT_CREATED,
    }
    Status status = Status.WAITING_FOR_PI
    Project project

    Set<String> keywords
    String sequencingCenter
    Integer approxNoOfSamples
    Set<SeqType> seqTypes
    boolean furtherDataProcessing
    String comments

    Set<User> deputyPis
    Set<User> responsibleBioinformaticians
    Set<User> bioinformaticians
    Set<User> submitters

    static constraints = {
        project nullable: true, validator: { val, obj ->
            if (!val && obj.status == Status.PROJECT_CREATED) {
                return "required"
            }
            if (val && obj.status != Status.PROJECT_CREATED) {
                return "illegal"
            }
        }
        name blank: false
        description blank: false
        organizationalUnit blank: false

        costCenter nullable: true
        endDate nullable: true
        storageUntil nullable: true
        predecessorProject nullable: true
        tumorEntity nullable: true
        speciesWithStrain nullable: true
        sequencingCenter nullable: true
        approxNoOfSamples nullable: true
        forceCopyFiles nullable: true
        furtherDataProcessing nullable: true
        comments nullable: true
        fundingBody nullable: true
        grantId nullable: true
    }

    static hasMany = [
            seqTypes: SeqType,
            deputyPis: User,
            responsibleBioinformaticians: User,
            bioinformaticians: User,
            submitters: User,
    ]


    static mapping = {
        description type: "text"
        deputyPis joinTable: [
                name: "project_request_deputy_pi",
                key: "project_request_id",
                column: "deputy_pi_id",
        ]
        responsibleBioinformaticians joinTable: [
                name: "project_request_responsible_bioinformatician",
                key: "project_request_id",
                column: "responsible_bioinformatician_id",
        ]
        bioinformaticians joinTable: [
                name: "project_request_bioinformatician",
                key: "project_request_id",
                column: "bioinformatician_id",
        ]
        submitters joinTable: [
                name: "project_request_submitter",
                key: "project_request_id",
                column: "submitter_id",
        ]
    }
}

@TupleConstructor
enum ProjectRequestRole {
    PI(true, true, true, true, true),
    DEPUTY_PI(true, false, true, false, true),
    RESPONSIBLE_BIOINFORMATICIAN(true, true, false, false, true),
    BIOINFORMATICIAN(true, true, false, false, true),
    SUBMITTER(true, false, false, false, true),

    final boolean accessToOtp
    final boolean accessToFiles
    final boolean manageUsers
    final boolean manageUsersAndDelegate
    final boolean receivesNotifications
}
