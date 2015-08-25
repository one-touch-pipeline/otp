package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.util.Environment
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.MergingSet.State
import de.dkfz.tbi.otp.ngsdata.*

class ProcessedMergedBamFileQaFileServiceTests {

    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Test(expected = IllegalArgumentException)
    void testQaResultsMd5sumFileBamFileNull() {
        processedMergedBamFileQaFileService.qaResultsMd5sumFile(null)
    }

    @Test
    void testQaResultsMd5sumFile() {
        MergingPass mergingPass = createMergingPass()
        Realm realm = DomainFactory.createRealmDataProcessing(name: mergingPass.project.realmName)
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        String destinationExp = realm.processingRootPath + "/project-dir/results_per_pid/patient/merging//sample-type/seq-type/library/DEFAULT/0/pass0/QualityAssessment/pass1/MD5SUMS"
        String destinationAct = processedMergedBamFileQaFileService.qaResultsMd5sumFile(mergedBamFile)
        assertEquals(destinationExp, destinationAct)
    }

    private MergingPass createMergingPass() {
        Project project = TestData.createProject(
                        name: "project",
                        dirName: "project-dir",
                        realmName: 'DKFZ',
                        )
        assertNotNull(project.save([flush: true]))

        Individual individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        SampleType sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        SeqType seqType = new SeqType(
                        name: "seq-type",
                        libraryLayout: "library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))

        MergingWorkPackage mergingWorkPackage = new TestData().createMergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true]))

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(mergingSet.save([flush: true]))

        MergingPass mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true]))
        return mergingPass
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile(MergingPass mergingPass) {
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                        fileExists: true,
                        type: BamType.MDUP,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED,
                        fileOperationStatus: FileOperationStatus.NEEDS_PROCESSING,
                        md5sum: null,
                        status: AbstractBamFile.State.PROCESSED,
                        ])
        assertNotNull(processedMergedBamFile.save([flush: true]))

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass(
                        abstractMergedBamFile: processedMergedBamFile,
                        identifier: 1,
                        description: 'QualtiyAssessmentMergedPassDescription'
                        )
        assertNotNull(qualityAssessmentMergedPass.save([flush: true]))

        return processedMergedBamFile
    }
}
