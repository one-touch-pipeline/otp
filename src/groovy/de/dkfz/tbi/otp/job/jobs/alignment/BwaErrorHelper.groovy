package de.dkfz.tbi.otp.job.jobs.alignment

/**
 * Class used to circunvent the lack of trustable error codes
 * from BWA implementations (conveybwa and bwa binary)
 *
 */
abstract class BwaErrorHelper {

    /**
     * Retrieves a bash script to validate the outputted file (also using generated error log file)
     *
     * Tests for:
     * - Outputted file size bigger then 200 bytes
     * - Presence of " fault" string in the error log file
     * - Presence of "error" string in the error log file
     * @param outputFile Outputted file
     * @param logFile Log file
     * @returns The bash script to test for errors
     */
    public static String failureCheckScript(String outputFile, String logFile) {
        return """
if [[ "200" -gt `stat -c %s ${outputFile}` ]]
then
    echo Output file is too small! exiting;
    rm ${outputFile}
    exit 33
fi

if [[ "\$?" != "0" ]]
then
    echo There was a non-zero exit code! exiting;
    rm ${outputFile}
    exit 32
fi

success=`grep " fault" ${logFile}`
if [ ! -z "\$success" ]
then
    echo Found segfault \$success in logfile! exiting;
    rm ${outputFile}
    exit 31
fi

success=`grep -i "error" ${logFile}`
if [ ! -z "\$success" ]
then
    echo Found error \$success in logfile! exiting;
    rm ${outputFile}
    exit 36
else
    echo all OK
fi"""
    }
}
