/*
 * Copyright 2011-2025 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.SeqTrack

import java.nio.file.Path

trait AbstractPanCancerFileService<T extends RoddyBamFile> implements ArtefactFileService<T>, RoddyResultServiceTrait<T>, AbstractAlignmentFileService<T> {

    Path getQADirectory(T bamFile) {
        return getDirectoryPath(bamFile).resolve(RoddyBamFileNames.QUALITY_CONTROL_DIR)
    }

    Path getMergedQADirectory(T bamFile) {
        return getQADirectory(bamFile).resolve(RoddyBamFileNames.MERGED_DIR)
    }

    Path getMergedQAJsonFile(T bamFile) {
        return getMergedQADirectory(bamFile).resolve(RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME)
    }

    Path getMergedQATargetExtractJsonFile(T bamFile) {
        return getMergedQADirectory(bamFile).resolve(RoddyBamFileNames.QUALITY_CONTROL_TARGET_EXTRACT_JSON_FILE_NAME)
    }

    // Example: run140801_SN751_0197_AC4HUVACXX_D2059_AGTCAA_L001
    Map<SeqTrack, Path> getSingleLaneQADirectories(T bamFile) {
        Path baseDirectory = getQADirectory(bamFile)
        Map<SeqTrack, Path> directoriesPerSeqTrack = (bamFile.seqTracks ?: [] as Set<SeqTrack>).collectEntries { SeqTrack seqTrack ->
            [(seqTrack): baseDirectory.resolve(seqTrack.readGroupName)]
        }
        return directoriesPerSeqTrack
    }

    Map<SeqTrack, Path> getSingleLaneQAJsonFiles(T bamFile) {
        return getSingleLaneQADirectories(bamFile).collectEntries { SeqTrack seqTrack, Path directory ->
            [(seqTrack): directory.resolve(RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME)]
        }
    }

    Path getExecutionStoreDirectory(T bamFile) {
        return getDirectoryPath(bamFile).resolve(executionStoreDirectory)
    }

    List<Path> getExecutionDirectories(T bamFile) {
        return bamFile.roddyExecutionDirectoryNames.collect {
            getExecutionStoreDirectory(bamFile).resolve(it)
        }
    }

    // Example: blood_somePid_merged.mdup.bam.md5
    Path getMd5sumFile(T bamFile) {
        return getDirectoryPath(bamFile).resolve("${bamFile.bamFileName}.md5")
    }
}
