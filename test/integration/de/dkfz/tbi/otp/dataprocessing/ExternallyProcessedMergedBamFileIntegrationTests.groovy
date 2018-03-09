package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class ExternallyProcessedMergedBamFileIntegrationTests {

    ExternallyProcessedMergedBamFile bamFile
    Project project
    Individual individual
    SampleType sampleType
    Sample sample
    SeqType seqType
    SeqPlatform seqPlatform
    SeqCenter seqCenter
    Run run
    ReferenceGenome referenceGenome
    ExternalMergingWorkPackage externalMergingWorkPackage

    TestConfigService configService

    @Before
    void setUp() {
        configService = new TestConfigService()

        project = DomainFactory.createProject(
                        name: "project",
                        dirName: "project-dir",
                        )
        assertNotNull(project.save([flush: true]))

        individual = DomainFactory.createIndividual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        sampleType = DomainFactory.createSampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true]))

        sample = DomainFactory.createSample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        seqType = DomainFactory.createSeqType(
                        name: "seq-type",
                        libraryLayout: "library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))
        seqType.refresh()

        referenceGenome = DomainFactory.createReferenceGenome(
                name: "REF_GEN"
        )

        externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage(
                pipeline: DomainFactory.createExternallyProcessedPipelineLazy(),
                sample: sample,
                seqType: seqType,
                referenceGenome: referenceGenome
        )

        bamFile = DomainFactory.createExternallyProcessedMergedBamFile(
                type: AbstractBamFile.BamType.SORTED,
                fileName: "FILE_NAME",
                workPackage: externalMergingWorkPackage
        )
    }

    void cleanup() {
        configService.clean()
    }

    @Test
    void testGetFile() {
        String otpFile = bamFile.getBamFile().absolutePath
        String expectedFile = "${configService.getRootPath()}/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/library/merged-alignment/nonOTP/analysisImport_REF_GEN/FILE_NAME"
        assert otpFile == expectedFile
    }
}
