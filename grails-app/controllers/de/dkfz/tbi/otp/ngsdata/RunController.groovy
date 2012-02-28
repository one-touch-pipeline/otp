package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.job.processing.Process

class RunController {

    def lsdfFilesService
    def metaDataService
    def seqTrackService
    def processService

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
        String[] finalPaths = lsdfFilesService.getAllPathsForRun(run)
        List<MetaDataKey> keys = []
        keys[0] = MetaDataKey.findByName("SAMPLE_ID")
        keys[1] = MetaDataKey.findByName("WITHDRAWN")

        // processing
        List<ProcessParameter> processParameters = 
            ProcessParameter.findAllByValue(run.id, run.class.name)

        int prevId = findPrevious(run)
        int nextId = findNext(run)
        return [run: run, finalPaths: finalPaths, keys: keys, processParameters: processParameters, nextId: nextId, prevId: prevId]
    }

    private int findPrevious(Run run) {
        List<Run> runs =
             Run.findAllByIdLessThan(run.id, [sort: "id", order: "desc", max: 1])
        return (runs.size()>0) ? runs.get(0).id : run.id
    }

    private int findNext(Run run) {
        List<Run> runs =
             Run.findAllByIdGreaterThan(run.id, [sort: "id", order: "asc", max: 1])
        return (runs.size()>0) ? runs.get(0).id : run.id
    }
    
    
    def submitForm = {
        List<SeqCenter> centers = SeqCenter.findAll()
        List<String> centerNames = new Vector<String>()
        centers.each { centerNames << it.name }
        List<SeqPlatform> seqTechs = SeqPlatform.findAll()
        [centers: centerNames, seqTechs: seqTechs ]
    }

    def submit = {
        SeqCenter seqCenter = SeqCenter.findByName(params.center)
        SeqPlatform seqTech = SeqPlatform.findByName(params.seqTech)
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
