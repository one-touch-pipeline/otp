package workflows.alignment

import workflows.WorkflowTestCase

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.ngsdata.*

abstract class AbstractAlignmentWorkflowTest extends WorkflowTestCase {
    LsdfFilesService lsdfFilesService


    void linkFastqFiles(SeqTrack seqTrack, List<File> testFastqFiles) {
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        assert seqTrack.seqType.libraryLayout.mateCount == dataFiles.size()

        Map<File, File> sourceLinkMap = [:]
        dataFiles.eachWithIndex { dataFile, index ->
            File sourceFastqFile = testFastqFiles[index]
            assert sourceFastqFile.exists()
            dataFile.fileSize = sourceFastqFile.length()
            File linkFastqFile = new File(lsdfFilesService.getFileFinalPath(dataFile))
            sourceLinkMap.put(sourceFastqFile, linkFastqFile)
            File linkViewByPidFastqFile = new File(lsdfFilesService.getFileViewByPidPath(dataFile))
            sourceLinkMap.put(linkFastqFile, linkViewByPidFastqFile)
        }
        createDirectories(sourceLinkMap.values()*.parentFile.unique(), TEST_DATA_MODE_DIR)
        linkFileUtils.createAndValidateLinks(sourceLinkMap, realm)
    }

    void setUpRefGenomeDir(MergingWorkPackage workPackage, File refGenDir) {
        File linkRefGenDir = referenceGenomeService.referenceGenomeDirectory(workPackage.referenceGenome, false)
        linkFileUtils.createAndValidateLinks([(refGenDir): linkRefGenDir], realm)
    }
}
