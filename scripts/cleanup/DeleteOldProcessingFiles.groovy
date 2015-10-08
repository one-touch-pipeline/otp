import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.joda.time.*

/**
 * Only files created before this instant will be deleted.
 */
final ReadableInstant minCreationInstant = new Interval(Period.weeks(8), Instant.now()).start

/**
 * Roughly the maximum allowed runtime for this script.
 */
final ReadableDuration maxRuntime = Duration.standardHours(1)


try {
    PrintWriter out = new PrintWriter(
        "$SCRIPT_ROOT_PATH/script_output/cleanup/DeleteOldProcessingFiles_" + new Date().format("yyyy-MM-dd_HH.mm.ss.SSS_Z") + ".log")
    try {
        final Instant startTime = Instant.now()
        long freedBytes = 0L
        LogThreadLocal.withThreadLog(out, {
            freedBytes += ctx.processedAlignmentFileService.deleteOldAlignmentProcessingFiles(minCreationInstant.toDate(), maxRuntime.millis)
            final Duration maxRemainingRuntime = new Duration(Instant.now(), startTime.plus(maxRuntime))
            if (maxRemainingRuntime > Duration.ZERO) {
                freedBytes += ctx.mergingPassService.deleteOldMergingProcessingFiles(minCreationInstant.toDate(), maxRemainingRuntime.millis)
            }
            threadLog.info "${freedBytes} bytes have been freed in total."
        })
        assert !out.checkError()
    } catch (final Throwable e) {
        e.printStackTrace(out)
        throw e
    } finally {
        out.flush()
        out.close()
    }
} catch (final Throwable e) {
    e.printStackTrace(System.out)
}
