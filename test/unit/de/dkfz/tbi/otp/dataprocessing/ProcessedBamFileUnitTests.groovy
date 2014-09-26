package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Individual.Type
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm

@TestFor(ProcessedBamFile)
@Mock([Project, SeqPlatform, SeqCenter, Run, Individual, SampleType, Sample, SoftwareTool, SeqTrack, AlignmentPass, SeqType])
class ProcessedBamFileUnitTests {

    AlignmentPass alignmentPass

    @Before
    void setUp() {
        Project project = new Project(
            name: "name",
            dirName: "dirName",
            realmName: "realmName"
            )
        assertNotNull(project.save([flush: true, failOnError: true]))

        SeqPlatform seqPlatform = new SeqPlatform(
            name: "name",
            model: "model"
            )
        assertNotNull(seqPlatform.save([flush: true, failOnError: true]))

        SeqCenter seqCenter = new SeqCenter(
            name: "name",
            dirName: "dirName"
            )
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))

        Run run = new Run(
            name: "name",
            seqCenter: seqCenter,
            seqPlatform: seqPlatform,
            storageRealm: StorageRealm.DKFZ
            )
        assertNotNull(run.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
            pid: "pid",
            mockPid: "mockPid",
            mockFullName: "mockFullName",
            type: Type.REAL,
            project: project
            )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
            name: "name"
            )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        Sample sample = new Sample(
            individual: individual,
            sampleType: sampleType
        )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        SeqType seqType = new SeqType(
            name: "name",
            libraryLayout: "library",
            dirName: "dirName"
            )
        assertNotNull(seqType.save([flush: true, failOnError: true]))

        SoftwareTool softwareTool = new SoftwareTool(
            programName: "name",
            programVersion: "version",
            qualityCode: "quality",
            type: de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type.ALIGNMENT
            )
        assertNotNull(softwareTool.save([flush: true, failOnError: true]))

        SeqTrack seqTrack = new SeqTrack(
            laneId: "laneId",
            run: run,
            sample: sample,
            seqType: seqType,
            seqPlatform: seqPlatform,
            pipelineVersion: softwareTool
            )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))

        alignmentPass = new AlignmentPass(
            identifier: 1,
            seqTrack: seqTrack,
            description: "test"
            )
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))
    }

    @After
    void tearDown() {
        alignmentPass = null
    }

    void testSave() {
        ProcessedBamFile bamFile = new ProcessedBamFile(
            type: BamType.SORTED,
            alignmentPass: alignmentPass,
            withdrawn: false,
            status: State.DECLARED
        )
        assertTrue(bamFile.validate())
        assertNotNull(bamFile.save([flush: true, failOnError: true]))

        bamFile.withdrawn = true
        assertTrue(bamFile.validate())
        assertNotNull(bamFile.save([flush: true, failOnError: true]))

        bamFile.status = State.NEEDS_PROCESSING
        assertFalse(bamFile.validate())

        bamFile.withdrawn = false
        assertTrue(bamFile.validate())
        assertNotNull(bamFile.save([flush: true, failOnError: true]))

        bamFile.type = BamType.RMDUP
        assertFalse(bamFile.validate())
    }
}
