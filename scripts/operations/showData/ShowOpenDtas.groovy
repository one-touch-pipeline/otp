/*
 * Copyright 2011-2021 The OTP authors
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

import de.dkfz.tbi.otp.project.dta.DataTransfer
import de.dkfz.tbi.otp.project.dta.DataTransferAgreement
import de.dkfz.tbi.util.TimeFormats

/**
 * scripts show details to all open DataTransfer (having no completion date))
 */

//---------------------------------------------------------------------
//input

//no input

//---------------------------------------------------------------------
//work

String header = [
        "project",
        "dtaId",
        "peer institution",
        "legal basis",
        "agreement created date",
        "agreement validity date",
        "agreement comment",
        "peer person",
        "peer account",
        "transfer mode",
        "requester",
        "ticketID",
        "performing user",
        "performing user account",
        "direction",
        "transfer created date",
        "transfer comment",
].join('\t')

String content = DataTransfer.findAllByCompletionDateIsNull().collect { DataTransfer dataTransfer ->
    DataTransferAgreement dataTransferAgreement = dataTransfer.dataTransferAgreement
    [
            dataTransferAgreement.project.name,
            dataTransferAgreement.dtaId ?: '',
            dataTransferAgreement.peerInstitution ?: '',
            dataTransferAgreement.legalBasis,
            TimeFormats.DATE.getFormattedDate(dataTransferAgreement.dateCreated),
            TimeFormats.DATE.getFormattedDate(dataTransferAgreement.validityDate),
            dataTransferAgreement.comment?.replaceAll('[\t\n\r]+', ' ') ?: '',
            dataTransfer.peerPerson ?: '',
            dataTransfer.peerAccount ?: '',
            dataTransfer.transferMode,
            dataTransfer.requester ?: '',
            dataTransfer.ticketID ?: '',
            dataTransfer.performingUser.realName,
            dataTransfer.performingUser.username,
            dataTransfer.direction,
            TimeFormats.DATE.getFormattedDate(dataTransfer.dateCreated),
            dataTransfer.comment?.replaceAll('[\t\n\r]+', ' ') ?: '',
    ].join('\t')
}.sort().join('\n')

println([
        header,
        content
].join('\n'))
