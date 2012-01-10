package de.dkfz.tbi.otp.example

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter

class UnzipJob extends AbstractJobImpl {

    @Override
    public void execute() throws Exception {
        String zipFile = getParameterValueOrClass("zipFile")
        String outputDir = getParameterValueOrClass("directory")

        String name = "${outputDir}/unzipJob_${System.currentTimeMillis()}.tar"
        String command = "gunzip -c ${zipFile}"
        println command
        def process = command.execute()
        process.waitFor()
        if (process.exitValue()) {
            throw new RuntimeException("Unzip failed: ${process.err.text}")
        }
        File outputFile = new File(name)
        outputFile.append(process.in)
        addOutputParameter("unzipFile", name)
    }
}
