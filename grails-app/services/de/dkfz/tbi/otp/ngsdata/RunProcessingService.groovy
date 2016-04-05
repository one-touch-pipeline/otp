package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * This service is responsible for orchestration of workflows running on Run objects.
 */

class RunProcessingService {

    Run runReadyToCheckFinalLocation() {
        def c = RunSegment.createCriteria()
        List<RunSegment> segments = c.list {
            and {
                eq("filesStatus", RunSegment.FilesStatus.NEEDS_CHECKING)
                eq("metaDataStatus", RunSegment.Status.COMPLETE)
            }
        }
        if (!segments) {
            return null
        }
        return segments.first().run
        // TODO check multi-threading
        for (RunSegment segment in segments) {
            Run run = segment.run
            if (checkIfAllSegmentsComplete(run)) {
                return run
            }
        }
        return null
    }

    private boolean checkIfAllSegmentsComplete(run) {
        List<RunSegment> segments =
            RunSegment.findAllByRunAndFilesStatus(run, RunSegment.FilesStatus.NEEDS_CHECKING)
        for (RunSegment segment in segments) {
            if (segment.metaDataStatus != RunSegment.Status.COMPLETE) {
                println "Status: ${segment.metaDataStatus} ${segment.id}"
                return false
            }
        }
        return true
    }

    void blockCheckingFinalLocation(Run run) {
        def c = RunSegment.createCriteria()
        List<RunSegment> segments = c.list {
            and {
                eq("run", run)
                eq("filesStatus", RunSegment.FilesStatus.NEEDS_CHECKING)
            }
        }
        for (RunSegment segment in segments) {
            segment.filesStatus = RunSegment.FilesStatus.PROCESSING_CHECKING
            segment.save(flush: true)
        }
    }

    Run runReadyToInstall(short minPriority) {
        final String hql = """
select
    runSegment.run
from
    DataFile dataFile
    join dataFile.runSegment runSegment
where
    runSegment.metaDataStatus = :metaDataStatus
    and runSegment.filesStatus = :filesStatus
    and dataFile.project.processingPriority >= :minPriority
    and not exists (
        select
            id
        from
            RunSegment runSegment2
        where
            runSegment2.run.id = runSegment.run.id
            and runSegment2.filesStatus in (:processingStatuses)
        )
order by
    dataFile.project.processingPriority desc, runSegment.id asc
"""

        return CollectionUtils.atMostOneElement(Run.executeQuery(hql,[
                metaDataStatus: RunSegment.Status.COMPLETE ,
                filesStatus: RunSegment.FilesStatus.NEEDS_INSTALLATION,
                processingStatuses: RunSegment.PROCESSING_FILE_STATUSES,
                minPriority: minPriority,
                max: 1,
        ]))
    }

    void blockInstallation(Run run) {
        def c = RunSegment.createCriteria()
        List<RunSegment> segments = c.list {
            and{
                eq("run", run)
                eq("metaDataStatus", RunSegment.Status.COMPLETE)
                eq("filesStatus", RunSegment.FilesStatus.NEEDS_INSTALLATION)
            }
        }
        for (RunSegment segment in segments) {
            segment.filesStatus = RunSegment.FilesStatus.PROCESSING_INSTALLATION
            segment.save(flush: true)
        }
    }

    Run runReadyToUnpack() {
        RunSegment segment =
            RunSegment.findByFilesStatus(RunSegment.FilesStatus.NEEDS_UNPACK)
        return (segment) ? segment.run : null
    }

    void blockUnpack(Run run) {
        def c = RunSegment.createCriteria()
        List<RunSegment> segments= c.list {
            and {
                eq("run", run)
                eq("filesStatus", RunSegment.FilesStatus.NEEDS_UNPACK)
            }
        }
        for (RunSegment segment in segments) {
            segment.filesStatus = RunSegment.FilesStatus.PROCESSING_UNPACK
            segment.save(flush: true)
        }
    }

    void setUnpackComplete(Run run) {
        def c = RunSegment.createCriteria()
        List<RunSegment> segments = c.list {
            and {
                eq("run", run)
                eq("filesStatus", RunSegment.FilesStatus.PROCESSING_UNPACK)
            }
        }
        for (RunSegment segment in segments) {
            segment.filesStatus = RunSegment.FilesStatus.NEEDS_INSTALLATION
            segment.currentFormat = RunSegment.DataFormat.FILES_IN_DIRECTORY
            segment.save(flush: true)
        }
    }

    boolean isMetaDataProcessingFinished() {
        List<RunSegment> segments =
            RunSegment.findAllByMetaDataStatusNotEqual(RunSegment.Status.COMPLETE)
        for (RunSegment segment in segments) {
            if (!segment.run.blacklisted) {
                return false
            }
        }
        return true
    }

    public List<RunSegment> runSegmentsWithFilesInProcessing(Run run) {
        List<RunSegment> segments = RunSegment.withCriteria {
            and{
                eq("run", run)
                eq("filesStatus", RunSegment.FilesStatus.PROCESSING_INSTALLATION)
            }
        }
        return segments
    }

    public List<DataFile> dataFilesForProcessing(Run run) {
        List<RunSegment> segments = runSegmentsWithFilesInProcessing(run)
        return DataFile.findAllByRunSegmentInListAndUsed(segments, true)
    }
}
