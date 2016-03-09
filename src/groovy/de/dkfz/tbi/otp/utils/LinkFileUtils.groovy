package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm
import static de.dkfz.tbi.otp.utils.WaitingFileUtils.*
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component
class LinkFileUtils {

    @Autowired
    ExecutionService executionService

    @Autowired
    CreateClusterScriptService createClusterScriptService

    @Autowired
    LsdfFilesService lsdfFilesService

    /**
     * Creates symbolic links.
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
                command << "ln -sf ${source.path} ${link.path}\n"
            }

            executionService.executeCommand(realm, command.toString())

            sourceLinkMap.each { File source, File link ->
                waitUntilExists(link)
            }
        }
    }
}
