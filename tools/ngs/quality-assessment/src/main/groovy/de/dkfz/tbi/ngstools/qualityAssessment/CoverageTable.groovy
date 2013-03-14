package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.SAMRecord
import net.sf.samtools.SAMSequenceRecord

/**
 * This class holds multiple tables used to plot genome coverage
 */
class CoverageTable {

    private final Map<String, int[]> table = new LinkedHashMap<String, int[]>()
    private int winSize

    public CoverageTable(int winSize, List<SAMSequenceRecord> chrList){
        this.winSize = winSize
        for (SAMSequenceRecord chr:chrList){
            int arraySize = Math.ceil(chr.getSequenceLength() / winSize)
            table.put(chr.getSequenceName(), new int[arraySize])
        }
    }

    public void increaseCoverageCount(SAMRecord rec) {
        String key = rec.getReferenceName()
        int[] arr = table.get(key)
        if (arr == null) {
            System.err.println("BAM header do not contain inf. on the chromosome sizes - FATAL ERROR")
            System.exit(1)
        }
        int beginning = rec.getAlignmentStart()
        int locBin = Math.floor(beginning / winSize)
        arr[locBin]++
    }

    /**
     * Export results to tabulated string
     * @return
     */
    public String toTrimedTab() {
        StringBuffer sb = new StringBuffer()
        for (Map.Entry<String, int[]> entry : table.entrySet()) {
            String key = entry.getKey()
            int[] data = entry.getValue()
            for(int i = 0; i < data.size(); i++){
                int loc = i * winSize
                int value = data[i]
                sb.append(key)
                sb.append("\t")
                sb.append(loc)
                sb.append("\t")
                sb.append(value)
                sb.append("\n")
            }
        }
        return sb.toString()
    }
}
