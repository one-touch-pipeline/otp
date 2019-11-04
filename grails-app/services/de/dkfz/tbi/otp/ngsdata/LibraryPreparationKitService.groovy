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

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.utils.CollectionUtils

class LibraryPreparationKitService extends MetadataFieldsService<LibraryPreparationKit> {

    List<Map> getDisplayableMetadata() {
                return LibraryPreparationKit.list(sort: "name", order: "asc").collect { LibraryPreparationKit it ->
            [
                    id                              : it.id,
                    name                            : it.name,
                    shortDisplayName                : it.shortDisplayName,
                    adapterFile                     : it.adapterFile,
                    reverseComplementAdapterSequence: it.reverseComplementAdapterSequence,
                    importAliases                   : it.importAlias?.sort()?.join(';\n'),
                    referenceGenomesWithBedFiles    : BedFile.findAllByLibraryPreparationKit(
                            it, [sort: "referenceGenome.name", order: "asc"])*.referenceGenome*.name.join(';\n'),
            ]
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    LibraryPreparationKit addAdapterFileToLibraryPreparationKit(LibraryPreparationKit libraryPreparationKit, String adapterFile) {
        assert libraryPreparationKit : "libraryPreparationKit must not be null"
        assert adapterFile : "adapterFile must not be null"
        libraryPreparationKit.adapterFile = adapterFile
        assert libraryPreparationKit.save(flush: true)
        return libraryPreparationKit
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    LibraryPreparationKit addAdapterSequenceToLibraryPreparationKit(LibraryPreparationKit libraryPreparationKit, String reverseComplementAdapterSequence) {
        assert libraryPreparationKit : "libraryPreparationKit must not be null"
        assert reverseComplementAdapterSequence : "reverseComplementAdapterSequence must not be null"
        libraryPreparationKit.reverseComplementAdapterSequence = reverseComplementAdapterSequence
        assert libraryPreparationKit.save(flush: true)
        return libraryPreparationKit
    }

    @Override
    protected LibraryPreparationKit findByName(String name, Map properties = [:]) {
        return CollectionUtils.atMostOneElement(clazz.findAllByNameIlike(name))
    }

    @Override
    protected void checkProperties(Map properties) {
        assert properties.shortDisplayName  : "the input shortDisplayName '${properties.shortDisplayName}' must not be null"
        assert !LibraryPreparationKit.findByShortDisplayName(properties.shortDisplayName) :
                "The shortdisplayname '${properties.shortDisplayName}' exists already"
    }

    @Override
    protected Class getClazz() {
        return LibraryPreparationKit
    }
}
