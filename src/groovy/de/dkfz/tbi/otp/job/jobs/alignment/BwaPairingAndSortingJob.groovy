package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

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
    ExecutionHelperService executionHelperService

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
        String pbsId = executionHelperService.sendScript(realm, cmd, "bwaPairingAndSortingJob")
        addOutputParameter("__pbsIds", pbsId)
        addOutputParameter("__pbsRealm", realm.id.toString())
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
        String samtoolsSortBuffer = optionService.findOptionSafe("samtoolsSortBuffer", null, null)
        String numberOfSampeThreads = optionService.findOptionSafe("numberOfSampeThreads", null, null)
        String bwaCommand = optionService.findOptionAssure("bwaCommand", null, project)
        String samToolsBinary = optionService.findOptionAssure("samtoolsCommand", null, null)
        String numberOfSamToolsSortThreads = optionService.findOptionSafe("numberOfSamToolsSortThreads", null, null)
        String mbuffer = optionService.findOptionAssure("mbufferPairingSorting", null, null)

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
                "insertSizeCutoff",
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
            it.dataFile.readNumber
        }
        def (ProcessedSaiFile saiFile1, ProcessedSaiFile saiFile2) = saiFiles
        def (DataFile dataFile1, DataFile dataFile2) = saiFiles*.dataFile
        assert 1 == dataFile1.readNumber
        assert 2 == dataFile2.readNumber
        MetaDataService.ensurePairedSequenceFileNameConsistency(dataFile1.fileName, dataFile2.fileName)
        MetaDataService.ensurePairedSequenceFileNameConsistency(dataFile1.vbpFileName, dataFile2.vbpFileName)
        String sai1 = processedSaiFileService.getFilePath(saiFile1)
        String sai2 = processedSaiFileService.getFilePath(saiFile2)
        String seq1 = lsdfFilesService.getFileViewByPidPath(dataFile1)
        String seq2 = lsdfFilesService.getFileViewByPidPath(dataFile2)
        return "${sai1} ${sai2} ${seq1} ${seq2} "
    }
}
