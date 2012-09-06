package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class BwaAlignmentJob extends AbstractJobImpl {

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
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
        Realm realm = null
        List<String> pbsIds = []
        SeqTrack seqTrack = alignmentPass.seqTrack
        List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
        for (DataFile file in files) {
            realm = configService.getRealmDataProcessing(file.project)
            ProcessedSaiFile saiFile = processedSaiFileService.createSaiFile(alignmentPass, file)
            pbsIds << sendAlignmentScript(realm, saiFile)
            log.debug realm
        }
        addOutputParameter("__pbsIds", pbsIds.join(","))
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

    private String sendAlignmentScript(Realm realm, ProcessedSaiFile saiFile) {
        AlignmentPass alignmentPass = saiFile.alignmentPass
        String bwaCommand = bwaCommand(alignmentPass)
        String dataFilePath = lsdfFilesService.getFileViewByPidPath(saiFile.dataFile)
        String saiFilePath = processedSaiFileService.getFilePath(saiFile)
        String referenceGenomePath = alignmentPassService.referenceGenomePath(alignmentPass)
        String qaSwitch = qualityEncoding(alignmentPass)
        String nCores = optionService.findOptionSafe("bwaNumberOfCores", null, null)
        // TODO: add option for clipping
        String bwaCmd = "${bwaCommand} aln ${nCores} ${qaSwitch} -f ${saiFilePath} ${referenceGenomePath} ${dataFilePath}"
        String chmodCmd = "chmod 440 ${saiFilePath}"
        String cmd = "${bwaCmd} ; ${chmodCmd}"
        log.debug cmd
        return executionHelperService.sendScript(realm, cmd)
    }

    private String bwaCommand(AlignmentPass alignmentPass) {
        Project project = alignmentPassService.project(alignmentPass)
        String cmd = optionService.findOption("bwaCommand", null, project)
        if (!cmd) {
            throw new ProcessingException("BWA command undefined for project ${project}")
        }
        return cmd
    }

    private String qualityEncoding(AlignmentPass alignmentPass) {
        String type = alignmentPass.seqTrack.qualityEncoding.toString()
        return optionService.findOptionSafe("bwaQualityEncoding", type , null )
    }
}
