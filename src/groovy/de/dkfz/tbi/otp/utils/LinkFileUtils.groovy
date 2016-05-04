package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import java.nio.file.*

import static de.dkfz.tbi.otp.utils.WaitingFileUtils.*


@Component
class LinkFileUtils {

    @Autowired
    ExecutionService executionService

    @Autowired
    CreateClusterScriptService createClusterScriptService

    @Autowired
    LsdfFilesService lsdfFilesService

    /**
     * Creates relative symbolic links.
     * Links which already exist are overwritten, parent directories are created automatically if necessary.
     * @param sourceLinkMap The values of the map are used as link names, the keys as the link targets.
     */
    public void createAndValidateLinks(Map<File, File> sourceLinkMap, Realm realm) {
        assert sourceLinkMap
        assert realm

        if (!sourceLinkMap.isEmpty()) {
            //delete old links if exist
            lsdfFilesService.deleteFilesRecursive(realm, sourceLinkMap.values())

            //create command to create base directories of links
            StringBuilder command = new StringBuilder()
            command << createClusterScriptService.makeDirs(sourceLinkMap.values()*.parentFile, CreateClusterScriptService.DIRECTORY_PERMISSION)
            command << "\n"

            //create command to create links
            sourceLinkMap.each { File source, File link ->
                Path sourceParentPath = Paths.get(source.parent)
                Path linkParentPath = Paths.get(link.parent)
                Path relativePath = linkParentPath.relativize(sourceParentPath)
                command << "ln -sf ${relativePath.toString() ?: "."}/${source.name} ${link.path}\n"
            }

            executionService.executeCommand(realm, command.toString())

            sourceLinkMap.each { File source, File link ->
                waitUntilExists(link)
            }
        }
    }
}
