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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.Entity

/**
 * An import of one or more {@linkplain MetaDataFile}s and {@linkplain DataFile}s.
 */
@ManagedEntity
class FastqImportInstance implements Entity {

    Set<DataFile> dataFiles
    static hasMany = [
            dataFiles: DataFile,
    ]

    OtrsTicket otrsTicket

    enum ImportMode {
        MANUAL,
        AUTOMATIC
    }
    ImportMode importMode

    static constraints = {
        //the field can be null, since for the old data the information is not needed; only for new incoming fastqImportInstances
        otrsTicket(nullable: true)
    }

    static mapping = {
        otrsTicket index: "fastq_import_instance_otrs_ticket_idx"
    }
}
