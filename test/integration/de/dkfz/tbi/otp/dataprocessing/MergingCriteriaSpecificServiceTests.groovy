package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.utils.HelperUtils

import static org.junit.Assert.*

import grails.validation.ValidationException
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage.MergingCriteria
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type

class MergingCriteriaSpecificServiceTests {

    MergingCriteriaSpecificService mergingCriteriaSpecificService

    @Test
    void testBamFilesForMergingCriteriaDEFAULT() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        assertEquals(processedBamFile, processedBamFileAct[0])
    }

    @Test
    void testBamFilesForMergingCriteriaDEFAULTTwoBamFiles() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        processedBamFile.status = State.INPROGRESS
        SeqTrack seqTrack2 = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)
        List<ProcessedBamFile> processedBamFileExp = [processedBamFile, processedBamFile2]
        Set<ProcessedBamFile> processedBamFileSetExp = new HashSet<ProcessedBamFile>(processedBamFileExp)
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        Set<ProcessedBamFile> processedBamFileSetAct = new HashSet<ProcessedBamFile>(processedBamFileAct)
        assertEquals(processedBamFileSetExp, processedBamFileSetAct)
    }

    @Test
    void testBamFilesForMergingCriteriaDEFAULTThreeBamFilesEqualSeqTrack() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        processedBamFile.status = State.INPROGRESS
        SeqTrack seqTrack2 = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)
        AlignmentPass alignmentPass3 = createAlignmentPass(seqTrack2, 1)
        ProcessedBamFile processedBamFile3 = createProcessedbamFile(alignmentPass3)

        List<ProcessedBamFile> processedBamFileExp = [processedBamFile, processedBamFile3]
        Set<ProcessedBamFile> processedBamFileSetExp = new HashSet<ProcessedBamFile>(processedBamFileExp)
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        Set<ProcessedBamFile> processedBamFileSetAct = new HashSet<ProcessedBamFile>(processedBamFileAct)
        assertEquals(processedBamFileSetExp, processedBamFileSetAct)
    }

    @Test(expected = ValidationException)
    void testBamFilesForMergingCriteriaDEFAULTTwoBamFilesNotSorted() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        processedBamFile.type = BamType.RMDUP
        assertNotNull(processedBamFile.save([flush: true, failOnError: true]))
    }

    @Test
    void testBamFilesForMergingCriteriaDEFAULTTwoBamFilesSampleNotEqual() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        processedBamFile.status = State.INPROGRESS
        Sample sample2 = createSample(individual, sampleType)
        SeqTrack seqTrack2 = createSeqTrack(run, sample2, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)

        List<ProcessedBamFile> processedBamFileExp = [processedBamFile]
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        assertEquals(processedBamFileExp, processedBamFileAct)
    }

    @Test
    void testBamFilesForMergingCriteriaDEFAULTTwoBamFilesSeqPlatformNotEqual() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        processedBamFile.status = State.INPROGRESS
        SeqPlatform seqPlatform2 = createSeqPlatform(project, 2)
        SeqTrack seqTrack2 = createSeqTrack(run, sample, seqType, seqPlatform2, softwareTool)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)

        List<ProcessedBamFile> processedBamFileExp = [processedBamFile]
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        assertEquals(processedBamFileExp, processedBamFileAct)
    }

    @Test
    void testBamFilesForMergingCriteriaDEFAULTTwoBamFilesSeqTypeNotEqual() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        processedBamFile.status = State.INPROGRESS
        SeqType seqType2 = createSeqType()
        SeqTrack seqTrack2 = createSeqTrack(run, sample, seqType2, seqPlatform, softwareTool)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)

        List<ProcessedBamFile> processedBamFileExp = [processedBamFile]
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        assertEquals(processedBamFileExp, processedBamFileAct)
    }

    @Test
    void testValidateBamFilesForMergingCriteriaDEFAULT() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)
        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment(mergingSet, processedBamFile)
        assertTrue(mergingCriteriaSpecificService.validateBamFilesForMergingCriteriaDEFAULT(mergingSet))
    }

    @Test
    void testValidateBamFilesForMergingCriteriaDEFAULTTwoBamFiles() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)
        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment(mergingSet, processedBamFile)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack, 1)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)
        MergingSetAssignment mergingSetAssignment2 = createMergingSetAssignment(mergingSet, processedBamFile2)

        assertTrue(mergingCriteriaSpecificService.validateBamFilesForMergingCriteriaDEFAULT(mergingSet))
    }

    @Test
    void testValidateBamFilesForMergingCriteriaDEFAULTWrongPlatform() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)
        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment(mergingSet, processedBamFile)
        SeqPlatform seqPlatform2 = createSeqPlatform(project, 2)
        SeqTrack seqTrack2 = createSeqTrack(run, sample, seqType, seqPlatform2, softwareTool)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)
        MergingSetAssignment mergingSetAssignment2 = createMergingSetAssignment(mergingSet, processedBamFile2)

        assertFalse(mergingCriteriaSpecificService.validateBamFilesForMergingCriteriaDEFAULT(mergingSet))
    }


    @Test
    void testValidateBamFilesForMergingCriteriaDEFAULTUnequalPlatformEqualName() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)
        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment(mergingSet, processedBamFile)
        SeqPlatform seqPlatform2 = createSeqPlatform(project, 1)
        SeqTrack seqTrack2 = createSeqTrack(run, sample, seqType, seqPlatform2, softwareTool)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)
        MergingSetAssignment mergingSetAssignment2 = createMergingSetAssignment(mergingSet, processedBamFile2)

        assertTrue(mergingCriteriaSpecificService.validateBamFilesForMergingCriteriaDEFAULT(mergingSet))
    }


    @Test
    void testValidateBamFilesForMergingCriteriaDEFAULTWrongSample() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)
        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment(mergingSet, processedBamFile)
        Sample sample2 = createSample(individual, sampleType)
        SeqTrack seqTrack2 = createSeqTrack(run, sample2, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)
        MergingSetAssignment mergingSetAssignment2 = createMergingSetAssignment(mergingSet, processedBamFile2)

        assertFalse(mergingCriteriaSpecificService.validateBamFilesForMergingCriteriaDEFAULT(mergingSet))
    }

    @Test
    void testValidateBamFilesForMergingCriteriaDEFAULTWrongSeqType() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)
        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment(mergingSet, processedBamFile)
        SeqType seqType2 = createSeqType()
        SeqTrack seqTrack2 = createSeqTrack(run, sample, seqType2, seqPlatform, softwareTool)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)
        MergingSetAssignment mergingSetAssignment2 = createMergingSetAssignment(mergingSet, processedBamFile2)

        assertFalse(mergingCriteriaSpecificService.validateBamFilesForMergingCriteriaDEFAULT(mergingSet))
    }

    @Test
    void testMethodsForMergingCriteria() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)

        List<MergingCriteria> mergingCriterias = MergingCriteria.values()
        mergingCriterias.each { MergingCriteria mergingCriteria ->
            assertNotNull(mergingCriteriaSpecificService."bamFilesForMergingCriteria${mergingCriteria}"(processedBamFile))
            assertNotNull(mergingCriteriaSpecificService."validateBamFilesForMergingCriteria${mergingCriteria}"(mergingSet))
        }
    }

    //mergedFile -> neues bamFile von gleichem sample, seqType, Platform + mergedFile fertig
    @Test
    void testMergedBamFileForMergingCriteriaSeqTypeSamplePlatformOnlyOneMergedBamFile() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)
        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment(mergingSet, processedBamFile)
        MergingPass mergingPass = createMergingPass(mergingSet, 0)
        ProcessedMergedBamFile processedMergedBamFile = createMergedBamFile(mergingPass)
        ProcessedMergedBamFile mergedBamFileAct = mergingCriteriaSpecificService.mergedBamFileForMergingCriteriaSeqTypeSamplePlatform(mergingWorkPackage, processedBamFile)
        assertEquals(processedMergedBamFile, mergedBamFileAct)
    }

    //mergedFile -> neues bamFile von gleichem sample, seqType, Platform + mergedFile nicht fertig
    @Test
    void testMergedBamFileForMergingCriteriaSeqTypeSamplePlatformOnlyOneMergedBamFileNotFinished() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)
        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment(mergingSet, processedBamFile)
        MergingPass mergingPass = createMergingPass(mergingSet, 0)
        ProcessedMergedBamFile processedMergedBamFile = createMergedBamFile(mergingPass)

        mergingSet.status = MergingSet.State.INPROGRESS
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        assertNull(mergingCriteriaSpecificService.mergedBamFileForMergingCriteriaSeqTypeSamplePlatform(mergingWorkPackage, processedBamFile))
    }

    //mergedFile -> neues bamFile von gleichem sample, seqType, anderer Platform + mergedFile fertig -> kein result file
    @Test
    void testMergedBamFileForMergingCriteriaSeqTypeSamplePlatformOnlyOneMergedBamFileWrongPlatformEqualName() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)
        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment(mergingSet, processedBamFile)
        MergingPass mergingPass = createMergingPass(mergingSet, 0)
        ProcessedMergedBamFile processedMergedBamFile = createMergedBamFile(mergingPass)

        SeqPlatform seqPlatform2 = createSeqPlatform(project, 1)
        SeqTrack seqTrack2 = createSeqTrack(run, sample, seqType, seqPlatform2, softwareTool)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)

        ProcessedMergedBamFile processedMergedBamFileAct = mergingCriteriaSpecificService.mergedBamFileForMergingCriteriaSeqTypeSamplePlatform(mergingWorkPackage, processedBamFile2)
        assertEquals(processedMergedBamFile, processedMergedBamFileAct)
    }

    //mergedFile -> neues bamFile von gleichem sample, seqType, anderer Platform + mergedFile fertig -> kein result file
    @Test
    void testMergedBamFileForMergingCriteriaSeqTypeSamplePlatformOnlyOneMergedBamFileWrongPlatformWrongName() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)
        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment(mergingSet, processedBamFile)
        MergingPass mergingPass = createMergingPass(mergingSet, 0)
        ProcessedMergedBamFile processedMergedBamFile = createMergedBamFile(mergingPass)

        SeqPlatform seqPlatform2 = createSeqPlatform(project, 2)
        SeqTrack seqTrack2 = createSeqTrack(run, sample, seqType, seqPlatform2, softwareTool)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)

        assertNull(mergingCriteriaSpecificService.mergedBamFileForMergingCriteriaSeqTypeSamplePlatform(mergingWorkPackage, processedBamFile2))
    }

    //2 mergedFile mit unterschiedlicher Platform -> neues bamFile von gleichem sample, seqType,  Platform + mergedFile fertig
    @Test
    void testMergedBamFileForMergingCriteriaSeqTypeSamplePlatformTwoMergedBamFileDifferentPlatform() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)
        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment(mergingSet, processedBamFile)
        MergingPass mergingPass = createMergingPass(mergingSet, 0)
        ProcessedMergedBamFile processedMergedBamFile = createMergedBamFile(mergingPass)

        SeqPlatform seqPlatform2 = createSeqPlatform(project, 2)
        SeqTrack seqTrack2 = createSeqTrack(run, sample, seqType, seqPlatform2, softwareTool)
        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)
        MergingWorkPackage mergingWorkPackage2 = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet2 = createMergingSet(mergingWorkPackage2, 1)
        MergingSetAssignment mergingSetAssignment2 = createMergingSetAssignment(mergingSet2, processedBamFile2)
        MergingPass mergingPass2 = createMergingPass(mergingSet2, 1)
        ProcessedMergedBamFile processedMergedBamFile2 = createMergedBamFile(mergingPass2)

        SeqTrack seqTrack3 = createSeqTrack(run, sample, seqType, seqPlatform2, softwareTool)
        AlignmentPass alignmentPass3 = createAlignmentPass(seqTrack3, 0)
        ProcessedBamFile processedBamFile3 = createProcessedbamFile(alignmentPass3)

        ProcessedMergedBamFile mergedBamFileAct = mergingCriteriaSpecificService.mergedBamFileForMergingCriteriaSeqTypeSamplePlatform(mergingWorkPackage2, processedBamFile3)
        assertEquals(processedMergedBamFile2, mergedBamFileAct)
    }

    //2 mergedFile mit gleichen Platformen -> neues bamFile von gleichem sample, seqType,  Platform + mergedFile fertig
    void testMergedBamFileForMergingCriteriaSeqTypeSamplePlatformTwoMergedBamFiles() {
        Project project = createProject()
        Individual individual = createIndividual(project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual, sampleType)
        SeqType seqType = createSeqType()
        SeqPlatform seqPlatform = createSeqPlatform(project, 1)
        SoftwareTool softwareTool = createSoftwareTool()
        SeqCenter seqCenter = createSeqCenter()
        Run run = createRun(seqPlatform, seqCenter)
        SeqTrack seqTrack = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass = createAlignmentPass(seqTrack, 0)
        ProcessedBamFile processedBamFile = createProcessedbamFile(alignmentPass)
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(sample, seqType)
        MergingSet mergingSet = createMergingSet(mergingWorkPackage, 0)
        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment(mergingSet, processedBamFile)
        MergingPass mergingPass = createMergingPass(mergingSet, 0)
        ProcessedMergedBamFile processedMergedBamFile = createMergedBamFile(mergingPass)

        AlignmentPass alignmentPass2 = createAlignmentPass(seqTrack, 1)
        ProcessedBamFile processedBamFile2 = createProcessedbamFile(alignmentPass2)
        MergingSet mergingSet2 = createMergingSet(mergingWorkPackage, 1)
        MergingSetAssignment mergingSetAssignment2 = createMergingSetAssignment(mergingSet2, processedBamFile2)
        MergingPass mergingPass2 = createMergingPass(mergingSet2, 0)
        ProcessedMergedBamFile processedMergedBamFile2 = createMergedBamFile(mergingPass2)

        AlignmentPass alignmentPass3 = createAlignmentPass(seqTrack, 2)
        ProcessedBamFile processedBamFile3 = createProcessedbamFile(alignmentPass3)
        MergingSet mergingSet3 = createMergingSet(mergingWorkPackage, 2)
        MergingSetAssignment mergingSetAssignment3 = createMergingSetAssignment(mergingSet3, processedBamFile3)
        MergingPass mergingPass3 = createMergingPass(mergingSet3, 0)
        ProcessedMergedBamFile processedMergedBamFile3 = createMergedBamFile(mergingPass3)

        AlignmentPass alignmentPass4 = createAlignmentPass(seqTrack, 3)
        ProcessedBamFile processedBamFile4 = createProcessedbamFile(alignmentPass4)
        MergingSetAssignment mergingSetAssignment4 = createMergingSetAssignment(mergingSet3, processedBamFile4)
        MergingPass mergingPass4 = createMergingPass(mergingSet3, 1)
        ProcessedMergedBamFile processedMergedBamFile4 = createMergedBamFile(mergingPass4)

        SeqTrack seqTrack2 = createSeqTrack(run, sample, seqType, seqPlatform, softwareTool)
        AlignmentPass alignmentPass5 = createAlignmentPass(seqTrack2, 0)
        ProcessedBamFile processedBamFile5 = createProcessedbamFile(alignmentPass5)

        ProcessedMergedBamFile mergedBamFileAct = mergingCriteriaSpecificService.mergedBamFileForMergingCriteriaSeqTypeSamplePlatform(mergingWorkPackage, processedBamFile5)
        assertEquals(processedMergedBamFile4, mergedBamFileAct)
    }

    Project createProject() {
        Project project = new Project(
                        name: "name",
                        dirName: "dirName",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true, failOnError: true]))
        return project
    }

    SeqPlatform createSeqPlatform(Project project, int identifier) {
        SeqPlatform seqPlatform = new SeqPlatform(
                        name: "name" + identifier,
                        model: "model"
                        )
        assertNotNull(seqPlatform.save([flush: true, failOnError: true]))
        return seqPlatform
    }

    SeqCenter createSeqCenter() {
        SeqCenter seqCenter = new SeqCenter(
                        name: "name",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))
        return seqCenter
    }

    Run createRun(SeqPlatform seqPlatform, SeqCenter seqCenter) {
        Run run = new Run(
                        name: "name",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true, failOnError: true]))
        return run
    }

    Individual createIndividual(Project project) {
        Individual individual = new Individual(
                        pid: "pid",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: de.dkfz.tbi.otp.ngsdata.Individual.Type.REAL,
                        project: project
                        )
        assertNotNull(individual.save([flush: true, failOnError: true]))
        return individual
    }

    SampleType createSampleType() {
        SampleType sampleType = new SampleType(
                        name: "name"
                        )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))
        return sampleType
    }

    Sample createSample(Individual individual, SampleType sampleType) {
        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true, failOnError: true]))
        return sample
    }

    SeqType createSeqType() {
        SeqType seqType = new SeqType(
                        name: "name${HelperUtils.uniqueString}",
                        libraryLayout: "library",
                        dirName: "dirName${HelperUtils.uniqueString}"
                        )
        assertNotNull(seqType.save([flush: true, failOnError: true]))
        return seqType
    }

    SoftwareTool createSoftwareTool() {
        SoftwareTool softwareTool = new SoftwareTool(
                        programName: "name",
                        programVersion: "version",
                        qualityCode: "quality",
                        type: Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true, failOnError: true]))
        return softwareTool
    }

    SeqTrack createSeqTrack(Run run, Sample sample, SeqType seqType, SeqPlatform seqPlatform, SoftwareTool softwareTool) {
        SeqTrack seqTrack = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))
        return seqTrack
    }

    AlignmentPass createAlignmentPass(SeqTrack seqTrack, int identifier) {
        AlignmentPass alignmentPass = new AlignmentPass(
                        identifier: identifier,
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))
        return alignmentPass
    }

    ProcessedBamFile createProcessedbamFile(AlignmentPass alignmentPass) {
        ProcessedBamFile processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(processedBamFile.save([flush: true, failOnError: true]))
        return processedBamFile
    }

    MergingWorkPackage createMergingWorkPackage(Sample sample, SeqType seqType) {
        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true, failOnError: true]))
        return mergingWorkPackage
    }

    MergingSet createMergingSet(MergingWorkPackage mergingWorkPackage, int identifier) {
        MergingSet mergingSet = new MergingSet(
                        identifier: identifier,
                        mergingWorkPackage: mergingWorkPackage,
                        status: MergingSet.State.PROCESSED
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))
        return mergingSet
    }

    MergingSetAssignment createMergingSetAssignment(MergingSet mergingSet, ProcessedBamFile processedBamFile) {
        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile
                        )
        assertNotNull(mergingSetAssignment.save([flush: true, failOnError: true]))
        return mergingSetAssignment
    }

    MergingPass createMergingPass(MergingSet mergingSet, int identifier) {
        MergingPass mergingPass = new MergingPass(
                        identifier: identifier,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true, failOnError: true]))
        return mergingPass
    }

    ProcessedMergedBamFile createMergedBamFile(MergingPass mergingPass) {
        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        type: BamType.MDUP,
                        status: State.PROCESSED
                        )
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))
        return processedMergedBamFile
    }
}
