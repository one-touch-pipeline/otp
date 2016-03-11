package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import groovy.text.SimpleTemplateEngine

class IlseNotificationService {

    ConfigService configService
    LsdfFilesService lsdfFilesService

    public static final String SEQ_CENTER_INBOX_PATH = 'STORAGE_ROOT/dmg/seq_center_inbox'
    public static final String META_DATA_FILE_REGEX = /.*_fastq.tsv$/

    public static final String ILSE_NUMBER_TEMPLATE = "000000"

    // could later be outsourced to ProcessingOptions to be able to change during run-time
    public static String NOTIFICATION_TEMPLATE = """
New run <%= run %> with <%= seqTypes %> is installed and is ready for the analysis.
Data are available in the directory:
<%= pathes %>

Samples:
<%= samples %>
"""

    /**
     * creates a notification message for all runs that belong to pased ILSe Ids
     * it is structured like it is defined in the NOTIFICATION_TEMPLATE
     */
    public String createIlseNotificationForIlseIds(List<String> ilseIds) {
        StringBuilder outputStringBuilder = new StringBuilder()

        assert !ilseIds.findAll().empty : "ILSe IDs not defined"

        Map dataPerRun = [:]

        ilseIds.findAll().each { String ilseId ->
            assert ilseId =~ /\d+/ : "An ILSe ID can just consist of numbers"

            File ilseFolder = getIlseFolder(ilseId)
            assert ilseFolder.exists() : "No Folder for ILSe ${ilseId} can be found"

            ilseFolder.eachFile { File runFolder ->
                File metaFile = findMetaFileInRunFolder(runFolder)

                Map<String, List> parsedMetaFile = parseMetaFileContentForProperties(metaFile.text, [samples: 'SAMPLE_ID', seqTypes: 'SEQUENCING_TYPE'])

                if(dataPerRun.containsKey(runFolder.name)) {
                    dataPerRun.get(runFolder.name).samples.addAll(parsedMetaFile.samples)
                    dataPerRun.get(runFolder.name).seqTypes.addAll(parsedMetaFile.seqTypes)
                } else {
                    dataPerRun.put(runFolder.name, parsedMetaFile)
                }
            }
        }

        dataPerRun.each { String run, Map properties ->
            List<Sample> samples = properties.get('samples').unique()
            List<SeqType> seqTypes = properties.get('seqTypes').unique()

            Project project = CollectionUtils.exactlyOneElement(SampleIdentifier.findAllByNameInList(samples)*.sample*.project.unique(),
                    "${run} contains samples of more than one project, what is currently not supported.")

            List<String> paths = getPathsToSeqTypesForRunAndProject(Run.findByName(run), project).collect {
                it.replace("//", "/").replace("STORAGE_ROOT/", "STORAGE_ROOT/")
            }.findAll()

            SimpleTemplateEngine engine = new SimpleTemplateEngine()

            outputStringBuilder.append(
                engine.createTemplate(NOTIFICATION_TEMPLATE).make(
                    [run: run,
                     seqTypes: seqTypes.join(", "),
                     pathes: paths.join("\n"),
                     samples: samples.join("\n")
                    ]
                ).toString()
            )
        }

        return outputStringBuilder.toString()
    }

    /**
     * Returns the absolute path to an ILSe Folder.
     * Ususally stored at STORAGE_ROOTSEQUENCING_INBOX/00[first digit of ILSe]/00[ILSe]
     */
    public File getIlseFolder(String ilseId) {
        String ilse = ILSE_NUMBER_TEMPLATE + ilseId
        return new File("${SEQ_CENTER_INBOX_PATH}/core/${ilse[-6..-1][0..2]}/${ilse[-6..-1]}")
    }

    /**
     * Finds the meta data file in a run folder
     */
    public File findMetaFileInRunFolder(File runFolder) {
        List<File> metaFiles = runFolder.listFiles().findAll() { File file ->
            file.name =~ META_DATA_FILE_REGEX
        }

        assert metaFiles.size() > 0 : "No meta-data file can be found under ${runFolder}"
        assert metaFiles.size() == 1 : "At least one other file exists under ${runFolder} that ends with 'fastq.tsv':\n${metaFiles.join('\n')}"

        return CollectionUtils.exactlyOneElement(metaFiles)
    }

    /**
     * Parses a meta data file for specific properties
     * properties = [property named for result map : property named in meta-data file]
     */
    public Map parseMetaFileContentForProperties(String metaFile, Map<String, String> properties) {
        assert metaFile != "" : "Meta-Data File is empty."

        Spreadsheet spreadSheet = new Spreadsheet(metaFile)

        Map propertyMap = [:]
        properties.each { String property, String metaFileProperty ->
            List<String> metaDataValues = spreadSheet.dataRows*.getCellByColumnTitle(metaFileProperty)*.text.findAll()

            assert metaDataValues : "Meta-Data File contains no information about ${property} with column-header ${metaFileProperty}"

            propertyMap << [(property): metaDataValues]
        }

        return propertyMap
    }

    /**
     * Returns all paths till the seqType folder for a specific run and project
     */
    public List<String> getPathsToSeqTypesForRunAndProject(Run run, Project project) {
        assert run
        assert project

        List<DataFile> dataFiles = DataFile.findAllByRunAndProject(run, project)
        assert dataFiles.size() > 0 : "No Data Files can be found for ${run} in ${project}"

        List<String> paths = dataFiles.collect { DataFile file ->
            String basePath = configService.getProjectSequencePath(file.project)
            String seqTypeDir = lsdfFilesService.seqTypeDirectory(file)
            return "${basePath}/${seqTypeDir}/"
        }

        return paths.unique().sort()
    }
}
