package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.job.processing.Process

class RunController {

    def lsdfFilesService
    def metaDataService
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
}
