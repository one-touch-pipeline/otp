package de.dkfz.tbi.otp.job.processing

import java.io.File
import java.util.List
import java.util.Map
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.log4j.Logger

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.example.Md5SumJob
import org.codehaus.groovy.grails.commons.*

/**
 * Helper class providing functionality for PBS related stuff
 * 
 * Provides connection to a remote host via ssh and validation of
 * pbs ids. 
 * 
 *
 */
class PbsService {
    /**
     * Dependency injection of grails application
     */
    def grailsApplication

    /**
     * Triggers the sending of pbs jobs
     *
     * The String representing the name of the script to be run on pbs
     * is optional as a file path and name can be specified in the
     * properties file. If the command is specified via method parameter
     * the pbs command has to be specified as well.
     * Several parameters necessary or optional for pbs jobs are read out
     * of the properties file like the host name of the targeted pbs.
     * 
     * @return The temporary file containing the output of the triggered pbs job
     */
    public File sendPbsJob(String cmd = null) {
        String resourceIdentifier
        String resource
        if(!cmd) {
            resource = new File((grailsApplication.config.otp.pbs.ssh.commandResource)).absolutePath
            resourceIdentifier = "commandResource"
        } else if(cmd) {
            resource = cmd
            resourceIdentifier = "command"
        } else {
            throw new ProcessingException("No resource is specified to be run on PBS.")
        }
        String host = (grailsApplication.config.otp.pbs.ssh.host).toString()
        String username = (grailsApplication.config.otp.pbs.ssh.username).toString()
        String password = (grailsApplication.config.otp.pbs.ssh.password).toString()
        if(resource.empty) {
            throw new ProcessingException("No resource is specified to be run on PBS.")
        }
        String timeout = (grailsApplication.config.otp.pbs.ssh.timeout).toString()
        return querySsh(resourceIdentifier, resource, host, username, password, timeout)
    }

    /**
     * Opens an ssh connection to a specified host with specified credentials 
     *
     * @param resourceIdentifier Identifies which resource shall be taken, string or file
     * @param resource Resource of the job
     * @param host Host to which the connection shall be opened
     * @param username User name to open the connection 
     * @param password Password of the user who opens the connection
     * @param timeout Timeout in seconds after which the connection is closed
     * @return Temporary file containing output of the connection
     */
    private File querySsh(String resourceIdentifier, String resource, String host, String username, String password, String timeout) {
        def ant = new AntBuilder()
        String identifier = resourceIdentifier.toString()
        File tempFile = File.createTempFile("pbsJobTempFile", ".tmp", new File("/tmp/"))
        ant.sshexec(host: host,
                password: password,
                username: username,
                trust: true,
                verbose: false,
                command: resource,
                //"${identifier}": resource,
                timeout: timeout,
                output: tempFile.absoluteFile
                )
        return tempFile
    }

    /**
     * Extracts pbs ids from a given file
     *
     * @param file File containing output of ssh session from pbs
     * @return List of Strings each them a pbs id
     */
    public List<String> extractPbsIds(File file) {
        Pattern pattern = Pattern.compile("\\d+")
        List<String> pbsIds = []
        file.eachLine { String line ->
            Matcher m = pattern.matcher(line)
            if (m.find()) {
                pbsIds.add(m.group())
            }
            else {
                return null
            }
        }
        if(!deleteFile(file)) {
            log.debug("File for temporaly storing PBS ids with name ${file.name} could not be deleted.")
        }
        return pbsIds
    }

    /**
     * Deletes a file and returns {@code true} if deleting was possible {@code false} otherwise
     *
     * @param file File to be deleted
     * @return Boolean value indicating if the file could be deleted
     */
    public boolean deleteFile(File file) {
        if(file.isFile()) {
            return file.delete()
        }
        return false
    }

    /**
     * Validates if jobs of which the pbs ids are are handed over are running
     *
     * @param pbsIds Pbs ids to be validated
     * @return Map of pbs ids with associated validation identifiers, which are Boolean values
     */
    private Map<String, Boolean> validate(List<String> pbsIds) {
        if(!pbsIds) {
            throw new InvalidStateException("No pbs ids handed over to be validated.")
        }
        Map<String, Boolean> stats = [:]
        for(String pbsId in pbsIds) {
            String cmd = "qstat ${pbsId}"
            File tmpStat= sendPbsJob(cmd)
            if(tmpStat.size() == 0 || !tmpStat.isFile()) {
                throw new ProcessingException("Temporary file to contain qstat could not be written properly.")
            }
            Boolean running = isRunning(tmpStat)
            stats.put(pbsId, running)
        }
        return stats
    }

    /**
     * Verifies if a job is running
     *
     * Verifies if a job of the handed over file contains
     * particular content indicating the job is running.
     * Returns {@code true} if job is running, otherwise {@code false}.
     *
     * @param file File containing output of a pbs job
     * @return Indicating if job is running
     */
    private boolean isRunning(File file) {
        Pattern pattern = Pattern.compile("\\s*Job id\\s*Name\\s*User.*")
        boolean found = false
        file.eachLine { String line ->
            Matcher m = pattern.matcher(line)
            if(m.find()) {
                found = true
            }
        }
        return found
    }
}
