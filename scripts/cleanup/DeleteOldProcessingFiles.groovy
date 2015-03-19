import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.joda.time.*

PrintWriter out = new PrintWriter(new FileOutputStream(
    "$SCRIPT_ROOT_PATH/cleanup/DeleteOldProcessingFiles_" + new Date().format("yyyy-MM-dd_HH.mm.ss.SSS_Z") + ".log"), true)

LogThreadLocal.withThreadLog(out, {
    final ReadableInstant someTimeAgo = new Interval(Period.weeks(8), Instant.now()).start
    try {
        long freedBytes = 0L
        freedBytes += ctx.processedAlignmentFileService.deleteOldAlignmentProcessingFiles(someTimeAgo.toDate(), Duration.standardMinutes(60).millis)
        freedBytes += ctx.mergingPassService.deleteOldMergingProcessingFiles(someTimeAgo.toDate(), Duration.standardMinutes(60).millis)
        log.info "${freedBytes} bytes have been freed in total."
    } catch (final Throwable e) {
        e.printStackTrace(out)
        e.printStackTrace(System.out)
    }
})
