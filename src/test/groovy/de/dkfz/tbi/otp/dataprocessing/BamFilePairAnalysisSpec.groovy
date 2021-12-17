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
package de.dkfz.tbi.otp.dataprocessing

import grails.artefact.Artefact
import grails.artefact.DomainClass
import grails.testing.gorm.DataTest
import grails.validation.Validateable
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.GormEntity
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class BamFilePairAnalysisSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergedBamFile,
                AceseqInstance,
                AlignmentPass,
                BamFilePairAnalysis,
                DataFile,
                ExternallyProcessedMergedBamFile,
                ExternalMergingWorkPackage,
                FileType,
                IndelCallingInstance,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingPass,
                MergingSet,
                MergingSetAssignment,
                MergingWorkPackage,
                MockBamFilePairAnalysis,
                Pipeline,
                ProcessedBamFile,
                ProcessedMergedBamFile,
                Project,
                Realm,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddySnvCallingInstance,
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

    void "test constraints when everything is fine, should be valid"() {
        given:
        MockBamFilePairAnalysis bamFilePairAnalysis = createMockBamFilePairAnalysis()

        expect:
        assert bamFilePairAnalysis.validate()
    }

    @Unroll
    void "test constraints when #property is null, should not be valid"() {
        given:
        MockBamFilePairAnalysis bamFilePairAnalysis = createMockBamFilePairAnalysis()

        when:
        bamFilePairAnalysis[property] = null

        then:
        !bamFilePairAnalysis.validate()

        where:
        property << [
                "config",
                "samplePair",
                "instanceName",
                "sampleType1BamFile",
                "sampleType2BamFile",
        ]
    }

    void "test the constraints when instanceName is blank, should not be valid"() {
        given:
        MockBamFilePairAnalysis bamFilePairAnalysis = createMockBamFilePairAnalysis()

        when:
        bamFilePairAnalysis.instanceName = ''

        then:
        !bamFilePairAnalysis.validate()
    }

    void "test the constraints when instanceName is not unique, should not be valid"() {
        given:
        MockBamFilePairAnalysis bamFilePairAnalysis = createMockBamFilePairAnalysis()
        MockBamFilePairAnalysis bamFilePairAnalysisSameName = createMockBamFilePairAnalysis()

        when:
        bamFilePairAnalysisSameName.instanceName = bamFilePairAnalysis.instanceName
        bamFilePairAnalysisSameName.samplePair = bamFilePairAnalysis.samplePair

        then:
        !bamFilePairAnalysisSameName.validate()
    }

    @Unroll
    void "check different bamFiles classes: #classBamFile1, #classBamFile2"() {
        given:
        AbstractMergedBamFile bamFile1 = createBamFile(classBamFile1)
        AbstractMergedBamFile bamFile2 = createBamFile(classBamFile2, [
                seqType: bamFile1.seqType,
                sample : DomainFactory.createSample([individual: bamFile1.individual]),
        ])

        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(bamFile1.mergingWorkPackage)

        expect:
        DomainFactory.createRoddySnvInstanceWithRoddyBamFiles(
                sampleType1BamFile: bamFile1,
                sampleType2BamFile: bamFile2,
        )

        where:
        classBamFile1                    | classBamFile2
        RoddyBamFile                     | RoddyBamFile
        ProcessedMergedBamFile           | ProcessedMergedBamFile
        ExternallyProcessedMergedBamFile | ExternallyProcessedMergedBamFile
        RoddyBamFile                     | ExternallyProcessedMergedBamFile
        ExternallyProcessedMergedBamFile | RoddyBamFile
        ProcessedMergedBamFile           | ExternallyProcessedMergedBamFile
        ExternallyProcessedMergedBamFile | ProcessedMergedBamFile
        RoddyBamFile                     | ProcessedMergedBamFile
        ProcessedMergedBamFile           | RoddyBamFile
    }

    private <E> AbstractMergedBamFile createBamFile(Class<E> clazz, Map propertiesForMergingWorkPackage = [:]) {
        Map properties = [
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                md5sum             : DomainFactory.DEFAULT_MD5_SUM,
                fileSize           : ++DomainFactory.counter,
        ]
        switch (clazz) {
            case RoddyBamFile:
                return DomainFactory.createRoddyBamFile([
                        workPackage: DomainFactory.createMergingWorkPackage([
                                pipeline: DomainFactory.createPanCanPipeline(),
                                seqType : DomainFactory.createWholeGenomeSeqType(),
                        ] + propertiesForMergingWorkPackage)
                ] + properties)
            case ProcessedMergedBamFile:
                return DomainFactory.createProcessedMergedBamFile([
                        workPackage: DomainFactory.createMergingWorkPackage([
                                pipeline: DomainFactory.createDefaultOtpPipeline(),
                                seqType : DomainFactory.createWholeGenomeSeqType(),
                        ] + propertiesForMergingWorkPackage)
                ] + properties)
            case ExternallyProcessedMergedBamFile:
                return DomainFactory.createExternallyProcessedMergedBamFile([
                        workPackage: DomainFactory.createExternalMergingWorkPackage([
                                pipeline: DomainFactory.createExternallyProcessedPipelineLazy(),
                                seqType : DomainFactory.createWholeGenomeSeqType(),
                        ] + propertiesForMergingWorkPackage)
                ] + properties)
            default:
                assert false
        }
    }

    static MockBamFilePairAnalysis createMockBamFilePairAnalysis() {
        Pipeline alignmentPipeline = DomainFactory.createPanCanPipeline()
        Pipeline snvPipeline = DomainFactory.createRoddySnvPipelineLazy()

        MergingWorkPackage controlWorkPackage = DomainFactory.createMergingWorkPackage(
                pipeline: alignmentPipeline,
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
        )
        SamplePair samplePair = DomainFactory.createDisease(controlWorkPackage)
        MergingWorkPackage diseaseWorkPackage = samplePair.mergingWorkPackage1

        RoddyBamFile disease = DomainFactory.createRoddyBamFile([workPackage: diseaseWorkPackage])
        RoddyBamFile control = DomainFactory.createRoddyBamFile([workPackage: controlWorkPackage, config: disease.config])

        return new MockBamFilePairAnalysis([
                instanceName      : "2014-08-25_15h32",
                samplePair        : samplePair,
                sampleType1BamFile: disease,
                sampleType2BamFile: control,
                config            : DomainFactory.createRoddyWorkflowConfig([seqType: samplePair.seqType, pipeline: snvPipeline]),
        ])
    }
}

@Artefact(DomainClassArtefactHandler.TYPE)
class MockBamFilePairAnalysis extends BamFilePairAnalysis implements DomainClass, GormEntity<MockBamFilePairAnalysis>, Validateable {
    @Override
    @Deprecated
    OtpPath getInstancePath() {
        return new OtpPath("somePath")
    }
}
