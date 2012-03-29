package de.dkfz.tbi.otp.job.processing

import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Helper class providing functionality for PBS related stuff
 * 
 * Provides connection to a remote host via ssh and validation of
 * pbs ids. 
 * 
 * @deprecated Is replaced by ExecutionService
 *
 */
class PbsService {
    /**
     * Dependency injection of grails application
     */
    @SuppressWarnings("GrailsStatelessService")
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
    public List<String> sendPbsJob(String command) {
        if(!command) {
            throw new ProcessingException("No resource is specified to be run on PBS.")
        }
        String host = (grailsApplication.config.otp.pbs.ssh.host).toString()
        String username = (grailsApplication.config.otp.pbs.ssh.username).toString()
        String password = (grailsApplication.config.otp.pbs.ssh.password).toString()
        String timeout = (grailsApplication.config.otp.pbs.ssh.timeout).toString()
        return querySsh(command, host, username, password, timeout)
    }

    /**
     * Opens an ssh connection to a specified host with specified credentials 
     *
     * @param command Command of the job
     * @param host Host to which the connection shall be opened
     * @param username User name to open the connection 
     * @param password Password of the user who opens the connection
     * @param timeout Timeout in seconds after which the connection is closed
     * @return Temporary file containing output of the connection
     */
    private List<String> querySsh(String command, String host, String username, String password, String timeout) {
        JSch jsch = new JSch()
        Session session = jsch.getSession(username, host, 22)
        session.setPassword(password)
        session.setTimeout(timeout.toInteger())
        java.util.Properties config = new java.util.Properties()
        config.put("StrictHostKeyChecking", "no")
        session.setConfig(config)
        session.connect()
        Channel channel = session.openChannel("exec")
        ((ChannelExec)channel).setCommand(command)
        channel.setInputStream(null)
        ((ChannelExec)channel).setErrStream(System.err)
        InputStream inputStream = channel.getInputStream()
        channel.connect()

        byte[] tmp = new byte[1024]
        List<String> values = []
        while (true){
            while (inputStream.available() > 0) {
                int i = inputStream.read(tmp, 0, 1024)
                if (i < 0) {
                    break
                }
                values.add(new String(tmp, 0, i))
            }
            if (channel.isClosed()) {
                break
            }
            try {
                Thread.sleep(1000)
            } catch(Exception ee){
            }
        }
        channel.disconnect()
        session.disconnect()

        return values
    }

    /**
     * Extracts pbs ids from a given String
     *
     * @param sshOut List of Strings containing output of ssh session from pbs
     * @return List of Strings each of them a pbs id
     */
    public List<String> extractPbsIds(String sshOutput) {
        Pattern pattern = Pattern.compile("\\d+")
        List<String> pbsIds = []
        sshOutput.eachLine { String line ->
            Matcher m = pattern.matcher(line)
            if (m.find()) {
                pbsIds.add(m.group())
            }
            else {
                return null
            }
        }
        return pbsIds
    }

    /**
     * Validates if jobs of which the pbs ids are are handed over are running
     *
     * @param pbsIds Pbs ids to be validated
     * @return Map of pbs ids with associated validation identifiers, which are Boolean values
     */
    public Map<String, Boolean> validate(List<String> pbsIds) {
        if(!pbsIds) {
            throw new InvalidStateException("No pbs ids handed over to be validated.")
        }
        Map<String, Boolean> stats = [:]
        for(String pbsId in pbsIds) {
            String cmd = "qstat ${pbsId}"
            List<String> tmpStat = sendPbsJob(cmd)
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
     * @param output List of Strings containing output of a pbs job
     * @return Indicating if job is running
     */
    private boolean isRunning(List<String> output) {
        Pattern pattern = Pattern.compile("\\s*Job id\\s*Name\\s*User.*")
        boolean found = false
        output.each { String line ->
            Matcher m = pattern.matcher(line)
            if(m.find()) {
                found = true
            }
        }
        return found
    }
}
