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

import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.StringUtils

@Transactional
class MetaDataService {

    /**
     * Retries a MetaDataEntry in permission aware manner.
     * @param id The id of the MetaDataEntry to retrieve
     * @return The MetaDataEntry if present, otherwise null
     */
    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or ((returnObject.dataFile.project != null) and hasPermission(returnObject.dataFile.project, 'OTP_READ_ACCESS'))")
    MetaDataEntry getMetaDataEntryById(Long id) {
        return MetaDataEntry.get(id)
    }

    /**
     * Retrieves the DataFile identified by the given ID in permission aware manner.
     * @param id The Id of the DataFile.
     * @return DataFile if it exists, otherwise null
     */
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
            throw new IllegalFileNameException("${mate1FileName} and ${mate2FileName} are not consistent as paired sequence file names.")
        }
    }

    /**
     * For paired/mate-paired files, OTP passes the files to roddy in a list so that pairs are grouped together consecutively.
     * However roddy reorders the file list which may cause wrong pairs, depending on the file names. This method checks whether that would happen.
     */
    static void ensurePairedSequenceFileNameOrder(List<File> vbpDataFiles) {
        List<File> sortedFiles = vbpDataFiles.iterator().sort { o1, o2 ->
            return o1.absolutePath <=> o2.absolutePath
        }.toList()

        final int mateCount = 2
        if (!CollectionUtils.containSame(vbpDataFiles.collate(mateCount), sortedFiles.collate(mateCount))) {
            throw new IllegalFileNameException("The file names would cause Roddy to reorder the files in the wrong way:\n" +
                    "${vbpDataFiles.join('\n')}\n----------\n${sortedFiles.join('\\n')}")
        }
    }
}
