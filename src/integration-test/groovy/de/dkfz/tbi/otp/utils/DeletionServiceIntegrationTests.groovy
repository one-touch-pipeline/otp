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
package de.dkfz.tbi.otp.utils

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.utils.exceptions.FileNotFoundException
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles

import java.nio.file.Path
import java.nio.file.Files

@Rollback
@Integration
class DeletionServiceIntegrationTests extends Specification implements UserAndRoles {

    DeletionService deletionService
    LsdfFilesService lsdfFilesService
    DataProcessingFilesService dataProcessingFilesService
    FastqcDataFilesService fastqcDataFilesService
    TestConfigService configService
    SnvCallingService snvCallingService
    FileService fileService

    @TempDir
    Path tempDir

    Path outputFolder

    void setupData() {
        createUserAndRoles()
        outputFolder = Files.createDirectory(tempDir.resolve("outputFolder"))
        configService.addOtpProperties(outputFolder)
        DomainFactory.createDefaultRealmWithProcessingOption()
    }

    void cleanup() {
        configService.clean()
    }

    void "deleteFastQCInformationFromRawSequenceFile"() {
        given:
        setupData()
        RawSequenceFile rawSequenceFile = DomainFactory.createFastqFile()
        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile(sequenceFile: rawSequenceFile)

        when:
        deletionService.deleteFastQCInformationFromRawSequenceFile(rawSequenceFile)

        then:
        !FastqcProcessedFile.get(fastqcProcessedFile.id)
    }

    void "deleteMetaDataEntryForRawSequenceFile"() {
        given:
        RawSequenceFile rawSequenceFile = DomainFactory.createFastqFile()
        MetaDataEntry metaDataEntry = DomainFactory.createMetaDataEntry(sequenceFile: rawSequenceFile)

        when:
        deletionService.deleteMetaDataEntryForRawSequenceFile(rawSequenceFile)

        then:
        !MetaDataEntry.get(metaDataEntry.id)
    }

    void "testDeleteQualityAssessmentInfoForAbstractBamFile_RoddyBamFile"() {
        given:
        setupData()
        AbstractBamFile abstractBamFile = DomainFactory.createRoddyBamFile()

        QualityAssessmentMergedPass qualityAssessmentPass = DomainFactory.createQualityAssessmentMergedPass(abstractBamFile: abstractBamFile)
        RoddyLibraryQa roddyLibraryQa = DomainFactory.createRoddyLibraryQa(qualityAssessmentMergedPass: qualityAssessmentPass,
                genomeWithoutNCoverageQcBases: 0, referenceLength: 0)
        RoddyMergedBamQa roddyMergedBamQa = DomainFactory.createRoddyMergedBamQa(qualityAssessmentMergedPass: qualityAssessmentPass,
                genomeWithoutNCoverageQcBases: 0, referenceLength: 0)
        RoddySingleLaneQa roddySingleLaneQa = DomainFactory.createRoddySingleLaneQa(seqTrack: abstractBamFile.seqTracks.iterator().next(),
                qualityAssessmentMergedPass: qualityAssessmentPass, genomeWithoutNCoverageQcBases: 0, referenceLength: 0)

        when:
        deletionService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        then:
        !QualityAssessmentMergedPass.get(qualityAssessmentPass.id)
        !RoddyLibraryQa.get(roddyLibraryQa.id)
        !RoddyMergedBamQa.get(roddyMergedBamQa.id)
        !RoddySingleLaneQa.get(roddySingleLaneQa.id)
    }

    void "testDeleteQualityAssessmentInfoForAbstractBamFile_SingleCellBamFile"() {
        given:
        setupData()
        AbstractBamFile abstractBamFile = DomainFactory.proxyCellRanger.createBamFile()

        QualityAssessmentMergedPass qualityAssessmentPass = DomainFactory.createQualityAssessmentMergedPass(abstractBamFile: abstractBamFile)

        when:
        deletionService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        then:
        !QualityAssessmentMergedPass.get(qualityAssessmentPass.id)
    }

    void "testDeleteQualityAssessmentInfoForAbstractBamFile_null"() {
        given:
        setupData()
        AbstractBamFile abstractBamFile = null

        when:
        final shouldFail = new GroovyTestCase().&shouldFail
        String message = shouldFail RuntimeException, {
            deletionService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)
        }

