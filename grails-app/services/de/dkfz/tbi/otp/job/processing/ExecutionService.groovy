package de.dkfz.tbi.otp.job.processing

import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @short Helper class providing functionality for remote execution of jobs.
 * 
 * Provides connection to a remote host via ssh and validation of
 * pbs ids. Specifically the remote hosts are PBS' meaning that a
 * format specific for usage on a PBS is built and executed. 
 * 
 */
class ExecutionService {

    /**
     * Dependency injection of grailsApplication
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication

    /**
     * Triggers the sending of remote jobs
     *
     * Commands are executed on remote servers using a pbs infrastructure.
     * The host can be specified and so the service is flexible for
     * executing on several different servers. Either command or script
     * have to be specified otherwise it does not work and an exception
     * is thrown.
     *
     * @param host The host on which the command shall be executed
     * @param port The port to be addressed on the server
     * @param timeout The timeout in milliseconds after which execution interrupts
     * @param command The command to be executed on the remote server
     * @param script The script to be executed on the remote server
     * @param options The options To make the command more specific
     * 
     * @return List of Strings containing the output of the triggered remote job
     */
    public List<String> executeRemoteJob(String host, int port, int timeout, String command = null, File script = null, String options = null) {
        if (!command && !script) {
            throw new ProcessingException("Neither a command nor a script specified to be run remotely.")
        }
        if (command) {
            // TODO: Script and command storage
        }
        return querySsh(host, port, timeout, command, script)
    }

    /**
     * Queries ssh on a pbs infrastructure
     * 
     * Opens an ssh connection to a specified host with specific credentials.
     * With the parameter options can options for the command be specified.
     *
     * @param host The host on which the command shall be executed
     * @param port The port to be addressed on the server
     * @param timeout The timeout in milliseconds after which execution interrupts
     * @param command The command to be executed on the remote server
     * @param script The script to be executed on the remote server
     * @param options The options To make the command more specific
     * @return List of Strings containing the output of the executed job
     */
    private List<String> querySsh(String host, int port, int timeout, String command = null, File script = null, String options = null) {
        String username = (grailsApplication.config.otp.pbs.ssh.username).toString()
        String password = (grailsApplication.config.otp.pbs.ssh.password).toString()
        JSch jsch = new JSch()
        Session session = jsch.getSession(username, host, port)
        if (!password) {
            throw new ProcessingException("No password for remote connection specified.")
        }
        session.setPassword(password)
        session.setTimeout(timeout)
        java.util.Properties config = new java.util.Properties()
        config.put("StrictHostKeyChecking", "no")
        session.setConfig(config)
        session.connect()
        Channel channel = session.openChannel("exec")
        if (command) {
            ((ChannelExec)channel).setCommand(command)
        } else if (script) {
            command = "qsub"
            if (options) {
                command += " ${options}"
            }
            ((ChannelExec)channel).setCommand(command)
            ((ChannelExec)channel).setInputStream(script.newInputStream())
        }
        ((ChannelExec)channel).setErrStream(System.err)
        List<String> values = getInputStream(channel)
        if (!values) {
            // TODO: How to handle this?
            throw new ProcessingException("test!")
        }
        disconnectSsh(channel)
        return values
    }

    /**
     * Disconnects channel and session to have a clear disconnect
     * from the remote host
     *
     * @param channel The channel to be disconnected
     */
    private void disconnectSsh(Channel channel) {
        channel.session.disconnect()
        channel.disconnect()
    }

    /**
     * Retrives the input stream and converts it to a List of Strings
     *
     * @param channel The channel to read the input stream from
     * @return List of Strings containing the returned stream
     */
    private List<String> getInputStream(Channel channel) {
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
        return values
    }
    /**
     * Extracts pbs ids from a given String
     *
     * @param sshOutput List of Strings containing output of ssh session from pbs
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
