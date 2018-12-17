package de.dkfz.tbi.otp.job.jobs.dataInstallation

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.ResumableJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkFileUtils

@Component
@Scope("prototype")
@ResumableJob
@UseJobLog
class CreateViewByPidJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ConfigService configService

    @Autowired
    LinkFileUtils linkFileUtils


    @Override
    void execute() throws Exception {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(getProcessParameterValue()))
        Realm realm = seqTrack.project.realm

        Map map = seqTrack.dataFiles.collectEntries { DataFile dataFile ->
            linkDataFile(dataFile)
        }
        linkFileUtils.createAndValidateLinks(map, realm)

        seqTrack.dataFiles*.fileLinked = true
        seqTrack.dataFiles*.dateLastChecked = new Date()
        seqTrack.dataFiles*.save(flush: true)
        seqTrack.dataInstallationState = SeqTrack.DataProcessingState.FINISHED
        seqTrack.fastqcState = SeqTrack.DataProcessingState.NOT_STARTED
        assert (seqTrack.save(flush: true))
        succeed()
    }

    private Map<File, File> linkDataFile(DataFile file) {
        String source = lsdfFilesService.getFileFinalPath(file)
        String link = lsdfFilesService.getFileViewByPidPath(file)

        assert source: "No source file could be found"
        assert link: "No link file could be found"

        return [(new File(source)): new File(link)]
    }
}
