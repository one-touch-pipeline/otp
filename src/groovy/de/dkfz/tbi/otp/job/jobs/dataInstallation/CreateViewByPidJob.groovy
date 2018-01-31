package de.dkfz.tbi.otp.job.jobs.dataInstallation

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

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
    public void execute() throws Exception {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(getProcessParameterValue()))
        Realm realm = seqTrack.project.realm

        Map map = seqTrack.dataFiles.collectEntries{ DataFile dataFile ->
            linkDataFile(dataFile)
        }
        linkFileUtils.createAndValidateLinks(map, realm)

        seqTrack.dataFiles*.fileLinked = true
        seqTrack.dataFiles*.save(flush: true)
        seqTrack.dataInstallationState = SeqTrack.DataProcessingState.FINISHED
        seqTrack.fastqcState = SeqTrack.DataProcessingState.NOT_STARTED
        assert(seqTrack.save(flush: true))
        succeed()
    }

    private Map<File, File> linkDataFile(DataFile file) {
        String source = lsdfFilesService.getFileFinalPath(file)
        String link = lsdfFilesService.getFileViewByPidPath(file)

        assert source : "No source file could be found"
        assert link : "No link file could be found"

        return [(new File(source)): new File(link)]
    }
}
