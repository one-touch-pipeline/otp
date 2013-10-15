package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.SAMFileHeader
import net.sf.samtools.SAMFileReader
import net.sf.samtools.SAMRecord
import net.sf.samtools.SAMRecordIterator
import net.sf.samtools.SAMSequenceRecord
import de.dkfz.tbi.ngstools.qualityAssessment.GenomeStatistic.ChromosomeDto

class SAMBamFileReaderImpl implements BamFileReader<SAMRecord> {

    private Parameters parameters

    private FileParameters fileParameters

    private GenomeStatisticFactory<SAMRecord> factory

    @Override
    public void setParameters(Parameters parameters) {
        this.parameters = parameters
    }

    @Override
    public void setFileParameters(FileParameters fileParameters) {
        this.fileParameters = fileParameters
    }

    @Override
    public void setGenomeStatisticFactory(GenomeStatisticFactory<SAMRecord> factory) {
        this.factory = factory
    }

    @Override
    GenomeStatistic<SAMRecord> read(File bamFile, File indexFile) {
        if (!bamFile.canRead()) {
            throw new RuntimeException("The bam file '${bamFile}' is not readable")
        }

        if (!indexFile.canRead()) {
            throw new RuntimeException("The index file '${indexFile}' is not readable")
        }

        SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT)
        SAMFileReader samFileReader = new SAMFileReader(bamFile, indexFile)
        SAMFileHeader header = samFileReader.getFileHeader()
        GenomeStatistic<SAMRecord> genomeStatistic = this.initGenomeStatistic(header)
        SAMRecordIterator iterator = samFileReader.iterator()
        genomeStatistic.preProcess()
        while (iterator.hasNext()) {
            SAMRecord record = iterator.next()
            String chromosomeName = record.getReferenceName()
            genomeStatistic.process(chromosomeName, record)
        }
        genomeStatistic.postProcess()
        return genomeStatistic
    }

    private GenomeStatistic<SAMRecord> initGenomeStatistic(SAMFileHeader header) {
        List<SAMSequenceRecord> seqRecords = header.getSequenceDictionary().getSequences()
        List<ChromosomeDto> chromosomeDtos = []
        seqRecords.each { SAMSequenceRecord seqRecord ->
            ChromosomeDto dto = new ChromosomeDto(chromosomeName: seqRecord.getSequenceName(), chromosomeLength: seqRecord.getSequenceLength() as long)
            chromosomeDtos.add(dto)
        }
        return this.factory.create(chromosomeDtos, this.parameters, this.fileParameters)
    }
}
