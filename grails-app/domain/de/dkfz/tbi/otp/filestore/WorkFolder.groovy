/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.filestore

import grails.gorm.hibernate.annotation.ManagedEntity
import groovy.transform.ToString

import de.dkfz.tbi.otp.utils.Entity

/**
 * WorkFolder represents a storage identified by an UUID.
 * The implementation on a file system is a chain of subfolders under the base folder
 * based on the given UUID
 *
 * UUID is defined by RFC 4122, ISO/IEC 9834-8:2005
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc4122">RFC 4122</a>
 */
@ToString(includeNames = true, includes = ["uuid", "baseFolder", "size"])
@ManagedEntity
class WorkFolder implements Entity {

    static final String UUID_SEPARATOR = '-'

    /**
     * An UUID identifying an unique subfolder created under the base folder
     *
     * UUID is represented by 32 hexadecimal digits
     * E.g.: a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
     */
    UUID uuid

    /**
     * Size of the subfolder
     * null means unknown or cannot be calculated
     */
    @SuppressWarnings("GrailsDomainReservedSqlKeywordName")
    Long size

    /**
     * Reference to the base folder, under which work folder starts
     */
    BaseFolder baseFolder

    /**
     * Whether or not the folder is deleted on the file system
     */
    boolean deleted = false

    static Closure constraints = {
        size nullable: true
    }
}
