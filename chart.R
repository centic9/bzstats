library(parsedate)
data <- read.csv("stats.csv", stringsAsFactors=FALSE, sep=",", quote="")

library(reshape2)
datam <- melt(data, id.vars="Date", measure.vars=
	# when you change the order here also adjust it below for the legent
	c("Open.overall","Actual.bugs","Workable.bugs","Needinfo","Enhancements","Bugs.with.patch"))

library(ggplot2)

ggplot(datam, aes(x=parse_date(Date, approx = TRUE), y=value, colour=variable)) +
	# draw lines with dots and inner white-circles to get a "subway map"-like effect
    geom_line(size = 2) +
    geom_point(size = 3) +
    geom_point(size = 1.50, color = "white") +
    # add fitted regression lines
    geom_smooth(data=subset(datam, parse_date(Date, approx = TRUE) >= parse_date("2015-08-10 10:21")), 
                            method="lm", level=0.99, linetype="dashed") +
    # pin the y-axis at zero
    expand_limits(y=0) +
    # start after the change in how we compute the values, it's actually at 2015-08-10 10:21
    #xlim(parse_date("2015-08-19 00:00"), max(parse_date(datam$Date, approx = TRUE))) +
    # specify the label for both axes
    xlab("Date") +
    ylab("Number of issues") +
    # add more ticks on the y-axis
    scale_y_continuous(breaks=seq(0,1000,by=100)) +
    # set a title for the graph
    ggtitle("Open bugs in Apache POI") + 
    # set the default black/white theme
    theme_bw() +
    # legend styling
    theme(legend.position="right",
          legend.key = element_rect(fill = "white")) +
    scale_colour_discrete(labels=
		# this list needs to match the order used above!
		c("Open","Bugs","Workable Bugs","Needinfo","Enhancements","Bugs with Patch")) +
    guides(colour=guide_legend(title=NULL))

ggsave("stats.svg", width=210, height=148, units="mm")
