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
package de.dkfz.tbi.otp.dataprocessing.sophia


import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class SophiaInstanceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AbstractMergedBamFile,
                DataFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingSet,
                MergingWorkPackage,
                Pipeline,
                Project,
                Realm,
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

    @Rule
    TemporaryFolder temporaryFolder

    TestConfigService configService

    SophiaInstance instance
    File instancePath

    /**
     * Creates Temporary File for Data Management path
     * so later on temp files can be generated and paths tested
     */
    void setup() {
        File temporaryFile = temporaryFolder.newFolder()
        DomainFactory.createRealm()
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFile.path])

        this.instance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()
        instance.processingState = AnalysisProcessingStates.FINISHED
        assert instance.save(flush: true)

        instancePath = new File(
                temporaryFile, "${instance.project.dirName}/sequencing/${instance.seqType.dirName}/view-by-pid/" +
                "${instance.individual.pid}/sv_results/${instance.seqType.libraryLayoutDirName}/" +
                "${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType2BamFile.sampleType.dirName}/" +
                "${instance.instanceName}"
        )
    }

    void cleanup() {
        configService.clean()
    }

    void "getSophiaInstancePath, tests if path is in a valid form"() {
        when:
        OtpPath sophiaInstancePath = instance.getInstancePath()

        then:
        instance.project == sophiaInstancePath.project
        instancePath == sophiaInstancePath.absoluteDataManagementPath
    }


    void "getFinalAceseqInputFile, tests if path is in a valid form"() {
        given:
        File expectedPath = new File(instancePath, "svs_${instance.individual.pid}_${instance.SOPHIA_OUTPUT_FILE_SUFFIX}")

        expect:
        instance.getFinalAceseqInputFile() == expectedPath
    }


    void "getLatestValidSophiaInstanceForSamplePair, test if one instance exists, return instance"() {
        expect:
        instance == SophiaInstance.getLatestValidSophiaInstanceForSamplePair(instance.samplePair)
    }

    void "getLatestValidSophiaInstanceForSamplePair, test if no instance exists, return null"() {
        given:
        SamplePair samplePair = instance.samplePair
        instance.delete()

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
