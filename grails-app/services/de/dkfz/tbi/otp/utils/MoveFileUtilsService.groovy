package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 */
@Deprecated
//TODO: OTP-1734 delete class
class MoveFileUtilsService {

    ExecutionService executionService

    void moveFileIfExists(Realm realm, File source, File target, boolean readableForAll = false) {
        assert realm : "Input realm must not be null"
        assert source : "Input source must not be null"
        assert target : "Input target must not be null"

        String permissionMask = readableForAll ? "644" : "640"

        moveFileIfExists(realm, source, target, permissionMask)
    }

    /**
     * Move the file from source to target and adapt the permission mask if defined. An empty or null permission mask disable changing of mask.
     * If source does not exist, no movement and no mask change are done.
     */
    void moveFileIfExists(Realm realm, File source, File target, String permissionMask) {
        assert realm : "Input realm must not be null"
        assert source : "Input source must not be null"
        assert target : "Input target must not be null"

        String changePermission = permissionMask ? "; chmod ${permissionMask} ${target}" : ""

        if (WaitingFileUtils.waitUntilExists(source)) {
            executionService.executeCommand(realm, "umask 027; mkdir -m 2750 -p ${target.parent}; mv -f ${source} ${target}${changePermission}")
        }
        assert WaitingFileUtils.waitUntilExists(target)
        assert WaitingFileUtils.waitUntilDoesNotExist(source)
    }


    void moveDirContentIfExists(Realm realm, File sourceDir, File targetDir) {
        assert realm : "Input realm must not be null"
        assert sourceDir : "Input sourceDir must not be null"
        assert targetDir : "Input targetDir must not be null"

        assert WaitingFileUtils.waitUntilExists(sourceDir) : "The source directory ${sourceDir} does not exist."
        Set<String> sourceDirContent = sourceDir.list() as Set
        if (sourceDirContent.size() > 0) {
            executionService.executeCommand(realm, "umask 027; mkdir -m 2750 -p ${targetDir}; mv -f ${sourceDir}/* ${targetDir}")
        }

        assert WaitingFileUtils.waitUntilExists(targetDir)

        assert ThreadUtils.waitFor({ (targetDir.list() as Set).containsAll(sourceDirContent) }, WaitingFileUtils.defaultTimeout.millis, 50)
        assert ThreadUtils.waitFor({ sourceDir.list().size() == 0 }, WaitingFileUtils.defaultTimeout.millis, 50)
    }
}
