package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.Before
import org.junit.Test

class ExternallyProcessedMergedBamFileIntegrationTests {

    ExternallyProcessedMergedBamFile bamFile

    @Before
    void setUp() {
        Project project = DomainFactory.createProject(
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
        seqType.refresh()

        SoftwareTool softwareTool = new SoftwareTool(
                        programName: "name",
                        programVersion: "version",
                        type: SoftwareTool.Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true]))

        SeqPlatform seqPlatform = SeqPlatform.build()

        SeqCenter seqCenter = new SeqCenter(
                        name: "name",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        Run run = new Run(
                        name: "name",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        )
        assertNotNull(run.save([flush: true]))

        Realm realm1 = Realm.build(
                name: 'DKFZ',
                cluster: Realm.Cluster.DKFZ,
                env: 'test',
                operationType: Realm.OperationType.DATA_MANAGEMENT,
        )

        ReferenceGenome referenceGenome = ReferenceGenome.build(
                name: "REF_GEN"
        )

        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.build(
                pipeline: Pipeline.build(name: Pipeline.Name.EXTERNALLY_PROCESSED),
                imported: true,
                seqPlatformGroup: seqPlatform.seqPlatformGroup,
                sample: sample,
                seqType: seqType,
                referenceGenome: referenceGenome
        )

        SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(mergingWorkPackage)

        FastqSet fastqSet = FastqSet.build(
                seqTracks: [seqTrack]
        )

        bamFile = ExternallyProcessedMergedBamFile.build(
                type: AbstractBamFile.BamType.SORTED,
                fastqSet: fastqSet,
                fileName: "FILE_NAME",
                source: "SOURCE",
                workPackage: mergingWorkPackage
        )

        //assert !bamFile.containedSeqTracks.any { it.withdrawn }
    }

    @Test
    void testGetFilePath() {
        OtpPath otpPath = bamFile.getFilePath()
        assert otpPath.project == bamFile.project
        assert otpPath.relativePath == new File("project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/library/merged-alignment/nonOTP/SOURCE_REF_GEN/FILE_NAME")
    }
}
