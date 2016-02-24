package de.dkfz.tbi.otp.example

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl

class TarJob extends AbstractJobImpl {

    @Override
    public void execute() throws Exception {
        String tarFiles = getParameterValueOrClass("files")
        String outputDir = getParameterValueOrClass("directory")

        String name = "${outputDir}/tarJob_${System.currentTimeMillis()}.tar"
        String[] files = tarFiles.split(" ")
        String path = files[0].substring(0, files[0].lastIndexOf('/') + 1)
        String command = "tar -pc --atime-preserve -f ${name} "
        files.each {
            command += it.replace(path, "")
            command += " "
        }
        def process = command.execute(null, new File(path))
        process.waitFor()
        if (process.exitValue()) {
            throw new RuntimeException("Tar failed: ${process.err.text}")
        }
        addOutputParameter("tarFile", name)
    }
}
