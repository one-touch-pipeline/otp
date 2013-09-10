package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type

import org.junit.*

class AlignmentPassServiceIntegrationTests {

    AlignmentPassService alignmentPassService

    @Test
    void testMaximalIdentifier() {
        SeqTrack seqTrack = createSeqTrack("1")
        ProcessedBamFile processedBamFile = createProcessedBamFile(seqTrack, 0)
        assertEquals(0, alignmentPassService.maximalIdentifier(processedBamFile))

        ProcessedBamFile processedBamFile1 = createProcessedBamFile(seqTrack, 1)
        assertEquals(1, alignmentPassService.maximalIdentifier(processedBamFile))
        assertEquals(1, alignmentPassService.maximalIdentifier(processedBamFile1))

        ProcessedBamFile processedBamFile2 = createProcessedBamFile(seqTrack, 2)
        assertEquals(2, alignmentPassService.maximalIdentifier(processedBamFile))
        assertEquals(2, alignmentPassService.maximalIdentifier(processedBamFile1))
        assertEquals(2, alignmentPassService.maximalIdentifier(processedBamFile2))
    }

    private createSeqTrack(String identifier) {
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
                    type: de.dkfz.tbi.otp.ngsdata.Individual.Type.REAL,
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
                    type: Type.ALIGNMENT
                    )
        assertNotNull(softwareTool.save([flush: true, failOnError: true]))

        SeqTrack seqTrack = new SeqTrack(
                    laneId: "laneId" + identifier,
                    run: run,
                    sample: sample,
                    seqType: seqType,
                    seqPlatform: seqPlatform,
                    pipelineVersion: softwareTool
                    )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))
        return seqTrack
    }

    private ProcessedBamFile createProcessedBamFile(SeqTrack seqTrack, int identifier) {
        AlignmentPass alignmentPass = new AlignmentPass(
            identifier: identifier,
            seqTrack: seqTrack,
            description: "test"
            )
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))

        ProcessedBamFile processedBamFile = new ProcessedBamFile(
            alignmentPass: alignmentPass,
            type: BamType.SORTED,
            status: State.NEEDS_PROCESSING
            )
        assertNotNull(processedBamFile.save([flush: true, failOnError: true]))
        return processedBamFile
    }
}
