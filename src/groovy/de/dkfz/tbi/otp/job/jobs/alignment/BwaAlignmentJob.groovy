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

/*
 * the very first version of bwaAlignment created by Sylwester running with binary bwa.
 * The code is not deleted due to possible usage in the future.
 */
@Component
@Scope("prototype")
@UseJobLog
class BwaAlignmentJob extends AbstractJobImpl {

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    SeqTrackService seqTrackService

    @Autowired
    LsdfFilesService lsdfFilesService

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
        List<String> jobIds = []
        SeqTrack seqTrack = alignmentPass.seqTrack
        List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
        for (DataFile file in files) {
            assert file.fileExists && file.fileSize > 0L
            realm = file.project.realm
            ProcessedSaiFile saiFile = processedSaiFileService.createSaiFile(alignmentPass, file)
            jobIds << sendAlignmentScript(realm, saiFile)
        }
        addOutputParameter(JobParameterKeys.JOB_ID_LIST, jobIds.join(","))
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
    }

    private String sendAlignmentScript(Realm realm, ProcessedSaiFile saiFile) {
        AlignmentPass alignmentPass = saiFile.alignmentPass
        String bwaCommand = bwaCommand(alignmentPass)
        String dataFilePath = lsdfFilesService.getFileViewByPidPath(saiFile.dataFile)
        String saiFilePath = processedSaiFileService.getFilePath(saiFile)
        String referenceGenomePath = alignmentPassService.referenceGenomePath(alignmentPass)
        String qaSwitch = qualityEncoding(alignmentPass)
        String nCores = optionService.findOptionSafe(OptionName.PIPELINE_OTP_ALIGNMENT_BWA_NUMBER_OF_CORES, null, null)
        String qParameter = optionService.findOptionSafe(OptionName.PIPELINE_OTP_ALIGNMENT_BWA_QUEUE_PARAMETER, alignmentPassService.seqType(alignmentPass).name, alignmentPassService.project(alignmentPass))
        // TODO: add option for clipping
        String bwaCmd = "${bwaCommand} aln ${nCores} ${qaSwitch} ${qParameter} -f ${saiFilePath} ${referenceGenomePath} ${dataFilePath}"
        String chmodCmd = "chmod 440 ${saiFilePath}"
        String cmd = "${bwaCmd} ; ${chmodCmd}"
        return clusterJobSchedulerService.executeJob(realm, cmd)
    }

    private String bwaCommand(AlignmentPass alignmentPass) {
        Project project = alignmentPassService.project(alignmentPass)
        String cmd = optionService.findOption(OptionName.COMMAND_CONVEY_BWA, null, project)
        if (!cmd) {
            throw new ProcessingException("BWA command undefined for project ${project}")
        }
        return cmd
    }

    private String qualityEncoding(AlignmentPass alignmentPass) {
        String type = alignmentPass.seqTrack.qualityEncoding.toString()
        return optionService.findOptionSafe(OptionName.PIPELINE_OTP_ALIGNMENT_CONVEY_BWA_QUALITY_ENCODING, type , null )
    }
}
