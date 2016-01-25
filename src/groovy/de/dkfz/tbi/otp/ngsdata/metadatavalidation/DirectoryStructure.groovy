package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple

interface DirectoryStructure {

    String getDescription()

    /**
     * The titles of the columns which the paths are constructed from
     */
    List<String> getColumnTitles()

    /**
     * @return The path of the data file or {@code null} if it cannot be constructed
     */
    File getDataFilePath(MetadataValidationContext context, ValueTuple valueTuple)
}
