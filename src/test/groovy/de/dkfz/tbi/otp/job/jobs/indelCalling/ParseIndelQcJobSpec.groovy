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
package de.dkfz.tbi.otp.job.jobs.indelCalling

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.QcThreshold
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Path

class ParseIndelQcJobSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                RawSequenceFile,
                FastqFile,
                FileType,
                IndelCallingInstance,
                IndelSampleSwapDetection,
                IndelQualityControl,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                ProcessingStep,
                Project,
                QcThreshold,
                Sample,
                SamplePair,
                SampleType,
                SampleTypePerProject,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SequencingKitLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
                ReferenceGenome,
                ReferenceGenomeEntry,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
        ]
    }

    TestConfigService configService

    @TempDir
    Path tempDir

    ParseIndelQcJob job

    IndelCallingInstance indelCallingInstance
    Path sampleSwapJsonFile
    Path indelQcJsonFile

    void setup() {
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): tempDir.toString()])
        indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
        job = [
                getProcessParameterObject: { -> indelCallingInstance },
        ] as ParseIndelQcJob

        sampleSwapJsonFile = tempDir.resolve("sampleSwapJsonFile")
        indelQcJsonFile = tempDir.resolve("indelQcJsonFile")

        job.indelCallingService = Mock(IndelCallingService) {
            getSampleSwapJsonFile(_) >> sampleSwapJsonFile
            getIndelQcJsonFile(_) >> indelQcJsonFile
        }

        job.qcTrafficLightService = new QcTrafficLightService()
    }

    void cleanup() {
        configService.clean()
    }

    @Unroll
    void "test execute method, throw error since #notAvailable does not exist on filesystem"() {
        given:
        Path notAvailablePath

        if (notAvailable == "indelQcJsonFile") {
            DomainFactory.createIndelSampleSwapDetectionFileOnFileSystem(sampleSwapJsonFile, indelCallingInstance.individual)
            notAvailablePath = indelQcJsonFile
        } else if (notAvailable == "sampleSwapJsonFile") {
            DomainFactory.createIndelQcFileOnFileSystem(indelQcJsonFile)
            notAvailablePath = sampleSwapJsonFile
        }

        when:
        job.execute()

        then:
        AssertionError e = thrown()
        e.message.contains("${notAvailablePath} on local filesystem is not accessible or does not exist.")

        where:
        notAvailable | _
        "sampleSwapJsonFile" | _
        "indelQcJsonFile"    | _
    }

    void "test execute method when both files available"() {
        given:
        DomainFactory.createIndelQcFileOnFileSystem(indelQcJsonFile)
        DomainFactory.createIndelSampleSwapDetectionFileOnFileSystem(sampleSwapJsonFile, indelCallingInstance.individual)

        when:
        job.execute()

        then:
        CollectionUtils.exactlyOneElement(IndelSampleSwapDetection.list()).indelCallingInstance == indelCallingInstance
        CollectionUtils.exactlyOneElement(IndelQualityControl.list()).indelCallingInstance == indelCallingInstance
        indelCallingInstance.processingState == AnalysisProcessingStates.FINISHED
    }
}
