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
package de.dkfz.tbi.otp.job.jobs.cellRanger

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.qcTrafficLight.QcThreshold
import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.Path

@Rollback
@Integration
class ParseCellRangerQcJobIntegrationSpec extends Specification implements CellRangerFactory {

    @TempDir
    Path tempDir

    File qaFile
    SingleCellBamFile singleCellBamFile
    ParseCellRangerQcJob job

    void setupData() {
        qaFile = CreateFileHelper.createFile(tempDir.resolve(SingleCellBamFile.METRICS_SUMMARY_CSV_FILE_NAME)).toFile()
        createQaFileOnFileSystem(qaFile)
        singleCellBamFile = createBamFile()
        singleCellBamFile.metaClass.getQualityAssessmentCsvFile = { -> qaFile }
        job = [
                getProcessParameterObject: { -> singleCellBamFile },
        ] as ParseCellRangerQcJob
        job.cellRangerService = new CellRangerService()
        job.cellRangerService.fileSystemService = new TestFileSystemService()
        job.qcTrafficLightService = new QcTrafficLightService()
        job.qcTrafficLightService.commentService = new CommentService()
        job.qcTrafficLightService.qcThresholdService = new QcThresholdService()
    }

    @Unroll
    void "ParseCellRangerQcJob sets QcTrafficLight depending on exceeded thresholds"() {
        given:
        setupData()

        createQaFileOnFileSystem(qaFile, ["Estimated Number of Cells": estimatedNumberOfCells])
        DomainFactory.createQcThreshold([
                qcProperty1          : "estimatedNumberOfCells",
                errorThresholdLower  : 0.0,
                warningThresholdLower: 2.0,
                warningThresholdUpper: 10.0,
                errorThresholdUpper  : 20.0,
                compare              : QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass              : CellRangerQualityAssessment.name,
                seqType              : singleCellBamFile.seqType,
                project              : singleCellBamFile.project,
        ])

        when:
        job.execute()

        then:
        singleCellBamFile.qcTrafficLightStatus == resultStatus

        where:
        estimatedNumberOfCells || resultStatus
        5.0                    || AbstractBamFile.QcTrafficLightStatus.QC_PASSED
        25.0                   || AbstractBamFile.QcTrafficLightStatus.WARNING
    }

    void "ParseCellRangerQcJob sets qualityAssessmentStatus of BamFile to FINISHED"() {
        given:
        setupData()

        when:
        job.execute()

        then:
        singleCellBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
    }
}
