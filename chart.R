library(parsedate)
data <- read.csv("stats.csv", stringsAsFactors=FALSE, sep=",", quote="")
plot(parse_date(data$Date, approx = TRUE), data$Open.overall, type="l", 
    ylim=c(0, 600), panel.first = grid(18, 18), xlab="Date", ylab="Issue Counts")
lines(parse_date(data$Date, approx = TRUE), data$Enhancements, col="red")
lines(parse_date(data$Date, approx = TRUE), data$Actual.bugs, col="blue")
lines(parse_date(data$Date, approx = TRUE), data$Needinfo, col="green")
lines(parse_date(data$Date, approx = TRUE), data$Workable.bugs, col="orange")
lines(parse_date(data$Date, approx = TRUE), data$Bugs.with.patch, col="brown")

