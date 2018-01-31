package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.utils.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class ConveyBwaAlignmentJob extends AbstractJobImpl {

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

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
        List<String> jobIds = []
        AlignmentPass.withTransaction {
            AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
            SeqTrack seqTrack = alignmentPass.seqTrack
            List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
            for (DataFile file in files) {
                assert file.fileExists && file.fileSize > 0L
                realm = file.project.realm
                ProcessedSaiFile saiFile = processedSaiFileService.createSaiFile(alignmentPass, file)
                jobIds << sendAlignmentScript(realm, saiFile)
            }
        }
        addOutputParameter(JobParameterKeys.JOB_ID_LIST, jobIds.join(","))
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
    }

    private String sendAlignmentScript(Realm realm, ProcessedSaiFile saiFile) {
        AlignmentPass alignmentPass = saiFile.alignmentPass
        Project project = alignmentPassService.project(alignmentPass)
        String conveyBwaCommand = ProcessingOptionService.findOptionAssure(OptionName.COMMAND_CONVEY_BWA, null, project)
        String dataFilePath = lsdfFilesService.getFileViewByPidPath(saiFile.dataFile)
        String saiFilePath = processedSaiFileService.getFilePath(saiFile)
        String referenceGenomePath = alignmentPassService.referenceGenomePath(alignmentPass)
        String qaSwitch = qualityEncoding(alignmentPass)
        String nCores = optionService.findOptionSafe(OptionName.PIPELINE_OTP_ALIGNMENT_CONVEY_BWA_NUMBER_OF_CORES, null, null)
        String bwaLogFilePath = processedSaiFileService.bwaAlnErrorLogFilePath(saiFile)
        String qParameter = optionService.findOptionSafe(OptionName.PIPELINE_OTP_ALIGNMENT_BWA_QUEUE_PARAMETER, alignmentPassService.seqType(alignmentPass).name, alignmentPassService.project(alignmentPass))
        String bwaCmd = "${conveyBwaCommand} aln ${nCores} ${qaSwitch} ${qParameter} ${referenceGenomePath} ${dataFilePath} -f ${saiFilePath} 2> ${bwaLogFilePath}"
        String bwaErrorCheckingCmd = BwaErrorHelper.failureCheckScript(saiFilePath, bwaLogFilePath)
        String chmodCmd = "chmod 440 ${saiFilePath} ${bwaLogFilePath}"
        String cmd = "${bwaCmd}; ${bwaErrorCheckingCmd}; ${chmodCmd}"
        return clusterJobSchedulerService.executeJob(realm, cmd)
    }

    private String qualityEncoding(AlignmentPass alignmentPass) {
        String type = alignmentPass.seqTrack.qualityEncoding.toString()
        return optionService.findOptionSafe(OptionName.PIPELINE_OTP_ALIGNMENT_CONVEY_BWA_QUALITY_ENCODING, type , null)
    }
}
