package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class ConveyBwaAlignmentJob extends AbstractJobImpl {

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    SeqTrackService seqTrackService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ConfigService configService

    @Autowired
    ProcessedSaiFileService processedSaiFileService

    @Autowired
    ProcessingOptionService optionService

    @Autowired
    AlignmentPassService alignmentPassService

    @Override
    public void execute() throws Exception {
        long alignmentPassId = Long.parseLong(getProcessParameterValue())
        Realm realm = null
        List<String> pbsIds = []
        AlignmentPass.withTransaction {
            AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
            SeqTrack seqTrack = alignmentPass.seqTrack
            List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
            for (DataFile file in files) {
                assert file.fileExists && file.fileSize > 0L
                realm = configService.getRealmDataProcessing(file.project)
                ProcessedSaiFile saiFile = processedSaiFileService.createSaiFile(alignmentPass, file)
                pbsIds << sendAlignmentScript(realm, saiFile)
            }
        }
        addOutputParameter("__pbsIds", pbsIds.join(","))
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

    private String sendAlignmentScript(Realm realm, ProcessedSaiFile saiFile) {
        AlignmentPass alignmentPass = saiFile.alignmentPass
        Project project = alignmentPassService.project(alignmentPass)
        String conveyBwaCommand = optionService.findOptionAssure("conveyBwaCommand", null, project)
        String dataFilePath = lsdfFilesService.getFileViewByPidPath(saiFile.dataFile)
        String saiFilePath = processedSaiFileService.getFilePath(saiFile)
        String referenceGenomePath = alignmentPassService.referenceGenomePath(alignmentPass)
        String qaSwitch = qualityEncoding(alignmentPass)
        String nCores = optionService.findOptionSafe("conveyBwaNumberOfCores", null, null)
        String bwaLogFilePath = processedSaiFileService.bwaAlnErrorLogFilePath(saiFile)
        String qParameter = optionService.findOptionSafe("bwaQParameter", alignmentPassService.seqType(alignmentPass).name, alignmentPassService.project(alignmentPass))
        String bwaCmd = "${conveyBwaCommand} aln ${nCores} ${qaSwitch} ${qParameter} ${referenceGenomePath} ${dataFilePath} -f ${saiFilePath} 2> ${bwaLogFilePath}"
        String bwaErrorCheckingCmd = BwaErrorHelper.failureCheckScript(saiFilePath, bwaLogFilePath)
        String chmodCmd = "chmod 440 ${saiFilePath} ${bwaLogFilePath}"
        String cmd = "${bwaCmd}; ${bwaErrorCheckingCmd}; ${chmodCmd}"
        return executionHelperService.sendScript(realm, cmd)
    }

    private String qualityEncoding(AlignmentPass alignmentPass) {
        String type = alignmentPass.seqTrack.qualityEncoding.toString()
        return optionService.findOptionSafe("conveyBwaQualityEncoding", type , null)
    }
}
