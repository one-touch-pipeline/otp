package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq

import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.*

trait DirectoryStructure {

    abstract String getDescription()

    /**
     * The titles of the columns which the paths are constructed from
     */
    abstract List<String> getColumnTitles()

    /**
     * @return The path of the data file or {@code null} if it cannot be constructed
     */
    abstract Path getDataFilePath(MetadataValidationContext context, ValueTuple valueTuple)

    /**
     * @return The path of the data file or {@code null} if it cannot be constructed
     */
    Path getDataFilePath(MetadataValidationContext context, Row row) {
        Map<String, String> valuesByColumnTitle = [:]
        Set<Cell> cells = new LinkedHashSet<Cell>()
        getColumnTitles().each {
            Cell cell = row.getCellByColumnTitle(it)
            if (cell) {
                valuesByColumnTitle.put(it, cell.text)
                cells.add(cell)
            }
        }
        return getDataFilePath(context, new ValueTuple(valuesByColumnTitle.asImmutable(), cells.asImmutable()))
    }

    private FileSystem fileSystem

    void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem
    }

    FileSystem getFileSystem() {
        return fileSystem ?: FileSystems.getDefault()
    }
}
