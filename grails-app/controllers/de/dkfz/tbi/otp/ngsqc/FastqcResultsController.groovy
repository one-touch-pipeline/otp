package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements


class RenderFileCommand {
    DataFile dataFile
    String path

    static constraints = {
        dataFile nullable: false
        path nullable: false, blank: false
    }
}

class FastqcResultsController {

    FastqcDataFilesService fastqcDataFilesService
    FastqcResultsService fastqcResultsService
    LsdfFilesService lsdfFilesService
    MetaDataService metaDataService

    def show() {
        DataFile dataFile = metaDataService.getDataFile(params.id as long)
        if (!dataFile) {
            render status: 404
            return
        }

        String result

        if(!fastqcResultsService.isFastqcAvailable(dataFile)) {
            result = message(code: "fastqc.show.notAvailable")
        } else {
            String html = fastqcDataFilesService.getInputStreamFromZipFile(dataFile, "fastqc_report.html").text

            Elements elements = Jsoup.parse(html).select("div.summary, div.main, div.footer")

            elements.select("h2").first().remove()

            elements.select("img").iterator().each { Element img ->
                String file = img.attr("src")
                String link
                switch (file) {
                    case "Icons/tick.png":
                        link = createLink(controller: "assets", action: "ok.png")
                        break
                    case "Icons/warning.png":
                        link = createLink(controller: "assets", action: "warning.png")
                        break
                    case "Icons/error.png":
                        link = createLink(controller: "assets", action: "error.png")
                        break
                    default:
                        link = createLink(controller: "fastqcResults",
                                action: "renderFile",
                                params: ["dataFile.id": dataFile.id, path: file]
                        )
                }
                img.attr("src", link)
            }
            result = elements.toString()
        }
        return [
                html      : result,
                pid       : dataFile.individual.displayName,
                runName   : dataFile.run.name,
                laneId    : dataFile.seqTrack.laneId,
                mateNumber: dataFile.mateNumber,
        ]
    }

    /**
     * All images and flat text files from fastqc are zipped.
     * So this closure is used to get content to be rendered by the browser from within the fastqc zip file
     */
    def renderFile(RenderFileCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
        } else {
            InputStream stream
            try {
                stream = fastqcDataFilesService.getInputStreamFromZipFile(cmd.dataFile, cmd.path)
            } catch (FileNotReadableException e) {
                render status: 404
                return
            }
            render file: stream , contentType: "image/png"
        }
    }
}
