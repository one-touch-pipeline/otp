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
package de.dkfz.tbi.otp.job.jobs.indelCalling

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

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

class ParseIndelQcJobSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergedBamFile,
                DataFile,
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
                Realm,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
        ]
    }

    TestConfigService configService

    @Rule
    TemporaryFolder temporaryFolder

    IndelCallingInstance indelCallingInstance
    ParseIndelQcJob job

    void setup() {
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
        job = [
                getProcessParameterObject: { -> indelCallingInstance },
        ] as ParseIndelQcJob
        job.qcTrafficLightService = new QcTrafficLightService()
    }

    void cleanup() {
        configService.clean()
    }

    @Unroll
    void "test execute method, throw error since #notAvailable does not exist"() {
        given:
        if (available == "sampleSwapJsonFile") {
            DomainFactory.createIndelSampleSwapDetectionFileOnFileSystem(indelCallingInstance.sampleSwapJsonFile, indelCallingInstance.individual)
        } else {
            DomainFactory.createIndelQcFileOnFileSystem(indelCallingInstance.indelQcJsonFile)
        }

        when:
        job.execute()

        then:
        AssertionError e = thrown()
        e.message.contains("${indelCallingInstance."${notAvailable}"} on local filesystem is not accessible or does not exist.")

        where:
        available            | notAvailable
        "sampleSwapJsonFile" | "indelQcJsonFile"
        "indelQcJsonFile"    | "sampleSwapJsonFile"
    }

    void "test execute method when both files available"() {
        given:
        DomainFactory.createIndelQcFileOnFileSystem(indelCallingInstance.indelQcJsonFile)
        DomainFactory.createIndelSampleSwapDetectionFileOnFileSystem(indelCallingInstance.sampleSwapJsonFile, indelCallingInstance.individual)

        when:
        job.execute()

        then:
        CollectionUtils.exactlyOneElement(IndelSampleSwapDetection.list()).indelCallingInstance == indelCallingInstance
        CollectionUtils.exactlyOneElement(IndelQualityControl.list()).indelCallingInstance == indelCallingInstance
        indelCallingInstance.processingState == AnalysisProcessingStates.FINISHED
    }
}
