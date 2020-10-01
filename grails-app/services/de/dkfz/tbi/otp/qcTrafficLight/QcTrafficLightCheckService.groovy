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

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile

@Transactional
class QcTrafficLightCheckService {

    QcTrafficLightNotificationService qcTrafficLightNotificationService

    void handleQcCheck(AbstractMergedBamFile bamFile, Closure callbackIfAllFine) {
        switch (bamFile.qcTrafficLightStatus.jobLinkCase) {
            case null:
            case AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.CREATE_LINKS:
                callbackIfAllFine()
                break
            case AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.CREATE_NO_LINK:
                //no links creating, so nothing to do
                break
            case AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.SHOULD_NOT_OCCUR:
                throw new OtpRuntimeException("${bamFile.qcTrafficLightStatus} is not a valid qcTrafficLightStatus " +
                        "during workflow processing, it should only occur after the workflow has finished")
            default:
                throw new AssertionError("Unknown value: ${bamFile.qcTrafficLightStatus.jobLinkCase}")
        }

        switch (bamFile.qcTrafficLightStatus.jobNotifyCase) {
            case null:
            case AbstractMergedBamFile.QcTrafficLightStatus.JobNotifyCase.NO_NOTIFY:
                //no email sending, so nothing to do
                break
            case AbstractMergedBamFile.QcTrafficLightStatus.JobNotifyCase.NOTIFY:
                qcTrafficLightNotificationService.informResultsAreBlocked(bamFile)
                break
            case AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.SHOULD_NOT_OCCUR:
                throw new OtpRuntimeException("${bamFile.qcTrafficLightStatus} is not a valid qcTrafficLightStatus " +
                        "during workflow processing, it should only occur after the workflow has finished")
            default:
                throw new AssertionError("Unknown value: ${bamFile.qcTrafficLightStatus.jobNotifyCase}")
        }
    }
}

