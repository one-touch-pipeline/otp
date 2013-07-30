package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile.State
import de.dkfz.tbi.otp.ngsdata.*

class ProcessedBamFileService {

    ProcessedAlignmentFileService processedAlignmentFileService
    ConfigService configService

    public String getFilePath(ProcessedBamFile bamFile) {
        String dir = getDirectory(bamFile)
        String filename = getFileName(bamFile)
        return "${dir}/${filename}"
    }

    public String getFilePathNoSuffix(ProcessedBamFile bamFile) {
        String dir = getDirectory(bamFile)
        String filename = getFileNameNoSuffix(bamFile)
        return "${dir}/${filename}"
    }

    /**
     * Retrieves the path to a log file used by bwa sampe
     * (Although is not Philosophy of OTP to keep track of log files,
     * it is required by bwa since it produces not empty output files
     * even when it fails, and so we need to analyse the log file contents too)
     *
     * @param saiFile processed bam file object
     * @return Path to the outputted error file produced by bwa sampe
     */
    public String bwaSampeErrorLogFilePath(ProcessedBamFile bamFile) {
        return "${getFilePath(bamFile)}_bwaSampeErrorLog.txt"
    }

    public String baiFilePath(ProcessedBamFile bamFile) {
        return "${getFilePath(bamFile)}.bai"
    }

    public String getDirectory(ProcessedBamFile bamFile) {
        return processedAlignmentFileService.getDirectory(bamFile.alignmentPass)
    }

    public String getFileName(ProcessedBamFile bamFile) {
        String body = getFileNameNoSuffix(bamFile)
        return "${body}.bam"
    }

    public String getFileNameNoSuffix(ProcessedBamFile bamFile) {
        SeqTrack seqTrack = bamFile.alignmentPass.seqTrack
        String sampleType = seqTrack.sample.sampleType.name.toLowerCase()
        String runName = seqTrack.run.name
        String lane = seqTrack.laneId
        String layout = seqTrack.seqType.libraryLayout
        String suffix = ""
        switch (bamFile.type) {
            case AbstractBamFile.BamType.SORTED:
                suffix = ".sorted"
                break
            case AbstractBamFile.BamType.RMDUP:
                suffix = ".sorted.rmdup"
                break
            case AbstractBamFile.BamType.MDUP:
                suffix = ".sorted.mdup"
                break
        }
        return "${sampleType}_${runName}_s_${lane}_${layout}${suffix}"
    }

    public ProcessedBamFile createSortedBamFile(AlignmentPass alignmentPass) {
        return createBamFile(alignmentPass, AbstractBamFile.BamType.SORTED)
    }

    private ProcessedBamFile createBamFile(AlignmentPass alignmentPass, AbstractBamFile.BamType type) {
        ProcessedBamFile pbf = new ProcessedBamFile(
            alignmentPass: alignmentPass,
            type: type
        )
        assert(pbf.save(flush: true))
        return pbf
    }

    public ProcessedBamFile findSortedBamFile(AlignmentPass alignmentPass) {
        def type = AbstractBamFile.BamType.SORTED
        return findBamFile(alignmentPass, type)
    }

    private ProcessedBamFile findBamFile(AlignmentPass alignmentPass, AbstractBamFile.BamType type) {
        return ProcessedBamFile.findByAlignmentPassAndType(alignmentPass, type)
    }

    public ProcessedBamFile findBamFile(long alignmentPassId, String type) {
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
        return ProcessedBamFile.findByAlignmentPassAndType(alignmentPass, type)
    }

    public boolean updateBamFile(ProcessedBamFile bamFile) {
        File file = new File(getFilePath(bamFile))
        if (!file.canRead()) {
            return false
        }
        bamFile.fileExists = true
        bamFile.fileSize = file.length()
        bamFile.dateFromFileSystem = new Date(file.lastModified())
        assertSave(bamFile)
        return bamFile.fileSize
    }

    public boolean updateBamFileIndex(ProcessedBamFile bamFile) {
        String path = baiFilePath(bamFile)
        File file = new File(path)
        if (!file.canRead()) {
            return false
        }
        bamFile.hasIndexFile = true
        assertSave(bamFile)
        return true
    }

    public Realm realm(ProcessedBamFile processedBamFile) {
        Project project = project(processedBamFile)
        return configService.getRealmDataProcessing(project)
    }

    public Project project(ProcessedBamFile processedBamFile) {
        return processedBamFile.alignmentPass.seqTrack.sample.individual.project
    }

    public SeqType seqType(ProcessedBamFile processedBamFile) {
        return processedBamFile.alignmentPass.seqTrack.seqType
    }

    /**
     * @return the first available {@link ProcessedBamFile}, which needs to be merged and was sorted in the alignment workflow
     */
    ProcessedBamFile processedBamFileNeedsProcessing() {
        ProcessedBamFile processedBamFile = ProcessedBamFile.findByStatusAndTypeAndWithdrawn(State.NEEDS_PROCESSING, BamType.SORTED, false)
        return processedBamFile
    }

    /**
     * @param bamFile, which is in progress to be assigned to a {@link MergingSet}
     */
    void blockedForAssigningToMergingSet(ProcessedBamFile bamFile) {
        notNull(bamFile, "the input bam file for the method blockedForAssigningToMergingSet is null")
        bamFile.status = State.INPROGRESS
        assertSave(bamFile)
    }

    /**
     * @param bamFile, which was assigned to a {@link MergingSet}
     */
    void assignedToMergingSet(ProcessedBamFile bamFile) {
        notNull(bamFile, "the input bam file for the method assignedToMergingSet is null")
        bamFile.status = State.PROCESSED
        assertSave(bamFile)
    }

    /**
     * @param bamFile, {@link ProcessedBamFile}, which shall be merged
     * @return true, if the bamFile is not already assigned to a MergingSet, otherwise false
     */
    boolean notAssignedToMergingSet(ProcessedBamFile bamFile) {
        notNull(bamFile, "the input bam file for the method notAssignedToMergingSet is null")
        return !MergingSetAssignment.findByBamFile(bamFile)
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }

    public List<ProcessedBamFile> findByMergingSet(MergingSet mergingSet) {
        notNull(mergingSet, "The parameter merging set is not allowed to be null")
        return MergingSetAssignment.findAllByMergingSet(mergingSet)*.bamFile
    }

    public List<ProcessedBamFile> findByProcessedMergedBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "The parameter processedMergedBamFile is not allowed to be null")
        return findByMergingSet(processedMergedBamFile.mergingPass.mergingSet)
    }

}
