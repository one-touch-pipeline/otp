package de.dkfz.tbi.otp.filehandling

import java.util.regex.Matcher

class BwaLogFileParser {

    static long parseReadNumberFromLog(File file) {
        assert file.canRead(): "Log file \"${file.absoluteFile}\" is not readable"

        long readNumber = -1
        file.eachLine() {
            if (it.startsWith("[cnybwa_aln_core]")) {
                Matcher matcher = it =~ /\[cnybwa_aln_core\]\s+(\d+) sequences processed.../
                readNumber = matcher.find() ? Long.parseLong(matcher.group(1)) : readNumber
            }
        }
        if (readNumber == -1) {
            throw new RuntimeException("Unable to find a read number in file \"${file.absoluteFile}\"")
        }
        return readNumber
    }
}
