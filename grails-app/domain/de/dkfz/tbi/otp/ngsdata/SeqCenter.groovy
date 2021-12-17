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

import de.dkfz.tbi.otp.project.ProjectFieldReferenceAble
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator

/** This table is used externally. Please discuss a change in the team */
class SeqCenter implements Entity, ProjectFieldReferenceAble {

    /** This attribute is used externally. Please discuss a change in the team */
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
        dirName(blank: false, unique: true, shared: "pathComponent")
        autoImportDir nullable: true, blank: false, unique: true, validator: { val, obj ->
            if (obj.autoImportable && !val) {
                return "required"
            }
            if (val != null && !OtpPathValidator.isValidAbsolutePath(val)) {
                return "validator.absolute.path"
            }
        }
        importDirsAllowLinking nullable: true, validator: { val ->
            if (val != null) {
                Set<String> invalidPaths = val.findAll {
                    !OtpPathValidator.isValidAbsolutePath(it)
                }
                if (invalidPaths) {
                    return ["absolute.path", invalidPaths.join(", ")]
                }
            }
        }
    }

    @Override
    String toString() {
        name
    }

    @Override
    String getStringForProjectFieldDomainReference() {
        return name
    }
}
