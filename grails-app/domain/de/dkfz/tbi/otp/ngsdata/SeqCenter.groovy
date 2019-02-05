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

package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.Entity

class SeqCenter implements Entity {

    String name

    /** where fastqc files are written to */
    String dirName

    /** where fastq files are imported from */
    String autoImportDir
    boolean autoImportable = false

    /** files in this directories may be linked */
    static hasMany = [
            importDirsAllowLinking: String,
    ]

    /** should meta data files be copied to the seq center inbox */
    boolean copyMetadataFile = false

    static constraints = {
        name(blank: false, unique: true)
        dirName(blank: false, unique: true, validator: { OtpPath.isValidPathComponent(it) })
        autoImportDir nullable: true, blank: false, unique: true,  validator: { val, obj ->
            if (obj.autoImportable && !val) {
                return "auto import dir must be set if auto import enabled"
            }
            if (val != null && !OtpPath.isValidAbsolutePath(val)) {
                return "'${val}' is not a valid absolute path"
            }
        }
        importDirsAllowLinking nullable: true, validator: { val ->
            if (val != null) {
                Set<String> invalidPaths = val.findAll {
                    !OtpPath.isValidAbsolutePath(it)
                }
                if (invalidPaths) {
                    return "'${invalidPaths.join(", ")}' not valid absolute path(s)"
                }
            }
        }
    }

    @Override
    String toString() {
        name
    }
}
