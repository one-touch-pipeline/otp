import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.apache.commons.logging.impl.SimpleLog

LogThreadLocal.setLog(new SimpleLog("ScriptLog") {
    protected void write(StringBuffer buffer) {
        System.out.println(buffer.toString())
    }
})
final Date someTimeAgo = new Date(System.currentTimeMillis() - 3L * 7L * 24L * 60L * 60L * 1000L)
try {
    ctx.processedAlignmentFileService.deleteOldAlignmentProcessingFiles(someTimeAgo)
} catch (final Throwable e) {
    e.printStackTrace(System.out)
}
