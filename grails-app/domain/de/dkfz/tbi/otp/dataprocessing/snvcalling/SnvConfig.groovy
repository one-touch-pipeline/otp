package de.dkfz.tbi.otp.dataprocessing.snvcalling

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.otp.dataprocessing.ConfigPerProject
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.BashScriptVariableReader
import de.dkfz.tbi.otp.utils.ExternalScript

class SnvConfig extends ConfigPerProject {

    static final String CHROMOSOME_COUNT_VARIABLE_NAME = 'OTP_CHROMOSOME_COUNT'
    static final String CHROMOSOME_NAMES_VARIABLE_NAME = 'OTP_CHROMOSOME_NAMES'


    /**
     * In this String the complete content of the config file is stored.
     * This solution was chosen to be as flexible as possible in case the style of the config file changes.
     */
    String configuration

    /**
     * Defines which version of the external scripts has to be used for this project.
     */
    String externalScriptVersion

    transient Map<String, String> variables

    static transients = [ 'variables' ]

    static belongsTo = [
            seqType: SeqType
    ]

    static constraints = {
        configuration blank: false
        externalScriptVersion blank: false, validator: { val, obj ->
            if (obj.obsoleteDate) {
                !ExternalScript.findAllByScriptVersion(val).empty
            } else {
                !ExternalScript.findAllByScriptVersionAndDeprecatedDate(val, null).empty
            }
        }
        seqType unique: ['project', 'obsoleteDate']  // partial index: WHERE obsolete_date IS NULL
    }

    static mapping = {
        configuration type: 'text'
    }

    SnvConfig evaluate() {
        evaluateWithoutConsistencyChecking()
        ensureExecuteFlagsAreConsistent()
        if (getChromosomeNames().size() < 1) {
            throw new RuntimeException('Illegal config. No chromosomes specified.')
        }
        return this
    }

    private SnvConfig evaluateWithoutConsistencyChecking() {
        final Set<String> variableNames = new HashSet<String>()
        variableNames.addAll(SnvCallingStep.values().collect { it.configExecuteFlagVariableName })
        variableNames.add(CHROMOSOME_COUNT_VARIABLE_NAME)
        variableNames.add(CHROMOSOME_NAMES_VARIABLE_NAME)
        variables = BashScriptVariableReader.executeAndGetVariableValues(
"""${configuration}
OTP_EXIT_CODE=\$?
if [ \$OTP_EXIT_CODE -ne 0 ]; then exit \$OTP_EXIT_CODE; fi
${CHROMOSOME_COUNT_VARIABLE_NAME}=\${#CHROMOSOME_INDICES[@]}
${CHROMOSOME_NAMES_VARIABLE_NAME}=\${CHROMOSOME_INDICES[@]}
""", variableNames)
        return this
    }

    /**
     * Ensures that if the execute flag for a step is set, the execute flags for all subsequent steps are also set.
     *
     * <p>
     * The result of interest is always the one from the last step (i.e. {@link SnvCallingStep#FILTER_VCF}), so it makes
     * no sense to run earlier steps, but not later steps.
     *
     * <p>
     * On the other hand, later steps can use the output if earlier steps <em>from an earlier
     * {@link SnvCallingInstance}</em>, so in some situations it makes sense to have the execute flag of early steps to 0.
     *
     * <p>
     * Due to implementation details, the Deep Annotation step can not be run without doing the normal Annotation first.
     * This because the Annotation step intermediate results are not stored.
     */
    private void ensureExecuteFlagsAreConsistent() {
        // sanity check 1: once we do a step, (re)do all steps following it
        boolean anyExecuteFlagSet = false
        SnvCallingStep.values().each {
            final boolean flag = getExecuteStepFlag(it)
            if (anyExecuteFlagSet && !flag) {
                throw new RuntimeException("Illegal config. ${it} is configured not to be executed, but a previous step is.")
            }
            if (flag) {
                anyExecuteFlagSet = true
            }
        }

        // sanity check 2: doing deep-annotation requires doing annotation first,
        // to prepare the (transient) annotation-results to deep-annotate.
        if (getExecuteStepFlag(SnvCallingStep.SNV_DEEPANNOTATION) && !getExecuteStepFlag(SnvCallingStep.SNV_ANNOTATION)) {
            throw new RuntimeException("Illegal config, trying to do DeepAnnotation without the required Annotation step.")
        }
    }

    List<String> getChromosomeNames() {
        final int chromosomeCount = Integer.parseInt(getVariableValue(CHROMOSOME_COUNT_VARIABLE_NAME, { value -> value ==~ /^[0-9]{1,2}$/ }))
        final String serializedChromosomeNames = getVariableValue(CHROMOSOME_NAMES_VARIABLE_NAME)
        if (chromosomeCount == 0 && serializedChromosomeNames.empty) {
            return []
        } else {
            final String[] chromosomeNames = serializedChromosomeNames.split(' ')
            assert chromosomeNames.length == chromosomeCount
            return Arrays.asList(chromosomeNames)
        }
    }

    boolean getExecuteStepFlag(final SnvCallingStep step) {
        return getVariableValue(step.configExecuteFlagVariableName, { value -> ['0', '1'].contains(value) }) == '1'
    }

    private String getVariableValue(final String variableName, final Closure validator = null) {
        if (variables == null) {
            throw new IllegalStateException('Must call the evaluate() method first.')
        }
        final String value = variables.get(variableName)
        if (validator != null) {
            final boolean valid
            try {
                valid = validator(value)
            } catch (final Throwable t) {
                throw new RuntimeException("Validation of variable ${variableName} failed for value: ${value}", t)
            }
            if (!valid) {
                throw new RuntimeException("Illegal value for variable ${variableName}: ${value}")
            }
        }
        return value
    }

    /**
     * Creates an SnvConfig for the specified project, seqType which holds
     * the content of the configFile in the property configuration
     *
     * This method has to be called in the groovy console:
     * <pre>
     * import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
     * import de.dkfz.tbi.otp.ngsdata.*
     *
     * Project project = Project.findByName("projectName")
     * SeqType seqType = SeqType.findByNameAndLibraryLayout("SeqTypeName", "PAIRED")
     * File configFile = new File("PathToConfigFile")
     *
     * SnvConfig.createFromFile(project, seqType, configFile)
     * </pre>
     */
    static SnvConfig createFromFile(Project project, SeqType seqType, File configFile, String externalScriptVersion) {
        SnvConfig snvConfig = getLatest(project, seqType, true)
        final SnvConfig config = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: configFile.text,
                externalScriptVersion: externalScriptVersion,
                previousConfig: snvConfig,
        ).evaluate()
        config.createConfigPerProject()
        return config
    }

    static SnvConfig getLatest(final Project project, final SeqType seqType, boolean allowNone = false) {
        try {
            return singleElement(SnvConfig.findAllByProjectAndSeqTypeAndObsoleteDate(project, seqType, null), allowNone)
        } catch (final Throwable t) {
            throw new RuntimeException("Could not get latest SnvConfig for Project ${project} and SeqType ${seqType}. ${t.message ?: ''}", t)
        }
    }


    void writeToFile(final File file, final boolean overwriteIfExists = false) {
        assert file.isAbsolute()
        if (!overwriteIfExists && file.exists()) {
            throw new RuntimeException("overwriteIfExists is false and file already exists: ${file}")
        }
        file.text = configuration
    }
}
