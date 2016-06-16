package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*

class AdapterFileService {

    static final String BASE_PATH_PROCESSING_OPTION_NAME = 'ADAPTER_BASE_PATH'

    ProcessingOptionService processingOptionService


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
