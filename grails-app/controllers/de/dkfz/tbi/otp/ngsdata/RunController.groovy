/*
 * Copyright 2011-2023 The OTP authors
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

import de.dkfz.tbi.otp.ngsqc.FastqcResultsService
import de.dkfz.tbi.otp.utils.CollectionUtils

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class RunController {

    LsdfFilesService lsdfFilesService
    RunService runService
    FastqcResultsService fastqcResultsService
    MetadataImportService metadataImportService

    static allowedMethods = [
            display        : "GET",
            show           : "GET",
    ]

    def display() {
        redirect(action: "show", id: params.id)
    }

    def show() {
        params.id = params.id ?: "0"
        Run run = runService.getRun(params.id)
        if (!run || run.project?.archived) {
            return response.sendError(404)
        }
        // This page requires using SAMPLE_NAME, since the RawSequenceFile has no connection to a SeqTrack. Its only used for legacy objects
        List<MetaDataKey> keys = []
        keys[0] = CollectionUtils.atMostOneElement(MetaDataKey.findAllByName(MetaDataColumn.SAMPLE_NAME.name()))
        keys[1] = CollectionUtils.atMostOneElement(MetaDataKey.findAllByName(MetaDataColumn.WITHDRAWN.name()))

        List<Map<String, Object>> wrappedMetaDataFiles = runService.retrieveMetaDataFiles(run).collect {
            [
                    metaDataFile  : it,
                    fullPathSource: metadataImportService.getMetaDataFileFullPath(it),
                    fullPathTarget: it.filePathTarget,
            ]
        }

        List<String> finalPaths = RawSequenceFile.findAllByRun(run).collect { RawSequenceFile dataFile ->
            lsdfFilesService.getSeqCenterRunDirectory(dataFile) as String ?: dataFile.initialDirectory
        }.unique().sort()

        return [
                run                : run,
                finalPaths         : finalPaths,
                keys               : keys,
                processParameters  : runService.retrieveProcessParameters(run),
                metaDataFileWrapper: wrappedMetaDataFiles,
                seqTracks          : runService.retrieveSequenceTrackInformation(run),
                errorFiles         : runService.rawSequenceFilesWithError(run),
                fastqcLinks        : fastqcResultsService.fastqcLinkMap(run),
        ]
    }
}
