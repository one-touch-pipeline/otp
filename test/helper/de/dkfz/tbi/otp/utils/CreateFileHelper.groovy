package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingStep

/**
 */
class CreateFileHelper {

    static final String MD5SUM = "a841c64c5825e986c4709ac7298e9366"

    static File createFile(File file, String content = "some content") {
        if (! file.parentFile.exists()) {
            assert file.parentFile.mkdirs()
        }
        file << content
        return file
    }

    static File createResultFile(SnvCallingInstance instance, SnvCallingStep step, String chromosome = null) {
        File inputResultFile = getResultOtpPath(instance, step, chromosome).absoluteStagingPath
        return createFile(inputResultFile, 'some dummy content')
    }

    static File createMD5SUMFile(SnvCallingInstance instance, SnvCallingStep step, String chromosome = null) {
        File inputResultFile = getResultOtpPath(instance, step, chromosome).absoluteStagingPath
        File md5sumFile = new File("${inputResultFile}.md5sum")
        md5sumFile.text = "${MD5SUM}   SnvDeepAnnotationJob"
        return md5sumFile
    }

    static OtpPath getResultOtpPath(SnvCallingInstance instance, SnvCallingStep step, String chromosome = null) {
        if (step == SnvCallingStep.CALLING) {
            return new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual, chromosome))
        } else {
            return new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual))
        }
    }
}
