package de.dkfz.tbi.otp.utils

import java.nio.file.Files
import java.nio.file.Path

class Md5SumService {

    String extractMd5Sum(Path md5Sum) {
        assert md5Sum: "Parameter md5Sum is null"
        assert md5Sum.isAbsolute() : "The md5sum file '${md5Sum}' is not absolute"
        assert Files.exists(md5Sum): "The md5sum file '${md5Sum}' does not exist"
        assert Files.isRegularFile(md5Sum): "The md5sum file '${md5Sum}' is not a file"
        assert Files.isReadable(md5Sum): "The md5sum file '${md5Sum}' is not readable"
        assert md5Sum.text: "The md5sum file '${md5Sum}' is empty"

        String md5sum = md5Sum.text.replaceAll("\n", "").toLowerCase(Locale.ENGLISH)
        assert md5sum ==~ /^[0-9a-f]{32}$/ : "The md5sum file '${md5Sum}' has not the correct form (/^[0-9a-f]{32}\$/)"
        return md5sum
    }
}
