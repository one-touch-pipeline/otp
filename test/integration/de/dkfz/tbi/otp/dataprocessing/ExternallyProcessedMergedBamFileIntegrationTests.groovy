package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class ExternallyProcessedMergedBamFileIntegrationTests {

    Sample sample
    SeqType seqType
    MergingSet mergingSet
    Project project
    Individual individual
    SampleType sampleType
    SoftwareTool softwareTool
    SeqPlatform seqPlatform
    Run run

    @Before
    void setUp() {
        project = new Project(
                        name: "project",
                        dirName: "project-dir",
                        realmName: 'DKFZ',
                        )
        assertNotNull(project.save([flush: true]))

        individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true]))

        sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        seqType = new SeqType(
                        name: "seq-type",
                        libraryLayout: "library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))

        softwareTool = new SoftwareTool(
                        programName: "name",
                        programVersion: "version",
                        qualityCode: "quality",
                        type: SoftwareTool.Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true]))

        seqPlatform = new SeqPlatform(
                        name: "name",
                        model: "model"
                        )
        assertNotNull(seqPlatform.save([flush: true]))

        SeqCenter seqCenter = new SeqCenter(
                        name: "name",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        run = new Run(
                        name: "name",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: Run.StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true]))
    }

    @After
    void tearDown() {
        sample = null
        seqType = null
        mergingSet = null
        project = null
        individual = null
        sampleType = null
        softwareTool = null
        seqPlatform = null
        run = null
    }


    @Test
    void testToString() {
        SeqTrack seqTrack = createSeqTrack("lane no. 1")
        FastqSet fastqSet = new FastqSet(
                seqTracks: [seqTrack]
        )
        assertNotNull(fastqSet.save([flush: true]))
        ExternallyProcessedMergedBamFile bamFile2 = new ExternallyProcessedMergedBamFile(
                type: AbstractBamFile.BamType.SORTED,
                fastqSet: fastqSet
        )
        assertNotNull(bamFile2.save([flush: true]))
        assertEquals("id: 1 (external) <br>sample: mockPid sample-type seqType: seq-type library <br>project: project", bamFile2.toString())
    }


    SeqTrack createSeqTrack(String laneId = "laneId") {
        SeqTrack seqTrack = new SeqTrack(
                        laneId: laneId,
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true]))
        return seqTrack
    }
}
