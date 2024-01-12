/*
 * Copyright 2011-2024 The OTP authors
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
import groovy.transform.CompileDynamic
import groovy.text.SimpleTemplateEngine
import org.hibernate.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileDynamic
@Transactional
class TicketService {

    ProcessingOptionService processingOptionService

    @Autowired
    NotificationCreator notificationCreator

    /**
     * helper do get pessimistic lock for ticket and wait till 10 seconds therefore
     */
    private void lockTicket(Ticket ticket) {
        Ticket.withSession { Session s ->
            s.refresh(ticket, new LockOptions(LockMode.PESSIMISTIC_WRITE).setTimeOut(10000))
        }
    }

    void saveStartTimeIfNeeded(Ticket ticket, Ticket.ProcessingStep step) {
        String property = "${step}Started"
        if (ticket[property] == null) {
            lockTicket(ticket)
            if (ticket[property] == null) {
                ticket[property] = new Date()
                ticket.save(flush: true)
            }
        }
    }

    boolean saveEndTimeIfNeeded(Ticket ticket, Ticket.ProcessingStep step) {
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

    void markFinalNotificationSent(Ticket ticket) {
        lockTicket(ticket)
        ticket.finalNotificationSent = true
        ticket.save(flush: true)
    }

    Ticket createTicket(String ticketNumber, String seqCenterComment, boolean automaticNotification) {
        Ticket ticket = new Ticket(
                ticketNumber: ticketNumber,
                seqCenterComment: seqCenterComment,
                automaticNotification: automaticNotification,
        )
        assert ticket.save(flush: true)
        return ticket
    }

    Ticket createOrResetTicket(String ticketNumber, String seqCenterComment, boolean automaticNotification) {
        Ticket ticket = CollectionUtils.atMostOneElement(Ticket.findAllByTicketNumber(ticketNumber))
        if (ticket) {
            ticket.with {
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
            ticket.automaticNotification = automaticNotification
            if (seqCenterComment && ticket.seqCenterComment && !ticket.seqCenterComment.contains(seqCenterComment)) {
                ticket.seqCenterComment += '\n\n' + seqCenterComment
            }
            ticket.seqCenterComment = ticket.seqCenterComment ?: seqCenterComment
            assert ticket.save(flush: true)
            return ticket
        }
        return createTicket(ticketNumber, seqCenterComment, automaticNotification)
    }

    void resetAlignmentAndAnalysisNotification(Ticket ticket) {
        ticket.alignmentFinished = null
        resetAnalysisNotification(ticket)
    }

    void resetAnalysisNotification(Ticket ticket) {
        ticket.with {
            snvFinished = null
            indelFinished = null
            sophiaFinished = null
            aceseqFinished = null
            runYapsaFinished = null
            finalNotificationSent = false
            assert save(flush: true)
        }
    }

    Set<Ticket> findAllTickets(Collection<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return [] as Set
        }
        List<Ticket> tickets = RawSequenceFile.createCriteria().listDistinct {
            'in'('seqTrack', seqTracks)
            fastqImportInstance {
                isNotNull('ticket')
                ticket {
                    order("ticketCreated", "asc")
                }
                projections {
                    property('ticket')
                }
            }
        }
        return (tickets ?: []) as Set
    }

    Set<SeqTrack> findAllSeqTracks(Ticket ticket) {
        return new LinkedHashSet<SeqTrack>(SeqTrack.findAll(
                'FROM SeqTrack st WHERE EXISTS (FROM RawSequenceFile df WHERE df.seqTrack = st AND df.fastqImportInstance.ticket = :ticket)',
                [ticket: ticket]
        ))
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void assignTicketToFastqImportInstance(String ticketNumber, Long fastqImportInstanceId) {
        FastqImportInstance fastqImportInstance = FastqImportInstance.get(fastqImportInstanceId)
        assert fastqImportInstance: "No FastqImportInstance found for ${fastqImportInstanceId}."

        Ticket oldTicket = fastqImportInstance.ticket

        if (oldTicket && oldTicket.ticketNumber == ticketNumber) {
            return
        }

        if (Ticket.ticketNumberConstraint(ticketNumber)) {
            throw new UserException("Ticket number ${ticketNumber} does not pass validation or error while saving. " +
                    "An Ticket must consist only of digits.")
        }

        // assigning a fastqImportInstance that belongs to an ticket which consists of several other fastqImportInstances is not allowed,
        // because it is not possible to calculate the right "Started"/"Finished" dates
        if (oldTicket && getFastqImportInstances(oldTicket).size() != 1) {
            throw new UserException(
                    "Assigning a fastqImportInstance that belongs to a Ticket which consists of several other fastqImportInstances is not allowed."
            )
        }

        Ticket newTicket = CollectionUtils.atMostOneElement(Ticket.findAllByTicketNumber(ticketNumber)) ?:
                createTicket(ticketNumber, null, true)

        if (newTicket.finalNotificationSent) {
            throw new UserException("It is not allowed to assign to an finally notified Ticket.")
        }

        fastqImportInstance.ticket = newTicket
        assert fastqImportInstance.save(flush: true)

        ProcessingStatus status = notificationCreator.getProcessingStatus(findAllSeqTracks(newTicket))
        for (Ticket.ProcessingStep step : Ticket.ProcessingStep.values()) {
            ProcessingStatus.WorkflowProcessingStatus stepStatus = status."${step}ProcessingStatus"
            if (stepStatus.mightDoMore) {
                newTicket."${step}Finished" = null
            } else {
                newTicket."${step}Finished" = [oldTicket?."${step}Finished", newTicket."${step}Finished"].max()
            }
            newTicket."${step}Started" = [oldTicket?."${step}Started", newTicket."${step}Started"].min()
        }

        assert newTicket.save(flush: true)
    }

    List<MetaDataFile> getMetaDataFilesOfTicket(Ticket ticket) {
        return MetaDataFile.createCriteria().list {
            fastqImportInstance {
                eq("ticket", ticket)
            }
        } as List<MetaDataFile>
    }

    List<FastqImportInstance> getAllFastqImportInstances(Ticket ticket) {
        return FastqImportInstance.findAllByTicket(ticket)
    }

    void saveSeqCenterComment(Ticket ticket, String value) {
        ticket.seqCenterComment = value
        ticket.save(flush: true)
    }

    List<FastqImportInstance> getFastqImportInstances(Ticket ticket) {
        // Doesn't work as a single Query, probably a Unit test problem
        return FastqImportInstance.withCriteria {
            eq('ticket', ticket)
        } as List<FastqImportInstance>
    }

    String getPrefixedTicketNumber(Ticket ticket) {
        return "${processingOptionService.findOptionAsString(ProcessingOption.OptionName.TICKET_SYSTEM_NUMBER_PREFIX)}#${ticket.ticketNumber}"
    }

    String buildTicketDirectLink(Ticket ticket) {
        return buildTicketDirectLink(ticket.ticketNumber)
    }

    String buildTicketDirectLinkNullPointerSave(Ticket ticket) {
        return ticket ? buildTicketDirectLink(ticket.ticketNumber) : ""
    }

    static String buildTicketDirectLink(String ticketNumber) {
        return new SimpleTemplateEngine().createTemplate(
                ProcessingOptionService.findOptionSafe(ProcessingOption.OptionName.TICKET_SYSTEM_URL, null, null) ?: "")
                .make([ticketNumber: ticketNumber])
                .toString()
    }
}
