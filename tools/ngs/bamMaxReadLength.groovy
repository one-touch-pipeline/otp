/**
 * Find the maximum read length contained in a BAM file
 *
 * Usage: samtools view example.bam | groovy bamMaxReadLength.groovy
 *    or: cat example.bam | samtools view - | groovy bamMaxReadLength.groovy
 *
 * @see <a href="https://samtools.github.io/hts-specs/SAMv1.pdf">Sequence Alignment/Map Format Specification</a>
 */
class MaxReadLength {
    static int calculateMaxReadLength(BufferedReader reader) {
        int maxLength = 0

        String line
        while ((line = reader.readLine()) != null) {
            List<String> fields = line.tokenize("\t")
            String cigarField = fields[5]
            if (cigarField == "*") {
                continue
            }
            List<Integer> lengths = cigarField.findAll(/(\d+)[MINSHP=X]/, { match, length ->
                length as Integer
            })
            Integer length = lengths.sum() as Integer

            if (maxLength < length) {
                maxLength = length
            }
        }
        return maxLength
    }

    static void main(String... args) {
        InputStreamReader stdinReader = new InputStreamReader(System.in)
        BufferedReader reader = new BufferedReader(stdinReader)

        try {
            int maxLength = calculateMaxReadLength(reader)
            println maxLength
        } catch (Throwable t) {
            t.printStackTrace(System.err)
            System.exit(1)
        } finally {
            reader.close()
            stdinReader.close()
        }
    }
}
