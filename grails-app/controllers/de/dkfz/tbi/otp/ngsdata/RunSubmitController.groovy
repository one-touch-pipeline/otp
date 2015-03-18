package de.dkfz.tbi.otp.ngsdata

import grails.plugins.springsecurity.Secured

@Secured(['ROLE_ADMIN'])
class RunSubmitController {

    def runSubmitService

    def index() {
        def centers = SeqCenter.list()
        def seqPlatforms = SeqPlatform.list().sort {it.fullName()}
        return [centers: centers, seqPlatforms: seqPlatforms, cmd: flash.params]
    }

    def submit(SubmitCommand cmd) {
        if(cmd.hasErrors()) {
            flash.params = cmd
            redirect(action: "index")
        }  else {
            int position = cmd.metaDataFile.lastIndexOf('/')
            String dataPath = cmd.metaDataFile.substring(0, position)
            String runName = cmd.metaDataFile.substring(position + 1)
            if(runName.startsWith("run")) {
                runName = runName.substring(3)
            }
            long runId = runSubmitService.submit(runName, cmd.seqPlatformId, cmd.seqCenter, dataPath, cmd.align)
            redirect(controller: "run", action: "show", id: runId)

        }
    }
}

class SubmitCommand implements Serializable {
    String metaDataFile
    String seqPlatformId
    String seqCenter
    boolean align

    static constraints = {
        importFrom Run, include: ["seqCenter"]
        importFrom RunSegment, include: ["align"]
        metaDataFile blank: false, validator: {
            File file = new File(it)
            file.list() //Workaround to reload the file system inode
            file.isAbsolute() && file.exists() && file.canRead()
        }
    }
}
