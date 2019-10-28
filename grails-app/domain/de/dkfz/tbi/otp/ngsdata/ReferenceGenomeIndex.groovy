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


class ReferenceGenomeIndex implements Entity {

    ToolName toolName
    ReferenceGenome referenceGenome
    // depending on the tool, path may be a file or a directory
    String path
    String indexToolVersion

    static constraints = {
        path unique: ['toolName', 'referenceGenome'], blank: false, validator: { String val ->
            OtpPath.isValidRelativePath(val)
        }
        indexToolVersion unique: ['toolName', 'referenceGenome'], blank: false
    }

    static belongsTo = [
            referenceGenome: ReferenceGenome,
    ]

    @Override
    String toString() {
        return "${referenceGenome.name} ${toolWithVersion}"
    }

    String getToolWithVersion() {
        return "${toolName.name} ${indexToolVersion}"
    }
}
