package de.dkfz.tbi.otp.job.processing


/**
 * This Service provides helper-methods to create short cluster scripts.
 * These methods should be written in a generic way so that it is easy to reuse them.
 *
 */
class CreateClusterScriptService {

    final static String DIRECTORY_PERMISSION = "2750"


    static String ensureFileDoesNotExistScript(final File file) {
        assert file.isAbsolute()
        return """
if [ -e "${file}" ]; then
    echo "File ${file} already exists."
    exit 1
fi
"""
    }

    static String ensureFileHasExpectedSizeScript(final File file, final long expectedSize) {
        assert file.isAbsolute()
        return """
ACTUAL_FILE_SIZE=\$(stat -Lc '%s' "$file")
if [ \$ACTUAL_FILE_SIZE -ne ${expectedSize} ]; then
    echo "File ${file} has size \$ACTUAL_FILE_SIZE bytes. Expected ${expectedSize} bytes."
    exit 1
fi
"""
    }

    /**
     * Create a string to make a directory
     * @param mode the access mode of the directory to be created. If used it must be given as a string
     */

    String makeDirs(Collection<File> dirs, String mode=null) {
        String m = mode ? "--mode ${mode}" : ""
        String umask = mode ? "umask ${extractMatchingUmaskFromMode(mode)};" : ""
        return "${umask} mkdir --parents ${m} ${dirs.join(' ')} &>/dev/null; echo \$?"
    }

    String extractMatchingUmaskFromMode(String mode) {
        return mode[-3..-1].toCharArray().collect({ 7 - Integer.parseInt(it.toString()) }).join("")
    }

    enum RemoveOption {
        RECURSIVE_FORCE("rm -rf"),
        RECURSIVE("rm -r"),
        EMPTY("rmdir"),

        private String command
        private RemoveOption(String command) {
            this.command = command
        }

        public String getCommand() {
            return command
        }
    }

    String removeDirs(Collection<File> dirs, RemoveOption option) {
        StringBuilder script = new StringBuilder()
        script.append(option.command)
        script.append(" ${dirs.join(' ')} &>/dev/null\n")
        script.append("echo \$?")
        return script.toString()
    }
}
