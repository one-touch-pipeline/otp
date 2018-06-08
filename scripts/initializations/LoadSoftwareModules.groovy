import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")

processingOptionService.createOrUpdate(COMMAND_LOAD_MODULE_LOADER, "")

// when updating fastqc, make sure that FastqcDataFilesService.fastqcFileNameWithoutZipSuffix is still correct
processingOptionService.createOrUpdate(COMMAND_FASTQC, "fastqc")
processingOptionService.createOrUpdate(COMMAND_ACTIVATION_FASTQC, "module load fastqc/0.11.5")

processingOptionService.createOrUpdate(COMMAND_GROOVY, 'groovy')
processingOptionService.createOrUpdate(COMMAND_ACTIVATION_GROOVY, 'module load groovy/2.4.15')

processingOptionService.createOrUpdate(COMMAND_SAMTOOLS, 'samtools')
processingOptionService.createOrUpdate(COMMAND_ACTIVATION_SAMTOOLS, 'module load samtools/1.2')
