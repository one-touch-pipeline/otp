/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.job.processing

import grails.gorm.transactions.Transactional

/**
 * This Service provides helper-methods to create short cluster scripts.
 * These methods should be written in a generic way so that it is easy to reuse them.
 */
@Transactional
class CreateClusterScriptService {

    final static String DIRECTORY_PERMISSION = "2750"

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

        String getCommand() {
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
