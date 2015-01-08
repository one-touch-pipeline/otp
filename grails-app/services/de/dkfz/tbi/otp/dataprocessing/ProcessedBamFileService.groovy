package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.filehandling.BwaLogFileParser
import de.dkfz.tbi.otp.ngsqc.FastqcBasicStatistics
import de.dkfz.tbi.otp.ngsqc.FastqcResultsService

import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import static org.springframework.util.Assert.*

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

class ProcessedBamFileService {

    DataProcessingFilesService dataProcessingFilesService
    ProcessedAlignmentFileService processedAlignmentFileService
    ConfigService configService
    ProcessedSaiFileService processedSaiFileService
    FastqcResultsService fastqcResultsService

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
        String sampleType = seqTrack.sample.sampleType.dirName
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
        return ProcessedBamFile.findByAlignmentPassAndType(alignmentPass, type)
    }

    public ProcessedBamFile findBamFile(long alignmentPassId, String type) {
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
        return ProcessedBamFile.findByAlignmentPassAndType(alignmentPass, type)
    }

    /**
     * @return The size of the BAM file in the file system.
     */
    public long updateBamFile(ProcessedBamFile bamFile) {
        File file = new File(getFilePath(bamFile))
        ensureReadable(file)
        bamFile.fileExists = true
        bamFile.fileSize = file.length()
        bamFile.dateFromFileSystem = new Date(file.lastModified())
        assertSave(bamFile)
        return bamFile.fileSize
    }

    public void updateBamFileIndex(ProcessedBamFile bamFile) {
        String path = baiFilePath(bamFile)
        File file = new File(path)
        ensureReadable(file)
        bamFile.hasIndexFile = true
        assertSave(bamFile)
    }

    private static ensureReadable(final File file) {
        if (!file.canRead()) {
            throw new RuntimeException("Cannot read ${file}")
        }
    }

    public Realm realm(ProcessedBamFile processedBamFile) {
        Project project = project(processedBamFile)
        return configService.getRealmDataProcessing(project)
    }

    public Project project(ProcessedBamFile processedBamFile) {
        return processedBamFile.project
    }

    public SeqType seqType(ProcessedBamFile processedBamFile) {
        return processedBamFile.seqType
    }

    public SeqTrack seqTrack(ProcessedBamFile processedBamFile) {
        return processedBamFile.seqTrack
    }

    public Sample sample(ProcessedBamFile processedBamFile) {
        return processedBamFile.sample
    }

    /**
     * Sets the status of the specified processed BAM file to NEEDS_PROCESSING. This triggers the
     * CreateMergingSetWorkflow.
     */
    public void setNeedsProcessing(final ProcessedBamFile processedBamFile) {
        notNull processedBamFile
        assert [AbstractBamFile.State.DECLARED, AbstractBamFile.State.NEEDS_PROCESSING].contains(processedBamFile.status)
        assert processedBamFile.seqTrack.alignmentState == SeqTrack.DataProcessingState.FINISHED
        processedBamFile.status = AbstractBamFile.State.NEEDS_PROCESSING
        assertSave(processedBamFile)
    }

    /**
     * @return the first available {@link ProcessedBamFile} which needs to be merged and was sorted
     * in the alignment workflow, or <code>null</code> if no such {@link ProcessedBamFile} exists.
     */
    ProcessedBamFile processedBamFileNeedsProcessing() {
        //the oldest bam file will be processed
        List<ProcessedBamFile> processedBamFiles = ProcessedBamFile.findAllByStatusAndTypeAndWithdrawn(State.NEEDS_PROCESSING, BamType.SORTED, false, [sort: "id"])
        return processedBamFiles.find { isMergeable(it) }
    }

    /**
     * checks, if the given {@link ProcessedBamFile} is ready for merging.
     *
     * Therefore the following requirements needs to be <code>true</code>:
     * <ul>
     * <li> The processedBamFile is not withdrawn</li>
     * <li> All {@link SeqTrack}s (exlude withdrawn) with the same {@link Sample} and {@link SeqType} have finished their
     *      alignments, see {@link #isAnyAlignmentPending(Iterable)}</li>
     * <li> No merging is in process for the corresponding {@link Sample} and {@link SeqType}, see
     *      {@link #isMergingInProgress(SeqTrack)}</li>
     * <li> All {@link ProcessedBamFile} (exlude withdrawn) for the {SeqTrack}s with the same {@link Sample}
     *      and {@link SeqType} have to be processable, see {@link #isAnyBamFileNotProcessable(Collection)}</li>
     * </ul>
     */
    public boolean isMergeable(ProcessedBamFile processedBamFile) {
        notNull(processedBamFile)

        if (processedBamFile.withdrawn) {
            return false
        }

        final SeqTrack seqTrack = processedBamFile.seqTrack
        List<SeqTrack> seqTracks = SeqTrack.createCriteria().list {
            eq("sample", seqTrack.sample)
            eq("seqType", seqTrack.seqType)
        }
        /* At least processedBamFile.alignmentPass.seqTrack must be in the result, so if the result
         * is empty, something is seriously broken.
         */
        assert !seqTracks.isEmpty()

        if (isAnyAlignmentPending(seqTracks)) {
            return false
        }
        if (isMergingInProgress(processedBamFile.seqTrack)) {
            return false
        }
        if (isAnyBamFileNotProcessable(seqTracks)) {
            return false
        }
        return true
    }

    /**
     * Checks for not finished alignments of the given seqtracks, ignoring withdrawn seq tracks
     *
     * A seqtrack is handled as withdrawn, if at least one of the corresponding {@link DataFile}s is withdrawn.
     *
     * @param seqTracks the seqTracks to be checked
     * @return <code>true</code>, if for at least one not withdrawn seqtrack the alignment is not finished yet
     */
    public boolean isAnyAlignmentPending(Iterable<SeqTrack> seqTracks) {
        notNull(seqTracks)
        return seqTracks.find { it.alignmentState != SeqTrack.DataProcessingState.FINISHED && !it.withdrawn}
    }

    /**
     * @return whether there is a merging process with the {@link Sample} and {@link SeqType} of the specified
     *      {@link SeqTrack} running at the moment
     */
    public boolean isMergingInProgress(SeqTrack seqTrack) {
        notNull(seqTrack)
        Sample sample = seqTrack.sample
        SeqType seqType = seqTrack.seqType
        MergingWorkPackage workPackage = MergingWorkPackage.findBySampleAndSeqType(sample, seqType)
        if (workPackage) {
            if (MergingSet.findByMergingWorkPackageAndStatusInList(workPackage,
                            [MergingSet.State.DECLARED, MergingSet.State.INPROGRESS, MergingSet.State.NEEDS_PROCESSING])) {
                return true
            }
        }
        return false
    }


    /**
     * @return whether a latest {@link ProcessedBamFile} (exlude withdrawn and old passes) belonging to any of the given
     *      {@link SeqTrack}s exists where the QA is not finished or the {@link AbstractBamFile#status}
     *      is {@link AbstractBamFile.State#DECLARED}.
     */
    public boolean isAnyBamFileNotProcessable(Collection<SeqTrack> seqTracks) {
        notNull(seqTracks)
        if (seqTracks.isEmpty()) {
            return false
        }
        List<AbstractBamFile.State> allowedMergingStates = [
            AbstractBamFile.State.NEEDS_PROCESSING,
            AbstractBamFile.State.INPROGRESS,
            AbstractBamFile.State.PROCESSED,
        ]
        List<ProcessedBamFile> processedBamFiles = ProcessedBamFile.createCriteria().list {
            alignmentPass {
                'in'("seqTrack", seqTracks)
            }
            eq("withdrawn", false) //withdrawn files should be ignored
            or {
                not { 'in'("status", allowedMergingStates) }
            }
        }

        //processedBamFiles of old passes should be ignored
        processedBamFiles = processedBamFiles.findAll {it.mostRecentBamFile}
        return !processedBamFiles.empty
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
     * @param bamFile, {@link ProcessedBamFile}, which shall be merged
     * @return true, if the bamFile is not already assigned to a MergingSet, otherwise false
     */
    boolean notAssignedToMergingSet(ProcessedBamFile bamFile) {
        notNull(bamFile, "the input bam file for the method notAssignedToMergingSet is null")
        return !MergingSetAssignment.findByBamFile(bamFile)
    }

    /**
     * Checks consistency for {@link #deleteProcessingFiles(ProcessedBamFile)}.
     *
     * If there are inconsistencies, details are logged to the thread log (see {@link LogThreadLocal}).
     *
     * @return true if there is no serious inconsistency.
     */
    public boolean checkConsistencyForProcessingFilesDeletion(final ProcessedBamFile bamFile) {
        notNull bamFile
        return dataProcessingFilesService.checkConsistencyWithDatabaseForDeletion(bamFile, new File(getFilePath(bamFile)))
    }

    /**
     * Deletes the *.bam file, the *.bam.bai file and the *.bam_bwaSampeErrorLog.txt file from the
     * "processing" directory on the file system. Sets {@link ProcessedBamFile#fileExists} to
     * <code>false</code> and {@link ProcessedBamFile#deletionDate} to the current time.
     *
     * @return The number of bytes that have been freed on the file system.
     */
    public long deleteProcessingFiles(final ProcessedBamFile bamFile) {
        notNull bamFile
        return dataProcessingFilesService.deleteProcessingFiles(
                bamFile,
                new File(getFilePath(bamFile)),
                new File(baiFilePath(bamFile)),
                new File(bwaSampeErrorLogFilePath(bamFile)),
        )
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }

    public ExomeEnrichmentKit exomeEnrichmentKit(ProcessedBamFile bamFile) {
        notNull(bamFile, 'bam file must not be null')
        SeqTrack seqTrack = bamFile.seqTrack
        isTrue(seqTrack.seqType.name == SeqTypeNames.EXOME.seqTypeName, 'This method must not be called on not exon data')
        // The domain ExomeSeqTrack is new, therefore it is possible that there are many bamFiles,
        // which do not have the connection to the ExomeEnrichtmentKit.
        isInstanceOf(ExomeSeqTrack, seqTrack, "The ExomeEnrichtmentKit is not defined which means that the data were included in OTP as SeqTrack, not as ExomeSeqTrack.")
        return seqTrack.exomeEnrichmentKit
    }

    long getAlignmentReadLength(ProcessedBamFile processedBamFile) {
        notNull(processedBamFile, 'processedBamFile must not be null')
        List<ProcessedSaiFile> saiFiles = ProcessedSaiFile.findAllByAlignmentPass(processedBamFile.alignmentPass)
        return saiFiles.collect { saiFile ->
            String bwaLogFilePath = processedSaiFileService.bwaAlnErrorLogFilePath(saiFile)
            BwaLogFileParser.parseReadNumberFromLog(new File(bwaLogFilePath))
        }.sum()
    }

    long getFastQCReadLength(ProcessedBamFile processedBamFile) {
        notNull(processedBamFile, 'processedBamFile must not be null')
        SeqTrack seqTrack = processedBamFile.alignmentPass.seqTrack
        List<FastqcProcessedFile> fastqcProcessedFiles = fastqcResultsService.fastqcFilesForSeqTrack(seqTrack)
        return fastqcProcessedFiles.collect { FastqcProcessedFile fastqcProcessedFile ->
            exactlyOneElement(FastqcBasicStatistics.findAllByFastqcProcessedFile(fastqcProcessedFile)).totalSequences
        }.sum()
    }
}
