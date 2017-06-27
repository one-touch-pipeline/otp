package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.jobs.utils.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*

class BwaPairingAndSortingJob extends AbstractJobImpl {

    @Autowired
    ConfigService configService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    ProcessedSaiFileService processedSaiFileService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    AlignmentPassService alignmentPassService

    @Autowired
    ProcessingOptionService optionService

    @Override
    public void execute() throws Exception {
        long alignmentPassId = Long.parseLong(getProcessParameterValue())
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
        ProcessedBamFile bamFile = processedBamFileService.createSortedBamFile(alignmentPass)
        Realm realm = alignmentPassService.realmForDataProcessing(alignmentPass)
        String cmd = createCommand(alignmentPass)
        String jobId = clusterJobSchedulerService.executeJob(realm, cmd)
        addOutputParameter(JobParameterKeys.JOB_ID_LIST, jobId)
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
    }

    private String createCommand(AlignmentPass alignmentPass) {
        Project project = alignmentPassService.project(alignmentPass)
        ProcessedBamFile bamFile = processedBamFileService.findSortedBamFile(alignmentPass)
        String sequenceAndSaiFiles = createSequenceAndSaiFiles(alignmentPass)
        String groupHeader = createGroupHeader(alignmentPass.seqTrack)
        String insertSizeOpt = insertSizeOption(alignmentPass.seqTrack)
        String referenceGenomePath = alignmentPassService.referenceGenomePath(alignmentPass)
        String outFilePathNoSuffix = processedBamFileService.getFilePathNoSuffix(bamFile)
        String outFilePath = processedBamFileService.getFilePath(bamFile)
        String samtoolsSortBuffer = optionService.findOptionSafe(OptionName.PIPELINE_OTP_ALIGNMENT_SAMTOOLS_SORT_BUFFER, null, null)
        String numberOfSampeThreads = optionService.findOptionSafe(ProcessingOption.OptionName.PIPELINE_OTP_ALIGNMENT_NUMBER_OF_SAMPE_THREADS, null, null)
        String bwaCommand = ProcessingOptionService.findOptionAssure(ProcessingOption.OptionName.COMMAND_BWA, null, project)
        String samToolsBinary = ProcessingOptionService.findOptionAssure(OptionName.COMMAND_SAMTOOLS, null, null)
        String numberOfSamToolsSortThreads = optionService.findOptionSafe(ProcessingOption.OptionName.PIPELINE_OTP_ALIGNMENT_NUMBER_OF_SAMTOOLS_SORT_THREADS, null, null)
        String mbuffer = ProcessingOptionService.findOptionAssure(ProcessingOption.OptionName.PIPELINE_OTP_ALIGNMENT_MBUFFER_PAIRING_SORTING, null, null)

        String sampeCmd = "${bwaCommand} sampe -P -T ${numberOfSampeThreads} ${insertSizeOpt} -r \"${groupHeader}\" ${referenceGenomePath} ${sequenceAndSaiFiles}"
        String viewCmd = "${samToolsBinary} view -uSbh - "
        String sortCmd = "${samToolsBinary} sort ${numberOfSamToolsSortThreads} ${samtoolsSortBuffer} - ${outFilePathNoSuffix}"
        String mbufferPart = "mbuffer -q ${mbuffer} -l /dev/null"
        String bwaLogFilePath = processedBamFileService.bwaSampeErrorLogFilePath(bamFile)
        String bwaErrorCheckingCmd = BwaErrorHelper.failureCheckScript(outFilePath, bwaLogFilePath)
        String chmodCmd = "chmod 440 ${outFilePath} ${bwaLogFilePath}"
        String cmd = "${sampeCmd} 2> ${bwaLogFilePath} | ${mbufferPart} | ${viewCmd} | ${mbufferPart} | ${sortCmd} ; ${bwaErrorCheckingCmd} ;${chmodCmd}"
        return cmd
    }

    private String createGroupHeader(SeqTrack seqTrack) {
        String pid = seqTrack.sample.individual.pid
        String sampleType = seqTrack.sample.sampleType.name
        String platformName = seqTrack.seqPlatform.name
        String sampleName = "${sampleType}_${pid}"
        String ID = "${seqTrack.run.name}_${seqTrack.laneId}"
        String SM = "sample_${sampleName}"
        String LB = sampleName
        String PL = SAMPlatformLabel.map(platformName).name()
        return "@RG\\tID:${ID}\\tSM:${SM}\\tLB:${LB}\\tPL:${PL}"
    }

    private String insertSizeOption(SeqTrack seqTrack) {
        return optionService.findOptionSafe(
                ProcessingOption.OptionName.PIPELINE_OTP_ALIGNMENT_INSERT_SIZE_CUT_OFF,
                seqTrack.seqType.libraryLayout,
                seqTrack.sample.individual.project
        )
    }

    private String createSequenceAndSaiFiles(AlignmentPass alignmentPass) {
        List<ProcessedSaiFile> saiFiles = ProcessedSaiFile.findAllByAlignmentPassAndFileExists(alignmentPass, true)
        if (saiFiles.size() != 2) {
            throw new ProcessingException("Not paired library. Number of sai files: ${saiFiles.size()}")
        }
        saiFiles = saiFiles.sort {
            it.dataFile.mateNumber
        }
        def (ProcessedSaiFile saiFile1, ProcessedSaiFile saiFile2) = saiFiles
        def (DataFile dataFile1, DataFile dataFile2) = saiFiles*.dataFile
        assert 1 == dataFile1.mateNumber
        assert 2 == dataFile2.mateNumber
        MetaDataService.ensurePairedSequenceFileNameConsistency(dataFile1.fileName, dataFile2.fileName)
        MetaDataService.ensurePairedSequenceFileNameConsistency(dataFile1.vbpFileName, dataFile2.vbpFileName)
        String sai1 = processedSaiFileService.getFilePath(saiFile1)
        String sai2 = processedSaiFileService.getFilePath(saiFile2)
        String seq1 = lsdfFilesService.getFileViewByPidPath(dataFile1)
        String seq2 = lsdfFilesService.getFileViewByPidPath(dataFile2)
        return "${sai1} ${sai2} ${seq1} ${seq2} "
    }
}
