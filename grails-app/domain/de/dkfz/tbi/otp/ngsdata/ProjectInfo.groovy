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

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.Entity

import java.text.DateFormat
import java.text.SimpleDateFormat

class ProjectInfo implements Entity {

    enum TransferMode {
        ASPERA,
        HARD_DISK,
        INTERNAL,
    }

    enum LegalBasis {
        DTA,
        PROBAND,
        COOPERATION,
        UNKNOWN,
    }

    String fileName

    User performingUser

    String recipientInstitution
    String recipientPerson
    String recipientAccount

    Date transferDate
    Date validityDate
    Date deletionDate

    TransferMode transferMode
    LegalBasis legalBasis

    String dtaId
    String requester
    String ticketID
    String comment

    Project project

    static belongsTo = [
            project: Project,
    ]

    static constraints = {
        fileName(blank: false, unique: 'project', validator: { String val ->
            OtpPath.isValidPathComponent(val)
        })
        recipientInstitution nullable: true
        recipientPerson nullable: true
        recipientAccount nullable: true
        performingUser nullable: true
        transferDate nullable: true
        validityDate nullable: true
        deletionDate nullable: true
        transferMode nullable: true
        legalBasis nullable: true
        dtaId nullable: true
        requester nullable: true
        ticketID nullable: true
        comment nullable: true
    }

    static mapping = {
        fileName type: "text"
        comment type: "text"
    }

    String getPath() {
        return "${project.projectDirectory.toString()}/${ProjectService.PROJECT_INFO}/${fileName}"
    }

    boolean hasAdditionalInfos() {
        recipientInstitution || recipientPerson || recipientAccount || performingUser ||
                transferDate || validityDate || deletionDate ||
                transferMode || legalBasis || dtaId || requester ||
                ticketID || comment
    }

    String getAdditionalInfos() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd" , Locale.ENGLISH)
        if (recipientInstitution) {
            return [
                    "recipientInstitution: ${recipientInstitution}",
                    "recipientPerson: ${recipientPerson}",
                    "recipientAccount: ${recipientAccount}",
                    "performingUser: ${performingUser.realName}",
                    "transferDate: ${transferDate ? dateFormat.format(transferDate) : '-'}",
                    "validityDate: ${validityDate ? dateFormat.format(validityDate) : '-'}",
                    "deletionDate: ${deletionDate ? dateFormat.format(deletionDate) : '-'}",
                    "transferMode: ${transferMode}",
                    "legalBasis: ${legalBasis}",
                    "requester: ${requester}",
                    "ticketID: ${ticketID}",
                    "comment: ${comment}",
            ].join(" | ")
        }
        return ""
    }
}
