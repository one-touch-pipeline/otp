package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 */
class MoveFileUtilsService {

    ExecutionService executionService

    void moveFileIfExists(Realm realm, File source, File target) {
        assert realm : "Input realm must not be null"
        assert source : "Input source must not be null"
        assert target : "Input target must not be null"

        if (WaitingFileUtils.confirmExists(source)) {
            executionService.executeCommand(realm, "mkdir -m 2750 -p ${target.parent}; mv -f ${source} ${target}")
        }
        assert WaitingFileUtils.confirmExists(target)
        assert WaitingFileUtils.confirmDoesNotExist(source)
    }


    void moveDirContentIfExists(Realm realm, File sourceDir, File targetDir) {
        assert realm : "Input realm must not be null"
        assert sourceDir : "Input sourceDir must not be null"
        assert targetDir : "Input targetDir must not be null"

        assert WaitingFileUtils.confirmExists(sourceDir) : "The source directory ${sourceDir} does not exist."
        Set<String> sourceDirContent = sourceDir.list() as Set
        if (sourceDirContent.size() > 0) {
            executionService.executeCommand(realm, "mkdir -m 2750 -p ${targetDir}; mv -f ${sourceDir}/* ${targetDir}")
        }

        assert ThreadUtils.waitFor({ (targetDir.list() as Set).containsAll(sourceDirContent) }, WaitingFileUtils.defaultTimeoutMillis, 50)
        assert ThreadUtils.waitFor({ sourceDir.list().size() == 0 }, WaitingFileUtils.defaultTimeoutMillis, 50)
    }
}
