import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.apache.commons.logging.Log
import org.apache.commons.logging.impl.SimpleLog
import org.joda.time.*

PrintWriter out = new PrintWriter(new FileOutputStream(
    "$SCRIPT_ROOT_PATH/cleanup/DeleteOldProcessingFiles_" + new Date().format("yyyy-MM-dd_HH.mm.ss.SSS_Z") + ".log"), true)

final Log log = new SimpleLog("ScriptLog") {
    protected void write(StringBuffer buffer) {
        out.println(buffer.toString())
    }
}
assert LogThreadLocal.threadLog == null
LogThreadLocal.setThreadLog(log)
try {
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
} finally {
    LogThreadLocal.removeThreadLog()
}
