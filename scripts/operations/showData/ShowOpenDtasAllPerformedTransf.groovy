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
 * scripts show details to last 25 listed DataTransfer
 */

//---------------------------------------------------------------------
//input

//no input

//---------------------------------------------------------------------
//work

String header = [
        "date created",
        "performing user",
        "performing user account",
        "project",
        "ticketID",
        "agreement comment",
        "status",
        "date completed",
        "institution",
        "direction",
        "transfer mode"
].join('\t')

String content = DataTransfer.findAll().collect { DataTransfer dataTransfer ->
    DataTransferAgreement dataTransferAgreement = dataTransfer.dataTransferAgreement
    [
            TimeFormats.DATE.getFormattedDate(dataTransferAgreement.dateCreated),
            dataTransfer.peerPerson ?: '',
            dataTransfer.peerAccount ?: '',
            dataTransferAgreement.project.name,
            dataTransfer.ticketID ?: '',
            dataTransferAgreement.comment?.replaceAll('[\t\n\r]+', ' ') ?: '',
            dataTransfer.completionDate ? 'complete' : 'incomplete',
            TimeFormats.DATE.getFormattedDate(dataTransfer.completionDate),
            dataTransferAgreement.peerInstitution ?: '',
            dataTransfer.direction,
            dataTransfer.transferMode,

    ].join(' \t')
}.sort().join('\n')

println([
        header,
        content
].join('\n'))
