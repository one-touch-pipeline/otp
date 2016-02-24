package de.dkfz.tbi.otp.example

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl

class UntarJob extends AbstractJobImpl {

    @Override
    public void execute() throws Exception {
        String fileName = getParameterValueOrClass("file")
        String directory = getParameterValueOrClass("directory")
        // run the process
        def process = "tar -pxv --atime-preserve -f ${fileName}".execute(null, new File(directory))
        process.waitFor()
        if (process.exitValue()) {
            throw new RuntimeException("Untar failed")
        }
        String files = ""
        process.in.eachLine { line ->
            files += directory
            if (!directory.endsWith('/')) {
                files += '/'
            }
            files += line.replace(" ", "\\ ")
            files += " "
        }
        log.debug files
        addOutputParameter("extractedFiles", files)
    }

}
