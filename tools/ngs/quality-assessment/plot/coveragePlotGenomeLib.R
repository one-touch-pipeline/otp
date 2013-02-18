# Conversion function for coverage data
# It just gives names to the columns and splits them in several per chromosome tables
splitsByChromosomeAndNameColumns <- function(x) {
    x = data.frame(ref = x[,1], start = x[,2], reads = x[,3])
    x = split(x, x$ref)
    return(x)
}

# Draw the grid and labels of Chromosomes at the plot

drawPlotGridAndLabels <- function(sampleSplitByChromosome, yValue) {
    chromosomes = names(sampleSplitByChromosome)
    # To separate each chromosome by a grey line
    chrLengthSum = 0
    for (i in chromosomes) {
        text(x = chrLengthSum + round((nrow(sampleSplitByChromosome[[i]]) / 2)), y = yValue, labels = i, pos = 3, cex = 1.2)
        chrLengthSum = chrLengthSum + nrow(sampleSplitByChromosome[[i]])
        abline(v = chrLengthSum, col = "grey3")
    }
    axis(1, at = pretty(c(0, chrLengthSum)), labels = paste(pretty(c(0, chrLengthSum / 1000)), "Mb", sep=""), cex.axis = 1.5)
}

# Plots the chromosome data (sample has to be ordered by chromosome and window size)
plotChromosomes <- function(sampleName, sampleOrdered, yValue) {
    # ordering by chromossome (the raw data could be not ordered)
    # Calculating log of base 2 (adding 1 to number of reads to avoid '-INF' )
    basis = 2
    plot(log(sampleOrdered[, 3] + 1, basis), pch = ".", main = paste(sampleName, sep = "  "), cex.main = 1.5, xaxs = "i", ylab = paste("log", basis, " #", countType, " per ", windowSize/1000, "kb", sep = ""), cex.lab = 1.5, axes = FALSE)
    box()
    # right axis
    axis(2, cex.axis = 1.5)
    # red line at the mean coverage level
    abline(h = log(mean(sampleOrdered[, 3]) + 1, basis), col = 2)
    abline(v = 0, col = "grey3")
}
