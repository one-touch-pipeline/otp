package de.dkfz.tbi.otp.example

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("unzipJob")
@Scope("prototype")
class UnzipJob extends AbstractJobImpl {

    @Override
    public void execute() throws Exception {
        Parameter zipFile = processingStep.input.find { it.type.name == "zipFile" }
        Parameter outputDir = processingStep.input.find { it.type.name == "directory" }

        String name = "${outputDir.value}/unzipJob_${System.currentTimeMillis()}.tar"
        String command = "gunzip -c ${zipFile.value}"
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
