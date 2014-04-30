package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*


@TestFor(ProcessedMergedBamFileService)
@Mock([MergingPass, MergingSet, MergingWorkPackage,
    Sample, SampleType, SeqType, Individual, Project, ProcessedMergedBamFile])
class ProcessedMergedBamFileServiceUnitTests {

    TestData testData = new TestData()

    Sample sample

    @Test
    void testExomeEnrichmentKitCorrect() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack, mergedBamFile)
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { input.bamFiles }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        assertEquals(input.kit, service.exomeEnrichmentKit(mergedBamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitNullInput() {
        service.exomeEnrichmentKit(null)
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitNotExomSeqType() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.WHOLE_GENOME.seqTypeName, ExomeSeqTrack, mergedBamFile)
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { input.bamFiles }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        service.exomeEnrichmentKit(mergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitNoSingleLaneBamFiles() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack, mergedBamFile)
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { [] }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        service.exomeEnrichmentKit(mergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitDiffSeqTrackTypes() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack, mergedBamFile)
        SeqTrack seqTrack = new SeqTrack()
        seqTrack.sample = input.sample
        seqTrack.laneId = "1"
        seqTrack.run = input.run
        seqTrack.seqType = input.seqType
        input.bamFiles[2].alignmentPass.seqTrack = seqTrack
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { input.bamFiles }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        service.exomeEnrichmentKit(mergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitDiffKits() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        ExomeEnrichmentKit kit = new ExomeEnrichmentKit(name: 'kit2')
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack, mergedBamFile)
        input.bamFiles[2].alignmentPass.seqTrack.exomeEnrichmentKit = kit
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { input.bamFiles }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        service.exomeEnrichmentKit(mergedBamFile)
    }

    private Map createKitAndSingleLaneBamFiles(String seqTypeName, Class seqTypeClass, ProcessedMergedBamFile processegMergedBamFile) {
        assert seqTypeClass == SeqTrack || seqTypeClass == ExomeSeqTrack
        List bamFiles = []
        SeqType seqType = testData.createSeqType([name: seqTypeName, dirName: "/tmp"])
        assert seqType.save()
        Sample sample = createSampleAndDBConnections()
        mergingPassAndDBConnections(processegMergedBamFile, sample, seqType)
        ExomeEnrichmentKit kit = new ExomeEnrichmentKit(name: 'kit1')
        Run run = new Run(name: 'run')
        3.times {
            SeqTrack seqTrack
            if (seqTypeClass == SeqTrack) {
                seqTrack = new SeqTrack()
            }
            if (seqTypeClass == ExomeSeqTrack) {
                seqTrack = new ExomeSeqTrack(exomeEnrichmentKit: kit)
            }
            seqTrack.laneId = "1"
            seqTrack.run = run
            seqTrack.seqType = seqType
            seqTrack.sample = sample
            AlignmentPass pass = new AlignmentPass(seqTrack: seqTrack)
            ProcessedBamFile bamFile = new ProcessedBamFile(alignmentPass: pass)
            bamFiles << bamFile
        }
        ProcessedMergedBamFileService.metaClass.seqType = { ProcessedMergedBamFile bamFile -> seqType }
        return [kit:kit, bamFiles:bamFiles, run: run, sample: sample, seqType: seqType]
    }


    private Sample createSampleAndDBConnections() {
        Project project = testData.createProject()
        assert project.save()
        Individual individual = testData.createIndividual([project: project])
        assert individual.save()
        SampleType sampleType = testData.createSampleType()
        assert sampleType.save()
        Sample sample = testData.createSample([individual: individual, sampleType: sampleType])
        assert sample.save()
        return sample
    }


    private void mergingPassAndDBConnections(ProcessedMergedBamFile processedMergedBamFile, Sample sample, SeqType seqType) {
        MergingWorkPackage mergingWorkPackage = testData.createMergingWorkPackage([sample: sample, seqType: seqType])
        assert mergingWorkPackage.save()
        MergingSet mergingSet = testData.createMergingSet([mergingWorkPackage: mergingWorkPackage])
        assert mergingSet.save()
        MergingPass mergingPass = testData.createMergingPass([mergingSet: mergingSet])
        assert mergingPass.save()

        processedMergedBamFile.mergingPass = mergingPass
        processedMergedBamFile.fileExists = true
        processedMergedBamFile.dateFromFileSystem = new Date()
    }
}
