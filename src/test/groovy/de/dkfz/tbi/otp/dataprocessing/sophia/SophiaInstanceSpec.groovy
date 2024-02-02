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
package de.dkfz.tbi.otp.dataprocessing.sophia

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import java.nio.file.Path

class SophiaInstanceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                RawSequenceFile,
                FastqFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                Project,
                ReferenceGenome,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
                Sample,
                SamplePair,
                SampleType,
                SampleTypePerProject,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
                SophiaInstance,
        ]
    }

    @TempDir
    Path tempDir

    TestConfigService configService

    SophiaInstance instance

    /**
     * Creates Temporary File for Data Management path
     * so later on temp files can be generated and paths tested
     */
    void setup() {
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): tempDir.toString()])

        this.instance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()
        instance.processingState = AnalysisProcessingStates.FINISHED
        assert instance.save(flush: true)
    }

    void cleanup() {
        configService.clean()
    }

    void "getLatestValidSophiaInstanceForSamplePair, test if one instance exists, return instance"() {
        expect:
        instance == SophiaInstance.getLatestValidSophiaInstanceForSamplePair(instance.samplePair)
    }

    void "getLatestValidSophiaInstanceForSamplePair, test if no instance exists, return null"() {
        given:
        SamplePair samplePair = instance.samplePair
        instance.delete(flush: true)

        expect:
        null == SophiaInstance.getLatestValidSophiaInstanceForSamplePair(samplePair)
    }

    void "getLatestValidSophiaInstanceForSamplePair, test if two instances exists, return latest instance"() {
        given:
        SamplePair samplePair = instance.samplePair

        samplePair.mergingWorkPackage1.bamFileInProjectFolder = instance.sampleType1BamFile
        assert samplePair.mergingWorkPackage1.save(flush: true)
        samplePair.mergingWorkPackage2.bamFileInProjectFolder = instance.sampleType2BamFile
        assert samplePair.mergingWorkPackage2.save(flush: true)
        SophiaInstance instance2 = DomainFactory.createSophiaInstance(samplePair)
        instance2.processingState = AnalysisProcessingStates.FINISHED
        assert instance2.save(flush: true)

        expect:
        instance2 == SophiaInstance.getLatestValidSophiaInstanceForSamplePair(samplePair)
    }

    void "getLatestValidSophiaInstanceForSamplePair, test if two instances exists but the latest is withdrawn, return first instance"() {
        given:
        SamplePair samplePair = instance.samplePair

        samplePair.mergingWorkPackage1.bamFileInProjectFolder = instance.sampleType1BamFile
        assert samplePair.mergingWorkPackage1.save(flush: true)
        samplePair.mergingWorkPackage2.bamFileInProjectFolder = instance.sampleType2BamFile
        assert samplePair.mergingWorkPackage2.save(flush: true)
        SophiaInstance instance2 = DomainFactory.createSophiaInstance(samplePair)
        instance2.withdrawn = true
        assert instance2.save(flush: true)

        expect:
        instance == SophiaInstance.getLatestValidSophiaInstanceForSamplePair(samplePair)
    }

    void "getLatestValidSophiaInstanceForSamplePair, test if two instances exists but the latest not finished yet, return first instance"() {
        given:
        SamplePair samplePair = instance.samplePair

        samplePair.mergingWorkPackage1.bamFileInProjectFolder = instance.sampleType1BamFile
        assert samplePair.mergingWorkPackage1.save(flush: true)
        samplePair.mergingWorkPackage2.bamFileInProjectFolder = instance.sampleType2BamFile
        assert samplePair.mergingWorkPackage2.save(flush: true)
        SophiaInstance instance2 = DomainFactory.createSophiaInstance(samplePair)
        instance2.processingState = AnalysisProcessingStates.IN_PROGRESS
        assert instance2.save(flush: true)

        expect:
        instance == SophiaInstance.getLatestValidSophiaInstanceForSamplePair(samplePair)
    }
}
