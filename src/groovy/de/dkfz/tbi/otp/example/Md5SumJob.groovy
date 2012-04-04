package de.dkfz.tbi.otp.example

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.RestartableJob

@RestartableJob
class Md5SumJob extends AbstractJobImpl {

    @Override
    public void execute() throws Exception {
        String file = getParameterValueOrClass("file")
        def process = "md5sum ${file}".execute()
        process.waitFor()
        if (process.exitValue()) {
            throw new RuntimeException("Calculating MD5Sum failed: ${process.err.text}")
        }
        String md5sum = process.in.text.split(' ')[0]
        addOutputParameter("md5sum", md5sum)
        println "md5sum calculated: ${md5sum}"
    }
}
