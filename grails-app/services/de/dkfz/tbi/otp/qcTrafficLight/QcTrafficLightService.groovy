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

package de.dkfz.tbi.otp.qcTrafficLight

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.LinkFilesToFinalDestinationService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.tracking.TrackingService

import static de.dkfz.tbi.otp.qcTrafficLight.QcThreshold.ThresholdLevel.ERROR

@Transactional
class QcTrafficLightService {

    CommentService commentService
    ConfigService configService
    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService

    TrackingService trackingService


    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#bamFile?.project, 'OTP_READ_ACCESS')")
    void setQcTrafficLightStatusWithComment(AbstractMergedBamFile bamFile, AbstractMergedBamFile.QcTrafficLightStatus qcTrafficLightStatus, String comment) {
        assert bamFile: "the bamFile must not be null"
        assert qcTrafficLightStatus: "the qcTrafficLightStatus must not be null"
        assert comment: "the comment must not be null"

        commentService.saveComment(bamFile, comment)
        setQcTrafficLightStatus(bamFile, qcTrafficLightStatus)
        if (bamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED) {
            trackingService.findAllOtrsTickets(bamFile.containedSeqTracks).each {
                trackingService.resetAnalysisNotification(it)
            }
            if (bamFile.seqType.isRna()) {
                linkFilesToFinalDestinationService.linkNewRnaResults((RnaRoddyBamFile) bamFile, configService.getDefaultRealm())
            } else {
                linkFilesToFinalDestinationService.linkNewResults(bamFile, configService.getDefaultRealm())
            }
        }
    }

    private void setQcTrafficLightStatus(AbstractMergedBamFile bamFile, AbstractMergedBamFile.QcTrafficLightStatus qcTrafficLightStatus) {
        bamFile.qcTrafficLightStatus = qcTrafficLightStatus
        assert bamFile.save(flush: true)
    }

    //TODO OTP-3097: provide method for handling qc in analsys

    void setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(AbstractMergedBamFile bamFile, QcTrafficLightValue qc) {
        if (bamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED) {
            return
        }

        Project project = bamFile.getProject()

        if (!project.qcThresholdHandling.checksThreshold) {
            setQcTrafficLightStatus(bamFile, AbstractMergedBamFile.QcTrafficLightStatus.UNCHECKED)
            return
        }

        AbstractMergedBamFile.QcTrafficLightStatus qcStatus
        if (qcValuesExceedErrorThreshold(bamFile, qc)) {
            if (project.qcThresholdHandling.blocksBamFile) {
                qcStatus = AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED
                commentService.saveCommentAsOtp(bamFile, "Bam file exceeded threshold")
            } else {
                qcStatus = AbstractMergedBamFile.QcTrafficLightStatus.AUTO_ACCEPTED
            }
        } else {
            qcStatus = AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        }

        setQcTrafficLightStatus(bamFile, qcStatus)
    }

    private boolean qcValuesExceedErrorThreshold(AbstractMergedBamFile bamFile, QcTrafficLightValue qc) {
        return QcThreshold.getValidQcPropertyForQcClass(qc.class.name).any { String property ->
            QcThreshold qcThreshold =
                    QcThreshold.findByQcClassAndSeqTypeAndQcProperty1AndProject(qc.class.name, bamFile.seqType, property, bamFile.project) ?:
                            QcThreshold.findByQcClassAndSeqTypeAndQcProperty1AndProjectIsNull(qc.class.name, bamFile.seqType, property)
            return qcThreshold?.qcPassed(qc) == ERROR
        }
    }
}
