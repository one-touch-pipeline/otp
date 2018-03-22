import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")

processingOptionService.createOrUpdate(COMMAND_LOAD_MODULE_LOADER, "")

// when updating fastqc, make sure that FastqcDataFilesService.fastqcFileNameWithoutZipSuffix is still correct
processingOptionService.createOrUpdate(COMMAND_FASTQC, "fastqc")
processingOptionService.createOrUpdate(COMMAND_ACTIVATION_FASTQC, "module load fastqc/0.11.5")
