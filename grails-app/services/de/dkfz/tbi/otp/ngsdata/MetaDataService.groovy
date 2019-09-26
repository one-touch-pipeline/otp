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
import org.springframework.security.access.prepost.PostAuthorize

import de.dkfz.tbi.otp.utils.StringUtils

@Transactional
class MetaDataService {

    /**
     * Retries a MetaDataEntry in an ACL aware manner.
     * @param id The id of the MetaDataEntry to retrieve
     * @return The MetaDataEntry if present, otherwise null
     */
    @SuppressWarnings('LineLength')
    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or ((returnObject.dataFile.project != null) and hasPermission(returnObject.dataFile.project, 'OTP_READ_ACCESS'))")
    MetaDataEntry getMetaDataEntryById(Long id) {
        return MetaDataEntry.get(id)
    }

    /**
     * Retrieves the DataFile identified by the given ID in an ACL aware manner.
     * @param id The Id of the DataFile.
     * @return DataFile if it exists, otherwise null
     */
    @SuppressWarnings('LineLength')
    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or ((returnObject.project != null) and hasPermission(returnObject.project, 'OTP_READ_ACCESS'))")
    DataFile getDataFile(Long id) {
        return DataFile.get(id)
    }

    /**
     * Ensures that the two file names are equal except for one character, and that this character is '1' for the first
     * file name and '2' for the second file name.
     */
    static void ensurePairedSequenceFileNameConsistency(final String mate1FileName, final String mate2FileName) {
        Map<String, Character> differentCharacters = StringUtils.extractDistinguishingCharacter([mate1FileName, mate2FileName])
        if (!differentCharacters ||
                differentCharacters.get(mate1FileName) != '1' ||
                differentCharacters.get(mate2FileName) != '2') {
            throw new RuntimeException("${mate1FileName} and ${mate2FileName} are not consistent as paired sequence file names.")
        }
    }
}
