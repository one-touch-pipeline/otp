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

    String fileName

    String recipient
    User performingUser
    User commissioningUser
    Date transferDate
    Date validityDate
    String transferMode
    String legalBasis

    Project project

    static belongsTo = [
            project: Project,
    ]

    static constraints = {
        fileName(blank: false, unique: 'project', validator: { String val ->
            OtpPath.isValidPathComponent(val)
        })
        recipient nullable: true
        performingUser nullable: true
        commissioningUser nullable: true
        transferDate nullable: true
        validityDate nullable: true
        transferMode nullable: true
        legalBasis nullable: true

    }

    static mapping = {
        fileName type: "text"
    }

    String getPath() {
        return "${project.getProjectDirectory().toString()}/${ProjectService.PROJECT_INFO}/${fileName}"
    }

    String getAdditionalInfos() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd" , Locale.ENGLISH)
        if (recipient) {
            return [
                    "recipient: ${recipient}",
                    "performingUser: ${performingUser.realName}",
                    "commissioningUser: ${commissioningUser.realName}",
                    "transferDate: ${dateFormat.format(transferDate)}",
                    "validityDate: ${dateFormat.format(validityDate)}",
                    "transferMode: ${transferMode}",
                    "legalBasis: ${legalBasis}",
            ].join(" | ")
        } else {
            return ""
        }
    }
}
