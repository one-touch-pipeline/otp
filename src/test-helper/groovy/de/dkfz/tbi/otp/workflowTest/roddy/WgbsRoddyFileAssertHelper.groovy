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
package de.dkfz.tbi.otp.workflowTest.roddy

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.WgbsAlignmentLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.WgbsAlignmentWorkFileService

import java.nio.file.Path

@Component
class WgbsRoddyFileAssertHelper extends RoddyFileAssertHelper {

    @Autowired
    WgbsAlignmentLinkFileService wgbsAlignmentLinkFileService

    @Autowired
    WgbsAlignmentWorkFileService wgbsAlignmentWorkFileService

    @Override
    List<Path> getAdditionalDirectories(RoddyBamFile bamFile) {
        return [
                wgbsAlignmentLinkFileService.getMethylationDirectory(bamFile),
                wgbsAlignmentLinkFileService.getMergedMethylationDirectory(bamFile),
        ]
    }

    @Override
    List<Path> getAdditionalLinks(RoddyBamFile bamFile) {
        return [
                wgbsAlignmentLinkFileService.getMetadataTableFile(bamFile)
        ] + (bamFile.hasMultipleLibraries() ?
                (wgbsAlignmentLinkFileService.getLibraryMethylationDirectories(bamFile).values() +
                        wgbsAlignmentLinkFileService.getLibraryQADirectories(bamFile).values()) : [])
    }

    @Override
    List<Path> getAdditionalQaDirectories(RoddyBamFile bamFile) {
        return bamFile.hasMultipleLibraries() ?
                wgbsAlignmentLinkFileService.getLibraryQADirectories(bamFile).values().toList() : []
    }

    @Override
    List<Path> getAdditionalWorkDirectories(RoddyBamFile bamFile) {
        return [
                wgbsAlignmentWorkFileService.getMethylationDirectory(bamFile),
                wgbsAlignmentWorkFileService.getMergedMethylationDirectory(bamFile),
        ] + (bamFile.hasMultipleLibraries() ? (
                wgbsAlignmentWorkFileService.getLibraryMethylationDirectories(bamFile).values() +
                        wgbsAlignmentWorkFileService.getLibraryQADirectories(bamFile).values()) : [])
    }

    @Override
    List<Path> getAdditionalWorkFiles(RoddyBamFile bamFile) {
        return [wgbsAlignmentWorkFileService.getMetadataTableFile(bamFile)]
    }

    @Override
    List<Path> getAdditionalQaWorkDirectories(RoddyBamFile bamFile) {
        return bamFile.hasMultipleLibraries() ?
                wgbsAlignmentWorkFileService.getLibraryQADirectories(bamFile).values().toList() : []
    }

    @Override
    List<Path> getAdditionalQaWorkFiles(RoddyBamFile bamFile) {
        return bamFile.hasMultipleLibraries() ?
                wgbsAlignmentWorkFileService.getLibraryQAJsonFiles(bamFile).values().toList() : []
    }
}
