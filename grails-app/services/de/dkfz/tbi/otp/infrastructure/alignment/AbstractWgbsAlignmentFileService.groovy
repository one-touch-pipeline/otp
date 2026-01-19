/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.infrastructure.alignment

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile

import java.nio.file.Path

trait AbstractWgbsAlignmentFileService extends AbstractPanCancerFileService<RoddyBamFile> {

    static final String METADATATABLE_FILE = 'metadataTable.tsv'

    static final String METHYLATION_DIR = 'methylation'

    Map<String, Path> getLibraryQADirectories(RoddyBamFile bamFile) {
        return getLibraryDirectories(bamFile, getQADirectory(bamFile))
    }

    Map<String, Path> getLibraryMethylationDirectories(RoddyBamFile bamFile) {
        return getLibraryDirectories(bamFile, getMethylationDirectory(bamFile))
    }

    Path getMergedMethylationDirectory(RoddyBamFile bamFile) {
        return getMethylationDirectory(bamFile).resolve(RoddyBamFileNames.MERGED_DIR)
    }

    private Map<String, Path> getLibraryDirectories(RoddyBamFile bamFile, Path baseDirectory) {
        return bamFile.seqTracks.collectEntries {
            [(it.libraryDirectoryName): baseDirectory.resolve(it.libraryDirectoryName)]
        }
    }

    Map<String, Path> getLibraryQAJsonFiles(RoddyBamFile bamFile) {
        return getLibraryQADirectories(bamFile).collectEntries { String lib, Path directory ->
            [(lib): directory.resolve(RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME)]
        }
    }

    Path getMetadataTableFile(RoddyBamFile bamFile) {
        return getDirectoryPath(bamFile).resolve(METADATATABLE_FILE)
    }

    Path getMethylationDirectory(RoddyBamFile bamFile) {
        return getDirectoryPath(bamFile).resolve(METHYLATION_DIR)
    }
}
