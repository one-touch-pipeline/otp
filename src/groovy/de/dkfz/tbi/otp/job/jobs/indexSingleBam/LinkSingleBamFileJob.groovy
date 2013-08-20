package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.*

//This Job is not used anymore in the workflow "IndexSingleBamWorkflow".
//It will be kept because of historical reasons and perhaps can be reused later.
@Deprecated
class LinkSingleBamFileJob extends AbstractJobImpl {

    SeqScan scan

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    MergedAlignmentDataFileService mergedAlignmentDataFileService

    @Autowired
    ExecutionService executionService

    @Override
    public void execute() throws Exception {
        long scanId = Long.parseLong(getProcessParameterValue())
        scan = SeqScan.get(scanId)
        Realm realm = scan.sample.individual.project.realm
        String text = buildScriptText()
        log.debug text
        executionService.executeCommand(realm, text)
    }

    private String buildScriptText() {
        String from = target()
        String to = link()
        String text = "ln -s ${from} ${to}"
        return text
    }

    private String target() {
        SeqTrack track = MergingAssignment.findBySeqScan(scan).seqTrack
        AlignmentLog alignLog = AlignmentLog.findBySeqTrack(track)
        FileType fileType = FileType.findByTypeAndSubType(FileType.Type.ALIGNMENT, "bam")
        DataFile file = DataFile.findByAlignmentLogAndFileType(alignLog, fileType)
        return lsdfFilesService.getFileFinalPath(file)
    }

    private String link() {
        MergingLog mergingLog = MergingLog.findBySeqScan(scan)
        MergedAlignmentDataFile dataFile = MergedAlignmentDataFile.findByMergingLog(mergingLog)
        return mergedAlignmentDataFileService.getFullPath(dataFile)
    }
}
