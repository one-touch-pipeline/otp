/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.MailHelperService

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@CompileDynamic
@Component
class UnalignableSeqTrackEmailCreator {

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    OtrsTicketService otrsTicketService

    MailContent getMailContent(MergingWorkPackage workPackage, SeqTrack seqTrack) {
        return new MailContent(
                subject: getSubject(seqTrack),
                body: getBody(workPackage, seqTrack),
        )
    }

    private String getSubject(SeqTrack seqTrack) {
        OtrsTicket ticket = atMostOneElement(otrsTicketService.findAllOtrsTickets([seqTrack]))
        return [
                ticket ? "[${otrsTicketService.getPrefixedTicketNumber(ticket)}]" : null,
                "Will not be aligned:",
                seqTrack.ilseId ? "[ILSe ${seqTrack.ilseId}]" : null,
                seqTrack.run.name,
                seqTrack.project,
                seqTrack.sample,
        ].findAll().join(" ")
    }

    private String getBody(MergingWorkPackage workPackage, SeqTrack seqTrack) {
        List<String> propertyOverview = MergingWorkPackage.getMergingProperties(seqTrack).collect { key, value ->
            return getComparedPropertiesForMail(workPackage, key, value)
        }

        return """\
            |Processing was stopped: samples which must be merged according to PID/Sample Type combination could not \
be merged because of incompatible sequencing platforms or used chemistry.
            |
            |OTP considered the following properties when checking for merging:
            |${propertyOverview.join("\n\n")}
            |
            |Please be aware that OTP can currently only handle one bam file, therefore your current samples will not be aligned.
            |Please contact ${mailHelperService.ticketSystemEmailAddress} if you wish the samples \
nevertheless to be merged or if you want to withdraw the old samples (would result in deletion of the old bam files), to align \
the current ones.""".stripMargin()
    }

    private String getComparedPropertiesForMail(MergingWorkPackage workPackage, String key, Object value) {
        List<String> props = ["- ${key}: ${transformObjectForMail(value ?: "None")}"]
        if (value != workPackage[key]) {
            props << "    Currently active BAM file uses: ${transformObjectForMail(workPackage[key]) ?: "None"}"
        }
        return props.join("\n")
    }

    private String transformObjectForMail(Object object) {
        switch (object?.class) {
            case Sample:
                Sample sample = (Sample) object
                return "${sample} from project '${sample.project.name}'"
            default:
                return object
        }
    }

    static class MailContent {
        String subject
        String body
    }
}
