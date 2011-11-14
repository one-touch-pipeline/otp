package de.dkfz.tbi.otp.example

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("md5SumJob")
@Scope("prototype")
class Md5SumJob extends AbstractJobImpl {

    @Override
    public void execute() throws Exception {
        Parameter file = processingStep.input.find { it.type.name == "file" }
        if (!file) {
            throw new RuntimeException("Required parameter not found")
        }
        def process = "md5sum ${file.value}".execute()
        process.waitFor()
        if (process.exitValue()) {
            throw new RuntimeException("Calculating MD5Sum failed: ${process.err.text}")
        }
        String md5sum = process.in.text.split(' ')[0]
        addOutputParameter("md5sum", md5sum)
        println "md5sum calculated: ${md5sum}"
    }

}
