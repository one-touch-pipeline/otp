package de.dkfz.tbi.otp.ngsdata

/**
 * This service is responsible for orchestration of workflows running on Run objects.
 */

class RunProcessingService {

    /**
     * Returns a run which has new RunSegment
     * @return
     */
    Run runWithNewMetaData() {
        RunSegment segment =
            RunSegment.findByMetaDataStatusAndCurrentFormatNotEqual(
                    RunSegment.Status.NEW, RunSegment.DataFormat.TAR
                 )
        return (segment) ? segment.run : null
    }

    void blockMetaData(Run run) {
        def c = RunSegment.createCriteria()
        List<RunSegment> segments = c.list {
            and{
                eq("run", run)
                eq("metaDataStatus", RunSegment.Status.NEW)
                ne("currentFormat", RunSegment.DataFormat.TAR)
            }
        }
        for (RunSegment segment in segments) {
            segment.metaDataStatus = RunSegment.Status.BLOCKED
            segment.save(flush: true)
        }
    }

    public List<DataFile> dataFilesWithMetaDataInProcessing(Run run) {
        List<RunSegment> segments =
            RunSegment.findAllByRunAndMetaDataStatus(run, RunSegment.Status.PROCESSING)
        return DataFile.findAllByRunSegmentInList(segments)
    }

    void setMetaDataComplete(Run run) {
        List<RunSegment> segments =
            RunSegment.findAllByRunAndMetaDataStatus(run, RunSegment.Status.PROCESSING)
        for (RunSegment segment in segments) {
            segment.metaDataStatus = RunSegment.Status.COMPLETE
            segment.save(flush: true)
        }
    }

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

    Run runReadyToInstall() {
        def c = RunSegment.createCriteria()
        List<RunSegment> segments = c.list {
            and{
                eq("metaDataStatus", RunSegment.Status.COMPLETE)
                eq("filesStatus", RunSegment.FilesStatus.NEEDS_INSTALLATION)
            }
        }
        if (!segments) {
            return null
        }
        return segments.first().run
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
