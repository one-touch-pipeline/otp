package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

import java.util.zip.*

@Component
@Scope("prototype")
@UseJobLog
class CheckQualityEncodingJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    SeqTrackService seqTrackService

    @Override
    public void execute() throws Exception {
        long alignmentPassId = Long.parseLong(getProcessParameterValue())
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
        SeqTrack seqTrack = alignmentPass.seqTrack
        List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
        for (DataFile file in files) {
            if (analyzeEncoding(seqTrack, file)) {
                succeed()
                return
            }
        }
        fail()
    }

    private boolean analyzeEncoding(SeqTrack seqTrack, DataFile dataFile) {
        LineNumberReader reader = openStream(dataFile)
        while (true) {
            String line = reader.readLine()
            // check if end of file reached
            if (line == null) {
                return false
            }
            if ((reader.getLineNumber() - 1) % 4 == 3) {
                SeqTrack.QualityEncoding encoding = searchForPattern(line)
                if (encoding != SeqTrack.QualityEncoding.UNKNOWN) {
                    seqTrack.qualityEncoding = encoding
                    assert(seqTrack.save(flush: true))
                    return true
                }
            }
        }
        return false
    }

    private LineNumberReader openStream(DataFile dataFile) {
        File file = new File(lsdfFilesService.getFileViewByPidPath(dataFile))

        if (!file.canRead()) {
            throw new FileNotReadableException(file)
        }
        GZIPInputStream zis = new GZIPInputStream(new FileInputStream(file))
        return new LineNumberReader(new InputStreamReader(zis))
    }

    private SeqTrack.QualityEncoding searchForPattern(String line) {
        final String phredPattern = /[\x22-\x3f]/
        final String illuminaPattern = /[\x5e-\x7e]/
        if (line =~ phredPattern) {
            return SeqTrack.QualityEncoding.PHRED
        }
        if (line =~ illuminaPattern) {
            return SeqTrack.QualityEncoding.ILLUMINA
        }
        return SeqTrack.QualityEncoding.UNKNOWN
    }
}
