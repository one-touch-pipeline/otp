package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.core.userdetails.UserDetails
import de.dkfz.tbi.otp.job.processing.ProcessParameter

/**
 * Service to retrieve information about Runs.
 * This service provides ACL protected access to Runs and the information connected with a given
 * Run. It's main usage is from a controller.
 */
class RunService {
    /**
     * Dependency Injection of Spring Security Service - needed for ACL checks
     */
    def springSecurityService

    def seqCenterService

    /**
     * Retrieves the given Run.
     * If the parameter can be converted to a Long it is assumed to be the database ID.
     * Otherwise it is tried to find the Run by its name - as well if the Run could not
     * be found by the database id.
     *
     * If no Run is found null is returned.
     * @param identifier Name or database Id
     * @return Run
     */

    List<Run> listRuns(boolean sortOrder, RunSortColumn column, RunFiltering filtering, String filter) {
        List seqCenters = seqCenterService.allSeqCenters()
        if (!seqCenters) {
            return []
        }
        def c = Run.createCriteria()
        return c.list {
            'in'('seqCenter', seqCenters)
            if (filter.length() >= 3) {
                filter = "%${filter}%"
                or {
                    ilike("name", filter)
                    seqCenter {
                        ilike("name", filter)
                    }
                }
            }
            if (filtering.name) {
                or {
                    filtering.name.each {
                        ilike('name', "%${it}%")
                    }
                }
            }
            if (filtering.seqCenter) {
                seqCenter {
                    'in'('id', filtering.seqCenter)
                }
            }
            if (filtering.dateCreated) {
                or {
                    filtering.dateCreated.each {
                        between('dateCreated', it[0], it[1])
                    }
                }
            }
            if (filtering.dateExecuted) {
                or {
                    filtering.dateExecuted.each {
                        between('dateExecuted', it[0], it[1])
                    }
                }
            }
            if (column.columnName == "seqCenter") {
                seqCenter {
                    order("name", sortOrder ? "asc" : "desc")
                }
            } else {
                order(column.columnName, sortOrder ? "asc" : "desc")
            }
        }
    }
    /**
     * Counts the Runs the User has access to by applying the provided filtering.
     * @param filtering The filters to apply on the data
     * @param filter Restrict on this search filter if at least three characters
     * @return Number of Runs matching the filtering
     */
    int countRun(RunFiltering filtering, String filter) {
        if (filtering.enabled) {
            def c = Run.createCriteria()
            return c.get {
                'in'('seqCenter', seqCenterService.allSeqCenters())
                if (filter.length() >= 3) {
                    filter = "%${filter}%"
                    or {
                        ilike("name", filter)
                        seqCenter {
                            ilike("name", filter)
                        }
                    }
                }
                if (filtering.name) {
                    or {
                        filtering.name.each {
                            ilike('name', "%${it}%")
                        }
                    }
                }
                if (filtering.seqCenter) {
                    seqCenter {
                        'in'('id', filtering.seqCenter)
                    }
                }
                if (filtering.dateCreated) {
                    or {
                        filtering.dateCreated.each {
                            between('dateCreated', it[0], it[1])
                        }
                    }
                }
                if (filtering.dateExecuted) {
                    or {
                        filtering.dateExecuted.each {
                            between('dateExecuted', it[0], it[1])
                        }
                    }
                }
                projections {
                    count('name')
                }
            }
        } else {
            // shortcut for unfiltered results
            return Run.countBySeqCenterInList(seqCenterService.allSeqCenters())
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Run checkPermission(Run run) {
        return run
    }

    @PostAuthorize("hasRole('ROLE_OPERATOR')")
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
     * */
    @PostAuthorize("hasRole('ROLE_OPERATOR')")
    Run getRun(long identifier) {
        return getRun("${identifier}")
    }

    /**
     * Retrieves the ProcessParameters for the given Run.
     * @param run The Run for which the ProcessParameter should be retrieved.
     * @return List of ProcessParameter
     * */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<ProcessParameter> retrieveProcessParameters(Run run) {
        if (!run) {
            return []
        }
        return ProcessParameter.findAllByValueAndClassName("${run.id}", run.class.name)
    }


    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Collection<MetaDataFile> retrieveMetaDataFiles(Run run) {
        Collection<DataFile> dataFiles = DataFile.findAllByRun(run)
        return dataFiles ? MetaDataFile.findAllByRunSegmentInList(dataFiles*.runSegment) : Collections.emptyList()
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
     * */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Map<SeqTrack, Map<String, Object>> retrieveSequenceTrackInformation(Run run) {
        Map<SeqTrack, Map<String, Object>> returnData = [:]
        if (!run) {
            return returnData
        }
        SeqTrack.findAllByRun(run, [sort: 'laneId']).each { track ->
            Map<AlignmentLog, List<DataFile>> alignments = [:]
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
     * */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<DataFile> dataFilesWithError(Run run) {
        return DataFile.findAllByRunAndUsed(run, false, [sort: "fileName"])
    }
}
