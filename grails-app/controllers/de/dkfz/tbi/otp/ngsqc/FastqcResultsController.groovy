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
package de.dkfz.tbi.otp.ngsqc

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

@PreAuthorize('isFullyAuthenticated()')
class FastqcResultsController {

    FastqcDataFilesService fastqcDataFilesService
    FastQcProcessedFileService fastQcProcessedFileService
    FastqcResultsService fastqcResultsService
    LsdfFilesService lsdfFilesService
    MetaDataService metaDataService

    static allowedMethods = [
            show      : "GET",
            renderFile: "GET",
    ]

    def show() {
        RawSequenceFile rawSequenceFile = metaDataService.getRawSequenceFile(params.id as long)
        if (!rawSequenceFile) {
            return response.sendError(404)
        }

        String result

        if (fastqcResultsService.isFastqcAvailable(rawSequenceFile)) {
            FastqcProcessedFile fastqcProcessedFile = fastQcProcessedFileService.findSingleByRawSequenceFile(rawSequenceFile)
            String html = fastqcDataFilesService.getInputStreamFromZipFile(fastqcProcessedFile, "fastqc_report.html").text

            Elements elements = Jsoup.parse(html).select("div.summary, div.main, div.footer")

            elements.select("h2").first().remove()

            elements.select("img").iterator().each { Element img ->
                String file = img.attr("src")
                String link
                if (!file.startsWith("data:")) {
                    link = createLink(controller: "fastqcResults",
                            action: "renderFile",
                            params: ["dataFile.id": rawSequenceFile.id, path: file]
                    )
                    img.attr("src", link)
                }
            }
            result = elements.toString()
        } else {
            result = message(code: "fastqc.show.notAvailable")
        }
        return [
                html      : result,
                pid       : rawSequenceFile.individual.displayName,
                runName   : rawSequenceFile.run.name,
                laneId    : rawSequenceFile.seqTrack.laneId,
                mateNumber: rawSequenceFile.mateNumber,
        ]
    }

    /**
     * All images and flat text files from fastqc are zipped.
     * So this closure is used to get content to be rendered by the browser from within the fastqc zip file
     */
    def renderFile(RenderFileCommand cmd) {
        if (cmd.hasErrors()) {
            return response.sendError(404)
        }
        InputStream stream
        try {
            RawSequenceFile rawSequenceFile = metaDataService.getRawSequenceFile(cmd.dataFile.id as long)
            FastqcProcessedFile fastqcProcessedFile = fastQcProcessedFileService.findSingleByRawSequenceFile(rawSequenceFile)
            stream = fastqcDataFilesService.getInputStreamFromZipFile(fastqcProcessedFile, cmd.path)
        } catch (FileNotReadableException e) {
            render(status: 404)
            return void
        }
        render(file: stream, contentType: "image/png")
    }
}

class RenderFileCommand {
    RawSequenceFile dataFile
    String path

    static constraints = {
        dataFile nullable: false
        path nullable: false, blank: false
    }
}
