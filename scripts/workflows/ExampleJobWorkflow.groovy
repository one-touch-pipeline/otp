import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("example") {
    start("example", "exampleStartJob") {
        constantParameter("directory", "/tmp/otp/start")
        outputParameter("file")
    }
    job("unzip", "unzipJob") {
        constantParameter("directory", "/tmp/otp/unzip")
        outputParameter("unzipFile")
        inputParameter("zipFile", "example", "file")
    }
    job("untar", "untarJob") {
        constantParameter("directory", "/tmp/otp/untar")
        outputParameter("extractedFiles")
        inputParameter("file", "unzip", "unzipFile")
    }
    job("tar", "tarJob") {
        constantParameter("directory", "/tmp/otp/tar")
        outputParameter("tarFile")
        inputParameter("files", "untar", "extractedFiles")
    }
    job("origMd5Sum", "md5SumJob") {
        outputParameter("md5sum")
        inputParameter("file", "unzip", "unzipFile")
    }
    job("generatedMd5Sum", "md5SumJob") {
        outputParameter("md5sum")
        inputParameter("file", "tar", "tarFile")
    }
    job("compare", "compareJob") {
        inputParameter("value1", "origMd5Sum", "md5sum")
        inputParameter("value2", "generatedMd5Sum", "md5sum")
    }
}
