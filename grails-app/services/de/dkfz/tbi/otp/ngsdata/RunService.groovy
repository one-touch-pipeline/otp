package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.ProcessParameter

/**
 * Service to retrieve information about Runs.
 * This service provides ACL protected access to Runs and the information connected with a given
 * Run. It's main usage is from a controller.
 *
 * TODO: add ACL
 **/
class RunService {

    /**
     * Retrieves the given Run.
     * If the parameter can be converted to a Long it is assumed to be the database ID.
     * Otherwise it is tried to find the Run by its name - as well if the Run could not
     * be found by the database id.
     *
     * If no Run is found null is returned.
     * @param identifier Name or database Id
     * @return Run
     **/
    Run getRun(String identifier) {
        if (!identifier) {
            return null
        }
        Run run = null
        if (identifier?.isLong()) {
            run = Run.get(identifier as Long)
        }
        if (!run) {
            run = Run.findByName(identifier)
        }
        return run
    }

    /**
     * Retrieves the given Run.
     * Overloaded method for convenience.
     * @param identifier Name or database Id
     * @return Run
     **/
    Run getRun(long identifier) {
        return getRun("${identifier}")
    }

    /**
     * Retrieves the ProcessParameters for the given Run.
     * @param run The Run for which the ProcessParameter should be retrieved.
     * @return List of ProcessParameter
     **/
    List<ProcessParameter> retrieveProcessParameters(Run run) {
        if (!run) {
            return []
        }
        return ProcessParameter.findAllByValueAndClassName("${run.id}", run.class.name)
    }

    /**
     * Retrieves the previous Run by database id if present.
     * @param run The Run for which the predecessor has to be retrieved
     * @return Previous Run if present, otherwise null
     **/
    Run previousRun(Run run) {
        if (!run) {
            return null
        }
        return Run.findByIdLessThan(run.id, [sort: "id", order: "desc"])
    }

    /**
     * Retrieves the next Run by database id if present.
     * @param run The Run for which the successor has to be retrieved
     * @return Next Run if present, otherwise null
     **/
    Run nextRun(Run run) {
        if (!run) {
            return null
        }
        return Run.findByIdGreaterThan(run.id, [sort: "id", oder: "asc"])
    }

    /**
     * Retrieves list of MetaDataFiles for the given Run by its initial run path.
     * @param Run The run for which the MetaDataFiles should be retrieved
     * @return List of MetaDataFile
     **/
    List<MetaDataFile> retrieveMetaDataFilesByInitialPath(Run run) {
        if (!run) {
            return []
        }
        // TODO: query with a criteria
        List<MetaDataFile> files = []
        List<RunInitialPath> paths = RunInitialPath.findAllByRun(run)
        paths.each {
            MetaDataFile.findAllByRunInitialPath(it).each {
                files << it
            }
        }
        return files
    }

    /**
     * Retrieves information about the Sequence Track of a given Run.
     * The returned data is a Map having SeqTrack as key and another Map as value.
     * This map consists of an element "files" with a list of DataFiles for the specific
     * SeqTrack as value and an element "alignments" with a Map of the Alignments for the
     * specific SeqTrack and a list of DataFiles for each Alignment.
     * Summarized the structure looks like the following
     * <ul>
     * <li>SeqTrack:<ul>
     *   <li>files: [DataFile, DataFile]</li>
     *   <li>alignments: <ul>
     *     <li>Alignment: [DataFile, DataFile]</li>
     *     <li>Alignment: [DataFile, DataFile]</li>
     *   </ul></li>
     * </ul></li>
     * <ul>
     * @param run The Run for which the Sequence Track information should be retrieved
     * @return Data Structure as described above
     **/
    Map<SeqTrack, Map<String, Object>> retrieveSequenceTrackInformation(Run run) {
        Map<SeqTrack, Map<String, Object>> returnData = new LinkedHashMap<SeqTrack, Map<String, Object>>()
        if (!run) {
            return returnData
        }
        SeqTrack.findAllByRun(run, [sort: 'laneId']).each { track ->
            Map<AlignmentLog, List<DataFile>> alignments = new LinkedHashMap<AlignmentLog, List<DataFile>>()
            AlignmentLog.findAllBySeqTrack(track).each { alignment ->
                alignments.put(alignment, DataFile.findAllByAlignmentLog(alignment))
            }
            Map dataElement = [files: DataFile.findAllBySeqTrack(track), alignments: alignments]
            returnData.put(track, dataElement)
        }
        returnData
    }

    /**
     * Retrieves all DataFiles for the given run with errors.
     * Error is identified by the fact that the file is not used.
     * List is sorted by file name.
     * @param run The Run for which the errornous data files need to be retrieved
     * @return List of DataFiles with errors.
     **/
    List<DataFile> dataFilesWithError(Run run) {
        return DataFile.findAllByRunAndUsed(run, false, [sort: "fileName"])
    }
}