        then:
        message == "The input AbstractBamFile is null"
    }

    void "deleteRawSequenceFile"() {
        given:
        setupData()
        RawSequenceFile rawSequenceFile = DomainFactory.createFastqFile()
        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile(sequenceFile: rawSequenceFile)

        DomainFactory.createMetaDataEntry(sequenceFile: rawSequenceFile)

        String fileFinalPath = lsdfFilesService.getFileFinalPath(rawSequenceFile)
        List<File> expected = [
                fileFinalPath,
                "${fileFinalPath}.md5sum",
                lsdfFilesService.getFileViewByPidPath(rawSequenceFile),
        ].collect { new File(it) }

        when:
        List<File> result = deletionService.deleteRawSequenceFile(rawSequenceFile)

        then:
        expected == result
        !FastqcProcessedFile.get(fastqcProcessedFile.id)
    }

    void "testDeleteAllProcessingInformationAndResultOfOneSeqTrack_RoddyBamFile"() {
        given:
        setupData()
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        roddyBamFile.workPackage.bamFileInProjectFolder = roddyBamFile
        roddyBamFile.workPackage.save(flush: true)

        when:
        deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(roddyBamFile.seqTracks.iterator().next())

        then:
        !RoddyBamFile.get(roddyBamFile.id)
        !MergingWorkPackage.get(roddyBamFile.workPackage.id)
    }

    void "testDeleteAllProcessingInformationAndResultOfOneSeqTrack_SingleCellBamFile"() {
        given:
        setupData()
        SingleCellBamFile singleCellBamFile = DomainFactory.proxyCellRanger.createBamFile()
        singleCellBamFile.workPackage.bamFileInProjectFolder = singleCellBamFile
        singleCellBamFile.workPackage.save(flush: true)

        when:
        deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(singleCellBamFile.seqTracks.iterator().next())

        then:
        !RoddyBamFile.get(singleCellBamFile.id)
        !MergingWorkPackage.get(singleCellBamFile.workPackage.id)
    }

    void "testDeleteSeqTrack"() {
        given:
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        RawSequenceFile rawSequenceFile = DomainFactory.createFastqFile(seqTrack: seqTrack)

        when:
        deletionService.deleteSeqTrack(seqTrack)

        then:
        !SeqTrack.get(seqTrack.id)
        !RawSequenceFile.get(rawSequenceFile.id)
    }

    void "testDeleteSeqTrack_seqTrackIsOnlyLinked"() {
        given:
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(linkedExternally: true)
        DomainFactory.createFastqFile(seqTrack: seqTrack)

        when:
        deletionService.deleteSeqTrack(seqTrack)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("seqTracks only linked")
    }

    void "testDeleteProcessingFilesOfProject_EmptyProject"() {
        given:
        setupData()
        Project project = DomainFactory.createProject()

        when:
        deletionService.deleteProcessingFilesOfProject(project.name, outputFolder)

        then:
        thrown(AssertionError)
    }

    void "testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesMissing"() {
        given:
        setupData()
        SeqTrack seqTrack = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        Project project = seqTrack.project

        when:
        deletionService.deleteProcessingFilesOfProject(project.name, outputFolder)

        then:
        thrown(FileNotFoundException)
    }

    void "testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesAvailable"() {
        given:
        setupData()
        Project project = deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles()

        when:
        deletionService.deleteProcessingFilesOfProject(project.name, outputFolder)

        then:
        noExceptionThrown()
    }

    void "testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesLinked"() {
        given:
        setupData()
        Project project = deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles()
        markFilesAsLinked(SeqTrack.list())

        when:
        deletionService.deleteProcessingFilesOfProject(project.name, outputFolder)

        then:
        thrown(FileNotFoundException)
    }

    void "testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesLinked_Verified"() {
        given:
        setupData()
        Project project = deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles()
        markFilesAsLinked(SeqTrack.list())

        when:
        deletionService.deleteProcessingFilesOfProject(project.name, outputFolder, true)

        then:
        noExceptionThrown()
    }

    void "testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesWithdrawn"() {
        given:
        setupData()
        SeqTrack seqTrack = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        Project project = seqTrack.project
        markFilesAsWithdrawn([seqTrack])

        when:
        deletionService.deleteProcessingFilesOfProject(project.name, outputFolder, true)

        then:
        thrown(FileNotFoundException)
    }

    void "testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesWithdrawn_IgnoreWithdrawn"() {
        given:
        setupData()
        SeqTrack seqTrack = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        Project project = seqTrack.project
        markFilesAsWithdrawn([seqTrack])

        when:
        deletionService.deleteProcessingFilesOfProject(project.name, outputFolder, true, true)

        then:
        noExceptionThrown()
    }

    void "testDeleteProcessingFileSOfProject_NoProcessedData_FastqFilesAvailalbe_explicitSeqTrack"() {
        given:
        setupData()
        SeqTrack st = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        createFastqFiles([st])

        expect:
        [st] == deletionService.deleteProcessingFilesOfProject(st.project.name, outputFolder, true, true, [st])
    }

    void "testDeleteProcessingFileSOfProject_NoProcessedData_FastqFilesAvailalbe_explicitSeqTrackDifferentProject_ShouldFail"() {
        given:
        setupData()
        SeqTrack st = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        createFastqFiles([st])

        Project project = DomainFactory.createProject()

        when:
        deletionService.deleteProcessingFilesOfProject(project.name, outputFolder, true, true, [st])

        then:
        thrown(AssertionError)
    }

    private SeqTrack deleteProcessingFilesOfProject_NoProcessedData_Setup() {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoFastqFiles()

        return seqTrack
    }

    private Project deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles() {
        SeqTrack seqTrack = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        createFastqFiles([seqTrack])

        return seqTrack.project
    }

    private void markFilesAsLinked(List<SeqTrack> seqTracks) {
        seqTracks.each {
            it.linkedExternally = true
            assert it.save(flush: true)
        }
    }

    private void markFilesAsWithdrawn(List<SeqTrack> seqTracks) {
        List<RawSequenceFile> rawSequenceFiles = RawSequenceFile.findAllBySeqTrackInList(seqTracks)
        rawSequenceFiles*.fileWithdrawn = true
        assert rawSequenceFiles*.save(flush: true)
    }

    private void createFastqFiles(List<SeqTrack> seqTracks) {
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance()
        RawSequenceFile.findAllBySeqTrackInList(seqTracks).each {
            it.fastqImportInstance = fastqImportInstance
            assert it.save(flush: true)
            CreateFileHelper.createFile(new File(lsdfFilesService.getFileViewByPidPath(it)))
        }
    }

    private void createFastqFiles(AbstractBamFile bamFile) {
        createFastqFiles(bamFile.containedSeqTracks as List)
    }

    private void dataBaseSetupForBamFiles(AbstractBamFile bamFile, boolean addRealm = true) {
        AbstractMergingWorkPackage mergingWorkPackage = bamFile.mergingWorkPackage
        mergingWorkPackage.bamFileInProjectFolder = bamFile
        assert mergingWorkPackage.save(flush: true)
        Project project = bamFile.project
        if (addRealm) {
            project.realm = DomainFactory.createRealm()
        }
    }

    private RoddyBamFile deleteProcessingFilesOfProject_RBF_Setup() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()

        dataBaseSetupForBamFiles(bamFile)
        createFastqFiles(bamFile)

        File finalBamFile = bamFile.baseDirectory
        CreateFileHelper.createFile(new File(finalBamFile, "test.bam"))

        return bamFile
    }

    private void deleteProcessingFilesOfProject_RBF_Validation() {
        assert AbstractBamFile.list().empty
        assert MergingWorkPackage.list().empty
    }

    void "testDeleteProcessingFilesOfProject_RBF"() {
        given:
        setupData()
        RoddyBamFile bamFile = deleteProcessingFilesOfProject_RBF_Setup()

        File finalBamFile = bamFile.baseDirectory
        Path outputFile = outputFolder.resolve("Delete_${bamFile.project.name}.sh")

        deletionService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder, true)

        assert outputFile.text.contains(finalBamFile.path)

        deleteProcessingFilesOfProject_RBF_Validation()
    }

    void "testDeleteProcessingFilesOfProject_RBF_notVerified"() {
        given:
        setupData()
        RoddyBamFile bamFile = deleteProcessingFilesOfProject_RBF_Setup()

        File finalBamFile = bamFile.baseDirectory
        Path outputFile = outputFolder.resolve("Delete_${bamFile.project.name}.sh")

        deletionService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder)

        assert outputFile.text.contains(finalBamFile.path)

        deleteProcessingFilesOfProject_RBF_Validation()
    }

    private AbstractSnvCallingInstance deleteProcessingFilesOfProject_RBF_SNV_Setup() {
        AbstractSnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        AbstractBamFile tumorBamFiles = snvCallingInstance.sampleType1BamFile
        dataBaseSetupForBamFiles(tumorBamFiles)
        createFastqFiles(tumorBamFiles)

        AbstractBamFile controlBamFiles = snvCallingInstance.sampleType2BamFile
        dataBaseSetupForBamFiles(controlBamFiles, false)
        createFastqFiles(controlBamFiles)

        File snvFolder = fileService.toFile(snvCallingService.getWorkDirectory(snvCallingInstance))
        CreateFileHelper.createFile(new File(snvFolder, "test.vcf"))

        return snvCallingInstance
    }

    void "testDeleteProcessingFilesOfProject_RBF_SNV"() {
        given:
        setupData()
        AbstractSnvCallingInstance snvCallingInstance = deleteProcessingFilesOfProject_RBF_SNV_Setup()

        when:
        deletionService.deleteProcessingFilesOfProject(snvCallingInstance.project.name, outputFolder, true)

        then:
        File snvFolder = snvCallingInstance.samplePair.snvSamplePairPath.absoluteDataManagementPath
        Path outputFile = outputFolder.resolve("Delete_${snvCallingInstance.project.name}.sh")

        outputFile.text.contains(snvFolder.path) && outputFile.text.contains(snvFolder.parent)
        AbstractSnvCallingInstance.list().empty
        SamplePair.list().empty
    }

    void "testDeleteProcessingFilesOfProject_RBF_SNV_notVerified"() {
        given:
        setupData()
        AbstractSnvCallingInstance snvCallingInstance = deleteProcessingFilesOfProject_RBF_SNV_Setup()

        when:
        deletionService.deleteProcessingFilesOfProject(snvCallingInstance.project.name, outputFolder)

        then:
        File snvFolder = snvCallingInstance.samplePair.snvSamplePairPath.absoluteDataManagementPath
        Path outputFile = outputFolder.resolve("Delete_${snvCallingInstance.project.name}.sh")

        outputFile.text.contains(snvFolder.path) && outputFile.text.contains(snvFolder.parent)
        AbstractSnvCallingInstance.list().empty
        SamplePair.list().empty
    }

    private ExternallyProcessedBamFile deleteProcessingFilesOfProject_ExternalBamFilesAttached_Setup() {
        Project project = deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles()
        SeqTrack seqTrack = SeqTrack.createCriteria().get {
            sample {
                individual {
                    eq('project', project)
                }
            }
        }

        ExternallyProcessedBamFile bamFile = DomainFactory.createExternallyProcessedBamFile(
                workPackage: DomainFactory.createExternalMergingWorkPackage(
                        sample: seqTrack.sample,
                        seqType: seqTrack.seqType,
                )
        )
        CreateFileHelper.createFile(bamFile.nonOtpFolder)

        return bamFile
    }

    void "testDeleteProcessingFilesOfProject_ExternalBamFilesAttached"() {
        given:
        setupData()
        ExternallyProcessedBamFile bamFile = deleteProcessingFilesOfProject_ExternalBamFilesAttached_Setup()

        when:
        deletionService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("external merged bam files")
    }

    void "testDeleteProcessingFilesOfProject_ExternalBamFilesAttached_Verified"() {
        given:
        setupData()
        ExternallyProcessedBamFile bamFile = deleteProcessingFilesOfProject_ExternalBamFilesAttached_Setup()

        when:
        deletionService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder, true)

        then:
        File nonOtpFolder = bamFile.nonOtpFolder
        Path outputFile = outputFolder.resolve("Delete_${bamFile.project.name}.sh")
        !outputFile.text.contains(nonOtpFolder.path)
        ExternallyProcessedBamFile.list().contains(bamFile)
    }

    void "testDeleteProcessingFilesOfProject_ExternalBamFilesAttached_nonMergedSeqTrackExists_Verified"() {
        given:
        setupData()
        ExternallyProcessedBamFile bamFile = deleteProcessingFilesOfProject_ExternalBamFilesAttached_Setup()

        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoFastqFiles([sample: bamFile.sample, seqType: bamFile.seqType])
        createFastqFiles([seqTrack])

        when:
        deletionService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder, true)

        then:
        File nonOtpFolder = bamFile.nonOtpFolder
        Path outputFile = outputFolder.resolve("Delete_${bamFile.project.name}.sh")
        !outputFile.text.contains(nonOtpFolder.path)
        ExternallyProcessedBamFile.list().contains(bamFile)
    }
}
