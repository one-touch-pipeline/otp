package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.security.access.prepost.PreAuthorize

class AdapterFileService {

    static final String BASE_PATH_PROCESSING_OPTION_NAME = 'ADAPTER_BASE_PATH'

    ProcessingOptionService processingOptionService

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public AdapterFile createAdapterFile(String fileName){
        assert fileName : "the input file name '${fileName}' must not be null"
        assert !AdapterFile.findByFileNameIlike(fileName) : "The adapter file '${fileName}' is linked already"
        AdapterFile adapterFile = new AdapterFile(
                fileName: fileName
        )
        assert adapterFile.save(flush: true, failOnError: true)
        return adapterFile
    }

    AdapterFile findByFileName(String fileName) {
        assert fileName
        return CollectionUtils.atMostOneElement(AdapterFile.findAllByFileName(fileName))
    }


    File baseDirectory() {
        String path = processingOptionService.getValueOfProcessingOption(BASE_PATH_PROCESSING_OPTION_NAME)
        assert OtpPath.isValidAbsolutePath(path)
        return new File(path)
    }


    File fullPath(AdapterFile adapterFile, boolean checkReadability = true) {
        assert adapterFile
        File file = new File(baseDirectory(), adapterFile.fileName)
        if (checkReadability) {
            assert file.canRead()
        }
        return file
    }
}
