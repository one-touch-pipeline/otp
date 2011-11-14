package de.dkfz.tbi.otp.example

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("untarJob")
@Scope("prototype")
class UntarJob extends AbstractJobImpl {

    @Override
    public void execute() throws Exception {
        // TODO: convenient method
        Parameter fileName = processingStep.input.find { it.type.name == "file" }
        Parameter directory = processingStep.input.find { it.type.name == "directory" }
        if (!fileName || !directory) {
            throw new RuntimeException("Required parameter not found")
        }
        // run the process
        def process = "tar -pxv --atime-preserve -f ${fileName.value}".execute(null, new File(directory.value))
        process.waitFor()
        if (process.exitValue()) {
            throw new RuntimeException("Untar failed")
        }
        String files = ""
        process.in.eachLine { line ->
            files += directory.value
            if (!directory.value.endsWith('/')) {
                files += '/'
            }
            files += line.replace(" ", "\\ ")
            files += " "
        }
        println files
        addOutputParameter("extractedFiles", files)
    }

}
