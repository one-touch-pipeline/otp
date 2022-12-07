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

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.utils.CollectionUtils

@Transactional
class SeqCenterService {

    List<SeqCenter> getDisplayableMetadata() {
        return SeqCenter.list(sort: "name", order: "asc")
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateAutoImportDirectory(SeqCenter seqCenter, String autoImportDirectory) {
        seqCenter.autoImportDir = autoImportDirectory
        seqCenter.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateAutoImportable(SeqCenter seqCenter, boolean autoImportable) {
        seqCenter.autoImportable = autoImportable
        seqCenter.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateImportDirsAllowLinking(SeqCenter seqCenter, String importDirOld, String importDirNew) {
        seqCenter.removeFromImportDirsAllowLinking(importDirOld)
        if (importDirNew) {
            seqCenter.addToImportDirsAllowLinking(importDirNew)
        }
        seqCenter.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void createImportDirsAllowLinking(SeqCenter seqCenter, String importDir) {
        seqCenter.addToImportDirsAllowLinking(importDir)
        seqCenter.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SeqCenter createSeqCenter(String name, String dirName) {
        assert name : "the input name '${name}' must not be null"
        assert dirName : "the input dirname '${dirName}' must not be null"
        assert !CollectionUtils.atMostOneElement(SeqCenter.findAllByName(name)) : "The SeqCenter '${name}' exists already"
        assert !CollectionUtils.atMostOneElement(SeqCenter.findAllByDirName(dirName)) : "The SeqCenter dirname '${dirName}' exists already"
        SeqCenter seqCenter = new SeqCenter(
            name: name,
            dirName: dirName
        )
        assert seqCenter.save(flush: true)
        return seqCenter
    }
}
