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
package de.dkfz.tbi.otp.tracking

import grails.gorm.transactions.Transactional
import org.hibernate.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.CollectionUtils

@Transactional
class OtrsTicketService {

    @Autowired
    NotificationCreator notificationCreator

    /**
     * helper do get pessimistic lock for ticket and wait till 10 seconds therefore
     */
    private void lockTicket(OtrsTicket ticket) {
        OtrsTicket.withSession { Session s ->
            s.refresh(ticket, new LockOptions(LockMode.PESSIMISTIC_WRITE).setTimeOut(10000))
        }
    }

    void saveStartTimeIfNeeded(OtrsTicket ticket, OtrsTicket.ProcessingStep step) {
        String property = "${step}Started"
        if (ticket[property] == null) {
            lockTicket(ticket)
            if (ticket[property] == null) {
                ticket[property] = new Date()
                ticket.save(flush: true)
            }
        }
    }

    boolean saveEndTimeIfNeeded(OtrsTicket ticket, OtrsTicket.ProcessingStep step) {
        String property = "${step}Finished"
        if (ticket[property] == null) {
            lockTicket(ticket)
            if (ticket[property] == null) {
                ticket[property] = new Date()
                ticket.save(flush: true)
                return true
            }
        }
        return false
    }

    void markFinalNotificationSent(OtrsTicket ticket) {
        lockTicket(ticket)
        ticket.finalNotificationSent = true
        ticket.save(flush: true)
    }

    OtrsTicket createOtrsTicket(String ticketNumber, String seqCenterComment, boolean automaticNotification) {
        OtrsTicket otrsTicket = new OtrsTicket(
                ticketNumber: ticketNumber,
                seqCenterComment: seqCenterComment,
                automaticNotification: automaticNotification,
        )
        assert otrsTicket.save(flush: true)
        return otrsTicket
    }

    OtrsTicket createOrResetOtrsTicket(String ticketNumber, String seqCenterComment, boolean automaticNotification) {
        OtrsTicket otrsTicket = CollectionUtils.atMostOneElement(OtrsTicket.findAllByTicketNumber(ticketNumber))
        if (otrsTicket) {
            otrsTicket.with {
                installationFinished = null
                fastqcFinished = null
                alignmentFinished = null
                snvFinished = null
                indelFinished = null
                sophiaFinished = null
                aceseqFinished = null
                runYapsaFinished = null
                finalNotificationSent = false
            }
            otrsTicket.automaticNotification = automaticNotification
            if (seqCenterComment && otrsTicket.seqCenterComment && !otrsTicket.seqCenterComment.contains(seqCenterComment)) {
                otrsTicket.seqCenterComment += '\n\n' + seqCenterComment
            }
            otrsTicket.seqCenterComment = otrsTicket.seqCenterComment ?: seqCenterComment
            assert otrsTicket.save(flush: true)
            return otrsTicket
        }
        return createOtrsTicket(ticketNumber, seqCenterComment, automaticNotification)
    }

    void resetAlignmentAndAnalysisNotification(OtrsTicket otrsTicket) {
        otrsTicket.alignmentFinished = null
        resetAnalysisNotification(otrsTicket)
    }

    void resetAnalysisNotification(OtrsTicket otrsTicket) {
        otrsTicket.snvFinished = null
        otrsTicket.indelFinished = null
        otrsTicket.sophiaFinished = null
        otrsTicket.aceseqFinished = null
        otrsTicket.runYapsaFinished = null
        otrsTicket.finalNotificationSent = false
        assert otrsTicket.save(flush: true)
    }


    Set<OtrsTicket> findAllOtrsTickets(Collection<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return [] as Set
        }
        List<OtrsTicket> otrsTickets = DataFile.createCriteria().listDistinct {
            'in'('seqTrack', seqTracks)
            fastqImportInstance {
                isNotNull('otrsTicket')
                otrsTicket {
                    order("ticketCreated", "asc")
                }
                projections {
                    property('otrsTicket')
                }
            }
        }
        return (otrsTickets ?: []) as Set
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void assignOtrsTicketToFastqImportInstance(String ticketNumber, Long fastqImportInstanceId) {
        FastqImportInstance fastqImportInstance = FastqImportInstance.get(fastqImportInstanceId)
        assert fastqImportInstance: "No FastqImportInstance found for ${fastqImportInstanceId}."

        OtrsTicket oldOtrsTicket = fastqImportInstance.otrsTicket

        if (oldOtrsTicket && oldOtrsTicket.ticketNumber == ticketNumber) {
            return
        }

        if (OtrsTicket.ticketNumberConstraint(ticketNumber)) {
            throw new UserException("Ticket number ${ticketNumber} does not pass validation or error while saving. " +
                    "An OTRS ticket must consist only of digits.")
        }

        // assigning a fastqImportInstance that belongs to an otrsTicket which consists of several other fastqImportInstances is not allowed,
        // because it is not possible to calculate the right "Started"/"Finished" dates
        if (oldOtrsTicket && oldOtrsTicket.fastqImportInstances.size() != 1) {
            throw new UserException("Assigning a fastqImportInstance that belongs to an OTRS-Ticket which consists of several other fastqImportInstances is not allowed.")
        }

        OtrsTicket newOtrsTicket = CollectionUtils.atMostOneElement(OtrsTicket.findAllByTicketNumber(ticketNumber)) ?:
                createOtrsTicket(ticketNumber, null, true)

        if (newOtrsTicket.finalNotificationSent) {
            throw new UserException("It is not allowed to assign to an finally notified OTRS-Ticket.")
        }

        fastqImportInstance.otrsTicket = newOtrsTicket
        assert fastqImportInstance.save(flush: true)

        ProcessingStatus status = notificationCreator.getProcessingStatus(newOtrsTicket.findAllSeqTracks())
        for (OtrsTicket.ProcessingStep step : OtrsTicket.ProcessingStep.values()) {
            ProcessingStatus.WorkflowProcessingStatus stepStatus = status."${step}ProcessingStatus"
            if (stepStatus.mightDoMore) {
                newOtrsTicket."${step}Finished" = null
            } else {
                newOtrsTicket."${step}Finished" = [oldOtrsTicket?."${step}Finished", newOtrsTicket."${step}Finished"].max()
            }
            newOtrsTicket."${step}Started" = [oldOtrsTicket?."${step}Started", newOtrsTicket."${step}Started"].min()
        }

        assert newOtrsTicket.save(flush: true)
    }

    List<MetaDataFile> getMetaDataFilesOfOtrsTicket(OtrsTicket otrsTicket) {
        return MetaDataFile.createCriteria().list {
            fastqImportInstance {
                eq("otrsTicket", otrsTicket)
            }
        } as List<MetaDataFile>
    }

    List<FastqImportInstance> getAllFastqImportInstances(OtrsTicket otrsTicket) {
        return FastqImportInstance.findAllByOtrsTicket(otrsTicket)
    }

    static String buildTicketDirectLink(OtrsTicket otrsTicket) {
        return buildTicketDirectLink(otrsTicket.ticketNumber)
    }

    static String buildTicketDirectLinkNullPointerSave(OtrsTicket otrsTicket) {
        return otrsTicket ? buildTicketDirectLink(otrsTicket.ticketNumber) : ""
    }

    static String buildTicketDirectLink(String ticketNumber) {
        return "${ProcessingOptionService.findOptionSafe(ProcessingOption.OptionName.TICKET_SYSTEM_URL, null, null)}" +
                "/index.pl?Action=AgentTicketZoom;TicketNumber=${ticketNumber}"
    }
}
