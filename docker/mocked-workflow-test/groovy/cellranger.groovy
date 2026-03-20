/*
 * Copyright 2011-2026 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.nio.file.*

class CellRanger {

    static final String INPUT_DIRECTORY_NAME = 'cell-ranger-input'

    static final String OUTPUT_DIRECTORY_NAME = 'outs'

    static final String ANALYSIS_DIRECTORY_NAME = 'analysis'

    static final String ORIGINAL_BAM_FILE_NAME = 'possorted_genome_bam.bam'

    static final String ORIGINAL_BAI_FILE_NAME = 'possorted_genome_bam.bam.bai'

    // is created manually
    static final String ORIGINAL_BAM_MD5SUM_FILE_NAME = 'possorted_genome_bam.md5sum'

    static final String METRICS_SUMMARY_CSV_FILE_NAME = 'metrics_summary.csv'

    static final String WEB_SUMMARY_HTML_FILE_NAME = 'web_summary.html'

    static final String CELL_RANGER_COMMAND_FILE_NAME = 'cell_ranger_command.txt'

    static final List<String> CREATED_RESULT_FILES = [
            WEB_SUMMARY_HTML_FILE_NAME,
            METRICS_SUMMARY_CSV_FILE_NAME,
            ORIGINAL_BAM_FILE_NAME,
            ORIGINAL_BAI_FILE_NAME,
            ORIGINAL_BAM_MD5SUM_FILE_NAME,
            'filtered_feature_bc_matrix.h5',
            'raw_feature_bc_matrix.h5',
            'molecule_info.h5',
            'cloupe.cloupe',
    ].asImmutable()

    static final List<String> CREATED_RESULT_DIRS = [
            'filtered_feature_bc_matrix',
            'raw_feature_bc_matrix',
            ANALYSIS_DIRECTORY_NAME,
    ].asImmutable()

    static void main(String[] args) {
        if (!args) {
            System.err.println("No args")
            System.exit(1)
        }
        assert args.length in [9, 10]

        assert args[0] == 'count'

        String id = assertParamWithEqual(args[1], '--id', { it.isInteger() })
        String path = assertParamWithEqual(args[2], '--fastqs', { it && Paths.get(it).isAbsolute() })
        assertParamWithEqual(args[3], '--transcriptome', { it && Paths.get(it).isAbsolute() })
        assertParamWithEqual(args[4], '--sample', { it })
        assertParamWithEqual(args[5], '--localcores', { it.isInteger() })
        assertParamWithEqual(args[6], '--localmem', { it.isInteger() })
        assertParamWithEqual(args[7], '--create-bam', { it in ['true', 'false'] })

        if (args.length == 10) {
            String param8 = args[8]
            if (param8.contains('--force-cells')) {
                assertParamWithEqual(param8, '--force-cells', { it.isInteger() })
            } else if (param8.contains('--expect-cells')) {
                assertParamWithEqual(param8, '--expect-cells', { it.isInteger() })
            } else {
                throw new IllegalArgumentException("Unexpected parameter: '${param8}', expects either '--force-cells' or '--expect-cells'")
            }
        }

        assert args[args.length - 1] == '--disable-ui'

        createOutput(id)
    }

    static String assertParamWithEqual(String param, String paramName, Closure<String> valueClosure = { true }) {
        assert param
        String[] split = param.split('=')
        assert split.length == 2
        assert split[0] == paramName
        String value = split[1]
        assert value
        assert valueClosure(value)
        return value
    }

    static void createOutput(String id) {
        Path currentDirectory = Paths.get(System.getProperty("user.dir"))
        Path outputDirectory = currentDirectory.resolve(id).resolve(OUTPUT_DIRECTORY_NAME)
        CREATED_RESULT_DIRS.each {
            Path directory = Files.createDirectories(outputDirectory.resolve(it))
            Files.writeString(directory.resolve("tmpFile"), "This is a tmp file for directory ${it}".toString())
        }
        CREATED_RESULT_FILES.each {
            Path file = Files.createFile(outputDirectory.resolve(it))
            String content = it == METRICS_SUMMARY_CSV_FILE_NAME ? getQaFileContent() : "This is a mocked file for ${it}".toString()
            Files.writeString(file, content)
        }
    }

    static String getQaFileContent() {
        Map<String, Object> csvData = [
                "Estimated Number of Cells"                     : "1234",
                "Mean Reads per Cell"                           : "123,456",
                "Median Genes per Cell"                         : "1234",
                "Number of Reads"                               : "123,456,789",
                "Valid Barcodes"                                : "12.3%",
                "Sequencing Saturation"                         : "45.6%",
                "Q30 Bases in Barcode"                          : "78.9%",
                "Q30 Bases in RNA Read"                         : "98.7%",
                "Q30 Bases in UMI"                              : "65.4%",
                "Reads Mapped to Genome"                        : "32.1%",
                "Reads Mapped Confidently to Genome"            : "11.1%",
                "Reads Mapped Confidently to Intergenic Regions": "2.2%",
                "Reads Mapped Confidently to Intronic Regions"  : "33.3%",
                "Reads Mapped Confidently to Exonic Regions"    : "44.4%",
                "Reads Mapped Confidently to Transcriptome"     : "55.5%",
                "Reads Mapped Antisense to Gene"                : "6.6%",
                "Fraction Reads in Cells"                       : "77.7%",
                "Total Genes Detected"                          : "54321",
                "Median UMI Counts per Cell"                    : "12345",
        ]
        return [
                csvData.keySet().join(","),
                csvData.values().collect { "\"${it}\"" }.join(","),
        ].join("\n")
    }
}
