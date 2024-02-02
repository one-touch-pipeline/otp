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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator

import java.nio.file.Path
import java.util.regex.Pattern

@Transactional
class LsdfFilesService {

    FileSystemService fileSystemService
    ProjectService projectService

    /**
     * This function return path to the initial location
     * of the given sequenceFile
     */
    Path getFileInitialPathAsPath(RawSequenceFile rawSequenceFile) {
        return fileSystemService.remoteFileSystem.getPath(getFileInitialPath(rawSequenceFile))
    }

    /**
     * This function return path to the initial location
     * of the given sequenceFile
     * @deprecated use {@link #getFileInitialPathAsPath}
     */
    @Deprecated
    static String getFileInitialPath(RawSequenceFile dataFile) {
        return "${dataFile.initialDirectory}/${dataFile.fileName}"
    }

    @Deprecated
    // Belongs to the old structure and will be deleted when the uuid structure is implemented
    Path getSeqTypeDirectory(RawSequenceFile rawSequenceFile) {
        Path basePath = projectService.getSequencingDirectory(rawSequenceFile.project)
        String seqTypeDirName = rawSequenceFile.seqTrack?.seqType?.dirName
        if (!seqTypeDirName) {
            return null
        }
        return basePath.resolve(seqTypeDirName)
    }

    @Deprecated
    // Belongs to the old structure and will be deleted when the uuid structure is implemented
    Path getSeqCenterRunDirectory(RawSequenceFile rawSequenceFile) {
        if (!rawSequenceFile.used) {
            return null
        }
        Path basePath = getSeqTypeDirectory(rawSequenceFile)
        String centerDir = rawSequenceFile.run.seqCenter.dirName
        return basePath?.resolve(centerDir)?.resolve(rawSequenceFile.run.dirName)
    }

    /**
     * Similar to {@link java.nio.file.Paths#get(String, String ...)} from Java 7.
     * @deprecated use Path
     */
    @Deprecated
    static File getPath(final String first, final String... more) {
        validatePathSegment(first, "first")
        File file = new File(first)
        for (int i = 0; i < more.length; i++) {
            validatePathSegment(more[i], "more[${i}]")
            file = new File(file, more[i])
        }
        return file
    }

    @Deprecated
    private static void validatePathSegment(final String segment, final String segmentPosition) {
        if (!segment) {
            throw new IllegalArgumentException("${segmentPosition} is blank")
        }
        if (segment =~ /(?:^|${Pattern.quote(File.separator)})\.{1,2}(?:${Pattern.quote(File.separator)}|$)/) {
            throw new IllegalArgumentException("${segmentPosition} contains '.' or '..': ${segment}")
        }
        if (!(segment ==~ OtpPathValidator.PATH_CHARACTERS_REGEX)) {
            throw new IllegalArgumentException("${segmentPosition} contains at least one illegal character: ${segment}")
        }
    }

    /**
     * @deprecated use {@link FileService#ensureFileIsReadableAndNotEmpty}
     */
    @Deprecated
    static void ensureFileIsReadableAndNotEmpty(final File file) {
        FileService.ensureFileIsReadableAndNotEmptyStatic(file.toPath())
    }

    /**
     * @deprecated use {@link FileService#ensureDirIsReadableAndNotEmpty}
     */
    @Deprecated
    static void ensureDirIsReadableAndNotEmpty(final File dir) {
        FileService.ensureDirIsReadableAndNotEmptyStatic(dir.toPath())
    }
}
