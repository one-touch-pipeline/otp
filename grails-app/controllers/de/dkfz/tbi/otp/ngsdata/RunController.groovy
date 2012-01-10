package de.dkfz.tbi.otp.ngsdata

class RunController {

    def lsdfFilesService
    def metaDataService
    def seqTrackService

    static scaffold = Run

    def display = {
        Run run = Run.findByName(params.id)
        long id = run.id
        redirect(action: "show", id:id)
    }

    def show = {
        int id = params.id as int
        Run run = Run.get(id)
        if (!run) {
            render ("run id=${id} does not exist.")
        }
        String[] paths = lsdfFilesService.getAllPathsForRun(run)
        List<MetaDataKey> keys = []
        keys[0] = MetaDataKey.findByName("SAMPLE_ID")
        keys[1] = MetaDataKey.findByName("WITHDRAWN")

        int prevId = (id > 1)? id-1 : 1
        int nextId = id+1
        return [run: run, finalPaths: paths, keys: keys, nextId: nextId, prevId: prevId]
    }

    def submitForm = {
        List<SeqCenter> centers = SeqCenter.findAll()
        List<String> centerNames = new Vector<String>()
        centers.each { centerNames << it.name }
        List<SeqTech> seqTechs = SeqTech.findAll()
        [centers: centerNames, seqTechs: seqTechs ]
    }

    def submit = {
        SeqCenter seqCenter = SeqCenter.findByName(params.center)
        SeqTech seqTech = SeqTech.findByName(params.seqTech)
        Run run = Run.findByName(params.runName)
        if (run) {
            log.debug("Run ${params.runName} already exist")
        } else {
            run = new Run(
                    name: params.runName,
                    seqCenter: seqCenter,
                    seqTech: seqTech,
                    )
        }
        run.dataPath = params.dataPath
        run.mdPath = params.mdPath
        run.save(flush: true)
        log.debug(run)
        long id = run.id

        // processing and poor's man time measurement

        def times = []
        println run.name
        times << new Date().getTime()
        metaDataService.registerInputFiles(id)
        times << new Date().getTime()
        metaDataService.loadMetaData(id)
        times << new Date().getTime()
        boolean valid = metaDataService.validateMetadata(id)
        if (!valid) {
            println "!!! Meta Data not valid !!!"
            render ("!!! Meta Data not valid !!!")
            return
        }
        times << new Date().getTime()
        metaDataService.buildExecutionDate(id)
        times << new Date().getTime()
        seqTrackService.buildSequenceTracks(id)
        times << new Date().getTime()
        seqTrackService.checkSequenceTracks(id)
        times << new Date().getTime()
        metaDataService.assignFilesToProjects(id)
        times << new Date().getTime()
        lsdfFilesService.runInFinalLocation(id)
        lsdfFilesService.checkAllFiles(id)
        times << new Date().getTime()

        println "total ${times[8]-times[0]}"
        for(int i=1; i<times.size(); i++) {
            println "step ${i} ${times[i]-times[i-1]}"
        }

        redirect(action: "show", id:id)
    }
}
