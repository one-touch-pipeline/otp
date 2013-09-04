
# base of the logarithm function to be used
logarithmBase = 2

# Conversion function for coverage data
# It just gives names to the columns and splits them in several per chromosome tables
splitsByChromosomeAndNameColumns <- function(x) {
    x = data.frame(ref = x[,1], start = x[,2], reads = x[,3])
    x = split(x, x$ref)
    return(x)
}

# Retrieves the maximum read count value from all chromossomes
maximumReadCountFromAllChromosomes <- function(sampleSplitByChromosome) {
    maxForAllChromosomes = 0
    maxChr = 0
    chrNames <- names(sampleSplitByChromosome)
    for (chr in chrNames) {
        maxChr = max(log(sampleSplitByChromosome[[chr]][,3] + 1,  logarithmBase))
        if (maxForAllChromosomes < maxChr) {
            maxForAllChromosomes = maxChr
        }
    }
    return(maxForAllChromosomes)
}

# Draw the grid and labels of Chromosomes at the plot
drawPlotGridAndLabels <- function(chromosomesOrdered, sampleSplitByChromosome) {
    chromosomes = names(sampleSplitByChromosome)
    # the chromosome label plot will be positioned at 95% of the space available
    yValue = maximumReadCountFromAllChromosomes(sampleSplitByChromosome) * 0.95
    # To separate each chromosome by a grey line
    chrLengthSum = 0
    for (i in chromosomesOrdered) {
        text(x = chrLengthSum + round((nrow(sampleSplitByChromosome[[i]]) / 2)), y = yValue, labels = i, pos = 3, cex = 1.2)
        chrLengthSum = chrLengthSum + nrow(sampleSplitByChromosome[[i]])
        abline(v = chrLengthSum, col = "grey3")
    }
    axis(1, at = pretty(c(0, chrLengthSum)), labels = paste(pretty(c(0, chrLengthSum / 1000)), "Mb", sep=""), cex.axis = 1.5)
}

# Plots the chromosome data (sample has to be ordered by chromosome and window size)
plotChromosomes <- function(sampleName, sampleOrdered) {
    # Calculating log of base (adding 1 to number of reads to avoid '-INF' )
    logOfSampleOrdered = log(sampleOrdered[, 3] + 1, logarithmBase)
    plot(logOfSampleOrdered, pch = ".", main = paste(sampleName, sep = "  "), cex.main = 1.5, xaxs = "i", ylab = paste("log", logarithmBase, " #", countType, " per ", windowSize/1000, "kb", sep = ""), cex.lab = 1.5, axes = FALSE)
    box()
    # right axis
    axis(2, cex.axis = 1.5)
    # red line at the mean coverage level
    abline(h = log(mean(sampleOrdered[, 3]) + 1, logarithmBase), col = 2)
    abline(v = 0, col = "grey3")
}
