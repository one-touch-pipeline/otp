/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.qcTrafficLight

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerWorkflowService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.CollectionUtils

import static de.dkfz.tbi.otp.qcTrafficLight.QcThreshold.ThresholdLevel.ERROR

@CompileDynamic
@Transactional
class QcTrafficLightService {

    CommentService commentService
    ConfigService configService
    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService

    CellRangerWorkflowService cellRangerWorkflowService

    TicketService ticketService

    SeqTypeService seqTypeService
    QcThresholdService qcThresholdService

    @Lazy
    Map<SeqType, Closure<Void>> qcHandlerMap = initQcHandlerMap()

    private Map<SeqType, Closure<Void>> initQcHandlerMap() {
        Map<SeqType, Closure<Void>> map = [:]
        Realm realm = configService.defaultRealm
        addToQcHandler(map, SeqTypeService.panCanAlignableSeqTypes) { AbstractBamFile roddyBamFile ->
            linkFilesToFinalDestinationService.linkNewResults((RoddyBamFile)roddyBamFile, realm)
        }
        addToQcHandler(map, SeqTypeService.rnaAlignableSeqTypes) { AbstractBamFile roddyBamFile ->
            linkFilesToFinalDestinationService.linkNewRnaResults((RnaRoddyBamFile)roddyBamFile, realm)
        }
        addToQcHandler(map, SeqTypeService.cellRangerAlignableSeqTypes) { AbstractBamFile roddyBamFile ->
            cellRangerWorkflowService.linkResultFiles((SingleCellBamFile)roddyBamFile)
        }
        return map.asImmutable()
    }

    private void addToQcHandler(Map<SeqType, Closure<Void>> map, List<SeqType> seqTypes, Closure<Void> linkResultClosure) {
        seqTypes.each {
            map[it] = linkResultClosure
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#bamFile?.project, 'OTP_READ_ACCESS')")
    void setQcTrafficLightStatusWithComment(AbstractBamFile bamFile, AbstractBamFile.QcTrafficLightStatus qcTrafficLightStatus, String comment) {
        assert bamFile: "the bamFile must not be null"
        assert qcTrafficLightStatus: "the qcTrafficLightStatus must not be null"
        assert comment: "the comment must not be null"

        AbstractBamFile.QcTrafficLightStatus prevQcTrafficLightStatus = bamFile.qcTrafficLightStatus

        commentService.saveComment(bamFile, comment)
        setQcTrafficLightStatus(bamFile, qcTrafficLightStatus)
        // No workflow will be triggered by changing from WARNING to ACCEPTED, since all workflows will be processed.
        // The following block can be removed after migration
        if ((bamFile.qcTrafficLightStatus == AbstractBamFile.QcTrafficLightStatus.ACCEPTED ||
             bamFile.qcTrafficLightStatus == AbstractBamFile.QcTrafficLightStatus.WARNING) &&
                prevQcTrafficLightStatus != AbstractBamFile.QcTrafficLightStatus.WARNING) {
            ticketService.findAllTickets(bamFile.containedSeqTracks).each {
                ticketService.resetAnalysisNotification(it)
            }
            Closure<Void> qcHandler = qcHandlerMap[bamFile.seqType]
            assert qcHandler
            qcHandler(bamFile)
        }
    }

    private void setQcTrafficLightStatus(AbstractBamFile bamFile, AbstractBamFile.QcTrafficLightStatus qcTrafficLightStatus) {
        bamFile.qcTrafficLightStatus = qcTrafficLightStatus
        assert bamFile.save(flush: true)
    }

    void setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(AbstractBamFile bamFile, QcTrafficLightValue qc) {
        if (bamFile.qcTrafficLightStatus == AbstractBamFile.QcTrafficLightStatus.BLOCKED) {
            return
        }

        if (qcThresholdService.getThresholds(bamFile.project, bamFile.seqType, qc.class).empty) {
            setQcTrafficLightStatus(bamFile, AbstractBamFile.QcTrafficLightStatus.UNCHECKED)
            return
        }

        AbstractBamFile.QcTrafficLightStatus qcStatus
        // if QC error occurs, just set status to WARNING and continue with processing
        if (qcValuesExceedErrorThreshold(bamFile, qc)) {
            qcStatus = AbstractBamFile.QcTrafficLightStatus.WARNING
            commentService.saveCommentAsOtp(bamFile, "Bam file exceeded threshold")
        } else {
            qcStatus = AbstractBamFile.QcTrafficLightStatus.QC_PASSED
        }

        setQcTrafficLightStatus(bamFile, qcStatus)
    }

    private boolean qcValuesExceedErrorThreshold(AbstractBamFile bamFile, QcTrafficLightValue qc) {
        List<String> properties = QcThreshold.getValidQcPropertyForQcClass(qc.class.name)
        Map<String, ?> qcMap = properties.collectEntries {
            [(it): qc[it]]
        }
        return properties.any { String property ->
            QcThreshold qcThreshold = CollectionUtils.atMostOneElement(
                    QcThreshold.findAllByQcClassAndSeqTypeAndQcProperty1AndProject(qc.class.name, bamFile.seqType, property, bamFile.project)) ?:
                            CollectionUtils.atMostOneElement(
                                    QcThreshold.findAllByQcClassAndSeqTypeAndQcProperty1AndProjectIsNull(qc.class.name, bamFile.seqType, property))
            return qcThreshold?.qcPassed(qcMap) == ERROR
        }
    }
}
