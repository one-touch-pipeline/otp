package de.dkfz.tbi.otp.utils

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm

import java.nio.file.Path
import java.nio.file.Paths

import static de.dkfz.tbi.otp.utils.WaitingFileUtils.waitUntilExists


@Component
class LinkFileUtils {

    @Autowired
    RemoteShellHelper remoteShellHelper

    @Autowired
    CreateClusterScriptService createClusterScriptService

    @Autowired
    LsdfFilesService lsdfFilesService

    /**
     * Creates relative symbolic links.
     * Links which already exist are overwritten, parent directories are created automatically if necessary.
     * @param sourceLinkMap The values of the map are used as link names, the keys as the link targets.
     */
    void createAndValidateLinks(Map<File, File> sourceLinkMap, Realm realm) {
        assert sourceLinkMap
        assert realm

        if (!sourceLinkMap.isEmpty()) {
            //delete old links if exist
            lsdfFilesService.deleteFilesRecursive(realm, sourceLinkMap.values())

            //create command to create base directories of links
            StringBuilder command = new StringBuilder()
            command << createClusterScriptService.makeDirs(sourceLinkMap.values()*.parentFile.unique(), CreateClusterScriptService.DIRECTORY_PERMISSION)
            command << "\n"

            //create command to create links
            sourceLinkMap.each { File source, File link ->
                Path sourceParentPath = Paths.get(source.parent)
                Path linkParentPath = Paths.get(link.parent)
                Path relativePath = linkParentPath.relativize(sourceParentPath)
                command << "ln -sf ${relativePath.toString() ?: "."}/${source.name} ${link.path}\n"
            }

            remoteShellHelper.executeCommand(realm, command.toString())

            sourceLinkMap.each { File source, File link ->
                waitUntilExists(link)
            }
        }
    }
}
