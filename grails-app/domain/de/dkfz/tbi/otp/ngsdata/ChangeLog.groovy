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

import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.ReferencedClass

class ChangeLog implements Entity {

    long rowId
    /**
     * The class this ChangeLog is pointing to
     */
    ReferencedClass referencedClass
    /**
     * This field has been dropped, only listed for backward compatibility of database
     * @deprecated Use referencedClass
     */
    String tableName = ""
    String columnName
    String fromValue
    String toValue
    String comment
    Date dateCreated = new Date()

    enum Source {
        SYSTEM, MANUAL
    }
    Source source
    static constraints = {
        // blank to be able to keep existing objects in Database
        tableName(blank: true)
        // nullable for compatibility with existing objects in Database
        referencedClass(nullable: true)
        columnName()
        fromValue()
        toValue()
        comment()
        // nullable for compatibility with existing objects in Database
        dateCreated(nullable: true)
    }
}
