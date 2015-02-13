package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.Realm
import static de.dkfz.tbi.otp.utils.WaitingFileUtils.confirmExists
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

/**
 */
@Component
class LinkFileUtils {

    @Autowired
    ExecutionService executionService

    /**
     * Creates links for the key in the sourceLinkMap to the value in the sourceLinkMap.
     * Links which exist already are overwritten.
     */
    public void createAndValidateLinks(Map<File, File> sourceLinkMap, Realm realm) {
        assert sourceLinkMap
        assert realm

        if (!sourceLinkMap.isEmpty()) {
            StringBuilder command = new StringBuilder()
            sourceLinkMap.each { File source, File link ->
                command << "ln -sf ${source.path} ${link.path}\n"
            }

            executionService.executeCommand(realm, command.toString())

            sourceLinkMap.each { File source, File link ->
                if (! confirmExists(link)) {
                    throw new IOException("couldn't create link ${source} --> ${link}")
                }
            }
        }
    }
}
