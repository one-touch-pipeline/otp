package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority

import static de.dkfz.tbi.otp.ngsdata.LsdfFilesService.*

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.ngsdata.BedFile
import de.dkfz.tbi.otp.ngsdata.BedFileService
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.MetaDataService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import org.springframework.beans.factory.annotation.Autowired


class ExecutePanCanJob extends AbstractRoddyJob {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    @Autowired
    BedFileService bedFileService

    @Override
    protected String prepareAndReturnWorkflowSpecificCommand(RoddyResult roddyResult, Realm realm) throws Throwable {
        assert roddyResult : "roddyResult must not be null"
        assert realm : "realm must not be null"

        RoddyBamFile roddyBamFile = roddyResult as RoddyBamFile
        RoddyBamFile baseBamFile = roddyBamFile.baseBamFile

        List<File> vbpDataFiles = []

        roddyBamFile.seqTracks.each { SeqTrack seqTrack ->
            List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
            assert 2 == dataFiles.size()
            dataFiles.sort {it.mateNumber}.each { DataFile dataFile ->
                File file = new File(lsdfFilesService.getFileViewByPidPath(dataFile))
                LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
                assert dataFile.fileSize == file.length()
                vbpDataFiles.add(file)
            }
        }

        vbpDataFiles.collate(2).each {
            MetaDataService.ensurePairedSequenceFileNameConsistency(it.first().path, it.last().path)
        }

        String seqTracksToMerge = vbpDataFiles.join(";")

        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(roddyBamFile.project, roddyBamFile.referenceGenome) as File
        assert referenceGenomeFastaFile : "Path to the reference genome file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(referenceGenomeFastaFile)


        File chromosomeStatSizeFile = referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage)
        assert chromosomeStatSizeFile : "Path to the chromosome stat size file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(chromosomeStatSizeFile)

        String analysisIDinConfigFile = executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
        String nameInConfigFile = roddyBamFile.config.getNameUsedInConfig()

        ensureCorrectBaseBamFileIsOnFileSystem(baseBamFile)
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(roddyBamFile.config.configFilePath))

        String additionalCValues = ''
        if (roddyBamFile.seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            BedFile bedFile = roddyBamFile.bedFile
            Realm dataProcessingRealm = configService.getRealmDataProcessing(roddyBamFile.project)
            File bedFilePath = bedFileService.filePath(dataProcessingRealm, bedFile) as File
            additionalCValues += "TARGET_REGIONS_FILE:${bedFilePath},"
            additionalCValues += "TARGETSIZE:${bedFile.targetSize},"
        }


        return executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, nameInConfigFile, analysisIDinConfigFile, realm) +
                    "--cvalues=\"fastq_list:${seqTracksToMerge},"  +
                    "${baseBamFile ? "bam:${baseBamFile.getPathForFurtherProcessing()}," : ""}" +
                    "REFERENCE_GENOME:${referenceGenomeFastaFile}," +
                    "INDEX_PREFIX:${referenceGenomeFastaFile}," +
                    "CHROM_SIZES_FILE:${chromosomeStatSizeFile}," +
                    additionalCValues +
                    "possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}" +
                    "${(roddyBamFile.processingPriority >= ProcessingPriority.FAST_TRACK_PRIORITY) ? ",PBS_AccountName:FASTTRACK" : ""}\""
    }


    @Override
    protected void validate(RoddyResult roddyResult) throws Throwable {
        assert roddyResult : "Input roddyResultObject must not be null"

        RoddyBamFile roddyBamFile = roddyResult as RoddyBamFile

        executeRoddyCommandService.correctPermissions(roddyBamFile)

        try {
            ensureCorrectBaseBamFileIsOnFileSystem(roddyBamFile.baseBamFile)
        } catch (AssertionError e) {
            throw new RuntimeException('The input BAM file seems to have changed on the file system while this job was processing it.', e)
        }

        ['Bam', 'Bai', 'Md5sum'].each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(roddyBamFile."work${it}File")
        }

        ['MergedQA', 'ExecutionStore'].each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(roddyBamFile."work${it}Directory")
        }

        ensureFileIsReadableAndNotEmpty(roddyBamFile.workMergedQAJsonFile)
        if (roddyBamFile.seqType.seqTypeName == SeqTypeNames.EXOME) {
            ensureFileIsReadableAndNotEmpty(roddyBamFile.workMergedQATargetExtractJsonFile)
        }
        roddyBamFile.workSingleLaneQAJsonFiles.values().each {
            ensureFileIsReadableAndNotEmpty(it)
        }

        assert [FileOperationStatus.DECLARED, FileOperationStatus.NEEDS_PROCESSING].contains(roddyBamFile.fileOperationStatus)
        roddyBamFile.fileOperationStatus = FileOperationStatus.NEEDS_PROCESSING
        assert roddyBamFile.save(flush: true)
    }

    void ensureCorrectBaseBamFileIsOnFileSystem(RoddyBamFile baseBamFile) {
        if (baseBamFile) {
            File bamFilePath = baseBamFile.getPathForFurtherProcessing()
            assert bamFilePath.exists()
            assert baseBamFile.fileSize == bamFilePath.length()
        }
    }
}
