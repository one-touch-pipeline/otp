import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.notification.*


ProcessingOptionService processingOptionService = ctx.processingOptionService


println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateBase,
        null,
        null,
        '''
Dear user,

${stepInformation}${seqCenterComment}

If you want to get access to OTP, please contact the DMG service team.

Best regards,
OTP

''',
        '',
)


println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateInstallation,
        null,
        null,
'''The FASTQ files of run(s) ${runs} have been installed.
They are available here:
${paths}

Samples:
${samples}

An overview of all processed samples is shown on
${links}
''',
        '',
)


println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateInstallationFurtherProcessing,
        null,
        null,
'''The samples marked with [A] will be aligned by OTP. As soon as the alignment is finished, OTP will send you another notification e-mail.
''',
        '',
)


println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateAlignment,
        null,
        null,
'''The FASTQ files of the following samples have been aligned and merged:
${samples}

You can check their quality here:
${links}

The BAM files were produced with the following configuration(s):
${processingValues}

You can find the merged BAM files in the "view-by-pid" folder:
${paths}
''',
        '',
)


println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateAlignmentFurtherProcessing,
        null,
        null,
        '''For the following sample pairs variants will be called by OTP:
${samplePairsWillProcess}
OTP executes the following variant calling pipeline(s): ${variantCallingPipelines}
As soon as a variant calling is finished, OTP will send you another notification e-mail.
''',
        '',
)


println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateAlignmentNoFurtherProcessing,
        null,
        null,
'''The following sample pairs will not been variant-called, most likely because they have not reached a threshold yet:
${samplePairsWontProcess}
If you want these sample pairs to be processed nevertheless, please contact the DMG service team.
''',
        '',
)


println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateAlignmentProcessing,
        null,
        null,
        '''${seqType} ${individuals ? "("+individuals+")" : ""}
    Reference genome: ${referenceGenome}
    Alignment program: ${alignmentProgram} ${alignmentParameter}
    Merging/duplication marking program: ${mergingProgram} ${mergingParameter}
    SAMtools program: ${samtoolsProgram}
''',
        '',
)


println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateSNVProcessed,
        null,
        null,
'''The SNV calling for following sample pairs is finished:
${samplePairsFinished}

For the SNV calling you can find links to the plots here:
${otpLinks}

The result files are available in the directories:
${directories}

You can find the config file used for the processing in the same directory as the results.
''',
        '',
)


println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateINDELProcessed,
        null,
        null,
'''The Indel calling for following sample pairs is finished:
${samplePairsFinished}

For the Indel calling you can find links to the plots here:
${otpLinks}

The result files are available in the directories:
${directories}

You can find the config file used for the processing in the same directory as the results.
''',
        '',
)

println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateACESEQProcessed,
        null,
        null,
        '''The CNV calling from AceSEQ for following sample pairs is finished:
${samplePairsFinished}

For the CNV calling from AceSEQ you can find links to the plots here:
${otpLinks}

The result files are available in the directories:
${directories}

You can find the config file used for the processing in the same directory as the results.
''',
        '',
)

println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateSOPHIAProcessed,
        null,
        null,
        '''The SV calling from SOPHIA for following sample pairs is finished:
${samplePairsFinished}

For the SV calling from SOPHIA you can find links to the plots here:
${otpLinks}

The result files are available in the directories:
${directories}

You can find the config file used for the processing in the same directory as the results.
''',
        '',
)

println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateSNVNotProcessed,
        null,
        null,
'''The following sample pairs have not been SNV-called, most likely because they have not reached a threshold yet:
${samplePairsNotProcessed}
If you want these sample pairs to be processed nevertheless, please contact the DMG service team.
''',
        '',
)


println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateINDELNotProcessed,
        null,
        null,
'''The following sample pairs have not been Indel-called, most likely because they have not reached a threshold yet:
${samplePairsNotProcessed}
If you want these sample pairs to be processed nevertheless, please contact the DMG service team.
''',
        '',
)

println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateACESEQNotProcessed,
        null,
        null,
        '''The following sample pairs have not been CNV-called, most likely because they have not reached a threshold yet:
${samplePairsNotProcessed}
If you want these sample pairs to be processed nevertheless, please contact the DMG service team.
''',
        '',
)

println processingOptionService.createOrUpdate(
        OptionName.notificationTemplateSOPHIANotProcessed,
        null,
        null,
        '''The following sample pairs have not been SV-called, most likely because they have not reached a threshold yet:
${samplePairsNotProcessed}
If you want these sample pairs to be processed nevertheless, please contact the DMG service team.
''',
        '',
)