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
package de.dkfz.tbi.otp.utils

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService

import java.nio.file.Files
import java.nio.file.Path

@Transactional
class Md5SumService {

    FileService fileService
    ConfigService configService

    String extractMd5Sum(Path md5Sum) {
        assert md5Sum: "Parameter md5Sum is null"
        assert md5Sum.isAbsolute() : "The md5sum file '${md5Sum}' is not absolute"
        assert Files.exists(md5Sum): "The md5sum file '${md5Sum}' does not exist"
        assert Files.isRegularFile(md5Sum): "The md5sum file '${md5Sum}' is not a file"
        assert fileService.fileIsReadable(md5Sum, configService.defaultRealm): "The md5sum file '${md5Sum}' is not readable"
        assert md5Sum.text: "The md5sum file '${md5Sum}' is empty"

        String md5sum = md5Sum.text.replaceAll("\n", "").toLowerCase(Locale.ENGLISH)
        assert md5sum ==~ /^[0-9a-f]{32}$/ : "The md5sum file '${md5Sum}' has not the correct form (/^[0-9a-f]{32}\$/)"
        return md5sum
    }
}
