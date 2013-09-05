package de.dkfz.tbi.ngstools.coverage_plot_mfs

import static org.junit.Assert.*
import org.junit.*

/**
 * Checks for processing methods
 *
 *
 */
class CoveragePlotMFSTests {

    CoveragePlotMFS coveragePlotMFS

    @Before
    public void setUp() throws Exception {
        coveragePlotMFS = new CoveragePlotMFS()
    }

    @After
    public void tearDown() throws Exception {
        coveragePlotMFS = null
    }

    @Test
    public void testLoadJsonMFS() {
        //create tmp file with data for load
        File f = File.createTempFile("otp-coverage-plot-mfs-json", ".json")
        f.deleteOnExit()
        StringBuilder sb = new StringBuilder(10000)
        sb << '{"chromosomeIdentifierMap":{"chr1":"1","chr2":"2","chr3":"3","chr4":"4","chr5":"5",'
        sb << '"chr6":"6","chr7":"7","chr8":"8","chr9":"9","chr10":"10",'
        sb << '"chr11":"11","chr12":"12","chr13":"13","chr14":"14","chr15":"15",'
        sb << '"chr16":"16","chr17":"17","chr18":"18","chr19":"19","chr20":"20",'
        sb << '"chr21":"21","chr22":"22","chrX":"X","chrY":"Y","chrM":"M","chr*":"*"},'
        sb << '"filterChromosomes":["M","*"],'
        sb << '"sortedChromosomeIdentifiers":["1","2","3","4","5","6","7","8","9","10",'
        sb << '"11","12","13","14","15","16","17","18","19","20","21","22","X","Y","M","*"]}'
        f << sb.toString()
        //set file to parameter
        coveragePlotMFS.fileParameters.formatingJsonFile = f.toString()
        //prepare structure to check against
        Map dataExp = [
            "chromosomeIdentifierMap": [
                "chr1": "1", "chr2": "2", "chr3": "3", "chr4": "4", "chr5": "5",
                "chr6": "6", "chr7": "7", "chr8": "8", "chr9": "9", "chr10": "10",
                "chr11": "11", "chr12": "12", "chr13": "13", "chr14": "14", "chr15": "15",
                "chr16": "16", "chr17": "17", "chr18": "18", "chr19": "19", "chr20": "20",
                "chr21": "21", "chr22": "22", "chrX": "X", "chrY": "Y", "chrM": "M", "chr*": "*"],
            "filterChromosomes":  ["M", "*"],
            "sortedChromosomeIdentifiers": ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
                "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "Y", "M", "*"]
        ]
        //do call and check
        Map<String, List<String>> dataMapped = coveragePlotMFS.readJsonFile()
        f.delete()
        assertEquals(dataExp, dataMapped)
    }

    @Test
    public void testLoadCoverageDataFile() {
        //create tmp file with data for load
        File f = File.createTempFile("otp-coverage-plot-mfs", ".bam")
        f.deleteOnExit()
        StringBuilder sb = new StringBuilder(10000)
        [1..5, "X", "Y"].flatten().each { chr ->
            [1..5].flatten().each { win -> sb << "${chr}\t${win}000\t0\n" }
        }
        f << sb.toString()
        //set file to parameter
        coveragePlotMFS.fileParameters.coverageDataFile = f.toString()
        //prepare structure to check against
        List<String> winSize = [
            "1000\t0",
            "2000\t0",
            "3000\t0",
            "4000\t0",
            "5000\t0"
        ]
        Map<String, List<String>> dataExp = ["1": winSize, "2": winSize, "3": winSize, "4": winSize, "5": winSize, "X": winSize, "Y": winSize]
        //do call and check
        Map<String, List<String>> dataMapped = coveragePlotMFS.load()
        f.delete()
        assertEquals(dataExp, dataMapped)
    }

    @Test
    public void testMap() {
        Map<String, String> map = ["chr1": "1", "chr2": "2", "chr3": "3", "chrX": "X", "chrY": "Y", "chrM": "M", "chr*": "*"]
        Map<String, List<String>> dataOrg = ["chr1": [], "chr2": [], "chr3": [], "chrX": [], "chrY": [], "chrM": [], "chr*": []]
        Map<String, List<String>> dataExp = ["1": [], "2": [], "3": [], "X": [], "Y": [], "M": [], "*": []]
        Map<String, List<String>> dataMapped = coveragePlotMFS.map(dataOrg, map)
        assertEquals(dataExp, dataMapped)
    }

    @Test
    public void testFilter() {
        List<String> filter = ["1", "2", "3", "X", "Y"]
        Map<String, List<String>> dataOrg = ["1": [], "2": [], "3": [], "X": [], "Y": [], "M": [], "*": []]
        Map<String, List<String>> dataExp = ["1": [], "2": [], "3": [], "X": [], "Y": []]
        Map<String, List<String>> dataFiltered = coveragePlotMFS.filter(dataOrg, filter)
        assertEquals(dataExp, dataFiltered)

        dataOrg = ["1": [], "2": [], "M": [], "*": [], "3": [], "X": [], "Y": []]
        dataExp = ["1": [], "2": [], "3": [], "X": [], "Y": []]
        dataFiltered = coveragePlotMFS.filter(dataOrg, filter)
        assertEquals(dataExp, dataFiltered)
    }

    @Test
    public void testSort() {
        List<String> sorted = [
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "X",
            "Y",
            "M",
            "*"
        ]
        Map<String, List<String>> dataOrg = ["Y": [1], "8": [1], "*": [1], "5": [1], "2": [1], "1": [1], "X": [1], "M": [1], "3": [1]]
        Map<String, List<String>> dataExp = ["1": [1], "2": [1], "3": [1], "5": [1], "8": [1], "X": [1], "Y": [1], "M": [1], "*": [1]]
        Map<String, List<String>> dataSorted = coveragePlotMFS.sort(dataOrg, sorted)
        assertEquals(dataExp, dataSorted)
        //the order of the keys are checked explicit, because maps ignores the order during checking
        assertEquals(dataExp.keySet().asList(), dataSorted.keySet().asList())
    }

    @Test
    public void testWrite() {
        //create tmp file with data for load
        File f = File.createTempFile("otp-coverage-plot-mfs", ".bam")
        f.deleteOnExit()
        //set file to parameter
        coveragePlotMFS.fileParameters.generatedCoverageDataFile = f.toString()
        //prepare data to save to check against
        List<String> winSize = [
            "1000\t0",
            "2000\t0",
            "3000\t0",
            "4000\t0",
            "5000\t0"
        ]
        Map<String, List<String>> dataOrg = ["1": winSize, "2": winSize, "3": winSize, "4": winSize, "5": winSize, "X": winSize, "Y": winSize]

        //create expected content
        StringBuilder expected = new StringBuilder(10000)
        [1..5, "X", "Y"].flatten().each { chr ->
            [1..5].flatten().each { win -> expected << "${chr}\t${win}000\t0\n" }
        }

        //do call and check
        coveragePlotMFS.writeCsvFile(dataOrg)
        String fileContent = f.getText()
        f.delete()
        assertEquals(expected.toString(), fileContent)
    }
}
