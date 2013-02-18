# Creates a coverage plot for two samples (control and tumor)

# Getting all args because it is necessary to get the path to the executing file to be able to source an additional library, all arguments are considered
cmdArgs = commandArgs(trailingOnly = F)
fileArgument = "--file="
scriptPath = dirname(sub(fileArgument, "", cmdArgs[grep(fileArgument, cmdArgs)]))
# Includes required functions
source(paste(scriptPath, "coveragePlotGenomeLib.R", sep = "/"))

numberOfParamsExpected = 4
# Index needed to only consider the arguments after "--args"
beginArgsIndexes = which(cmdArgs == "--args")
numberOfParams = length(cmdArgs) - beginArgsIndexes
if (numberOfParams != numberOfParamsExpected) {
    errorMsg = paste("Incorrect number of arguments (", numberOfParamsExpected," expected): ", numberOfParams)
    exampleMsg = paste("Required arguments: pathToControlData pathToTumorData countType pathToOutputPlot.png",
    "",
    "Example: ",
    "Rscript coveragePlotControlVsTumor.R \"./control_PID_merged.bam.rmdup_readCoverage_1kb_windows.txt\" \"./tumor_PID_merged.bam.rmdup_readCoverage_1kb_windows.txt\" \"read\" \"./control_tumor_comparison.png\"",
    sep = "\n")
    fullMsg = cat(errorMsg, exampleMsg, sep = "\n")
    print(fullMsg)
    q()
}

filenameOne = cmdArgs[beginArgsIndexes + 1]
filenameTwo = cmdArgs[beginArgsIndexes + 2]
countType = cmdArgs[beginArgsIndexes + 3]
outputPath = cmdArgs[beginArgsIndexes + 4]

sampleOneName = basename(filenameOne)
sampleTwoName = basename(filenameTwo)

#input coverage data
sampleOneRaw = read.table(filenameOne, sep = "\t", as.is = TRUE)
sampleOneSplit = splitsByChromosomeAndNameColumns(sampleOneRaw)

sampleTwoRaw = read.table(filenameTwo, sep = "\t", as.is = TRUE)
sampleTwoSplit = splitsByChromosomeAndNameColumns(sampleTwoRaw)

# validating the window size
windowSizeSampleOne = sampleOneRaw[2,2] - sampleOneRaw[1,2]
windowSizeSampleTwo = sampleTwoRaw[2,2] - sampleTwoRaw[1,2]
if (windowSizeSampleOne != windowSizeSampleTwo) {
	stop("The window size from the samples is different")
}
windowSize = windowSizeSampleOne

# Start PNG device driver to save output to figure.png
png(outputPath, width = 2100, height = 1000)
# create a grid (setting number of plots (3 X 1 = 3) and margins)
par(mfrow = c(3, 1), mar = c(2, 4, 2, 2))

# plot both samples
yValue = 17
plotChromosomes(sampleOneName, sampleOneRaw, yValue)
drawPlotGridAndLabels(sampleOneSplit, yValue)
plotChromosomes(sampleTwoName, sampleTwoRaw, yValue)
drawPlotGridAndLabels(sampleTwoSplit, yValue)

#plot for comparison of samples (Ratio between the 2 samples normalized to #reads all chromosomes)
#calculate total number of reads for normalization
sumSampleOne = sum(sapply(sampleOneSplit, function(x) sum(x$reads)))
sumSampleTwo = sum(sapply(sampleTwoSplit, function(x) sum(x$reads)))
basis = 2

plot(log(((sampleTwoRaw[, 3]) / sumSampleTwo) / ((sampleOneRaw[, 3]) / sumSampleOne), basis), pch = ".", main = paste("log", basis, " ratio ", "tumor", " vs. ", "control genomewide", sep=""), cex.main = 1.5, xaxs = "i", ylab = paste("#", countType, " per ", windowSize, "kb normalized to total ", countType, sep=""), cex.lab = 1.5, axes = FALSE)

box()
axis(2, cex.axis = 1.5)
abline(h = 0, col = 2)
abline(v = 0, col = "grey3")
drawPlotGridAndLabels(sampleTwoSplit, 3)
graphics.off()
