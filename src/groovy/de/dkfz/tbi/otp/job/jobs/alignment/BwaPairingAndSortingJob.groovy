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
        log.debug cmd
        String pbsId = executionHelperService.sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsId)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

    private String createCommand(AlignmentPass alignmentPass) {
        String groupHeader = createGroupHeader(alignmentPass.seqTrack)
        String insertSizeOpt = insertSizeOption(alignmentPass.seqTrack)
        String referenceGenomePath = alignmentPassService.referenceGenomePath(alignmentPass)
        String sequenceAndSaiFiles = createSequenceAndSaiFiles(alignmentPass)
        String outFile = getOutputFile(alignmentPass)
        String samtoolsSortBuffer = optionService.findOptionSafe("samtoolsSortBuffer", null, null)
        String sampeCmd = "bwa sampe -P ${insertSizeOpt} ${groupHeader} ${referenceGenomePath} ${sequenceAndSaiFiles}"
        String viewCmd = "samtools view -uSbh - "
        String sortCmd = "samtools sort ${samtoolsSortBuffer} - ${outFile}"
        String chmodCmd = "chmod 440 ${outFile}.bam"
        return "${sampeCmd} | ${viewCmd} | ${sortCmd} ; ${chmodCmd}"
    }

    private String createGroupHeader(SeqTrack seqTrack) {
        String runName = seqTrack.run.name
        String laneName = seqTrack.laneId
        return "-r \"@RG\\tID:${runName}\\tLB:${laneName}\""
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
            throw new ProcessingException("Not paired library ${saiFiles.size()}")
        }
        String sai1 = processedSaiFileService.getFilePath(saiFiles.get(0))
        String sai2 = processedSaiFileService.getFilePath(saiFiles.get(1))
        String seq1 = lsdfFilesService.getFileViewByPidPath(saiFiles.get(0).dataFile)
        String seq2 = lsdfFilesService.getFileViewByPidPath(saiFiles.get(1).dataFile)
        return "${sai1} ${sai2} ${seq1} ${seq2} "
    }

    private String getOutputFile(AlignmentPass alignmentPass) {
        ProcessedBamFile bamFile = processedBamFileService.findSortedBamFile(alignmentPass)
        return processedBamFileService.getFilePathNoSuffix(bamFile)
    }

}