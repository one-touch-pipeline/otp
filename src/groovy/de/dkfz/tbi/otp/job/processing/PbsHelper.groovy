package de.dkfz.tbi.otp.job.processing

import java.util.List
import org.apache.log4j.Logger

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.example.Md5SumJob

class PbsHelper {
    @Autowired
    GrailsApplication grailsApplication

    Logger log = new Logger(this)

    public File sshConnect(File resource = null, String cmd = null) {
        if(!resource && !cmd) {
            throw new ProcessingException("No resource is specified to be run on PBS.")
        }
        String resourceIdentifier
        if(!resource && !cmd) {
            resource = new File(grailsApplication.config.otp.pbs.commandResource)
            resourceIdentifier = "commandResource"
        } else if(cmd) {
            resource = cmd
            resourceIdentifier = "command"
        }
        String host = grailsApplication.config.otp.pbs.host
        String username = grailsApplication.config.otp.pbs.username
        String password = grailsApplication.config.otp.pbs.password
        if(!resource.isFile || resource.size() == 0) {
            throw new ProcessingException("No resource is specified to be run on PBS.")
        }
        String timeout = grailsApplication.config.otp.pbs.timeout
        String fileNameWithPath = "/tmp/${new Date() new Random().nextInt()}".encodeAsMD5()
        return querySsh(resourceIdentifier, resource, host, username, timeout, fileNameWithPath)
    }

    private File querySsh(String resourceIdentifier, String resource, String host, String username, String password, String timeout, String fileNameWithPath) {
        def ant = new AntBuilder()
        ant.sshexec(host: host,
                password: password,
                username: username,
                trust: false,
                verbose: true,
                "${resourceIdentifier}": resource,
                timeout: timeout,
                output: fileNameWithPath
                )
        return fileNameWithPath
    }

    public List<String> extractPbsIds(File file) throws InvalidStateException {
        def pattern = grailsApplication.config.otp.pbs.pattern.pbsId
        List<String> pbsIds
        file.eachLine { String line ->
            if (pattern.matcher(line)) {
                pbsIds.add(line)
            }
        }
        if(!deleteFile(file)) {
            log.debug("File for temporaly storing PBS ids with name ${file.name} could not be deleted.")
        }
        return pbsIds
    }

    public boolean deleteFile(File file) {
        if(file.isFile) {
            return file.delete()
        }
        return false
    }
}
