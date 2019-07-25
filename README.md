
# Pie Chart for Status Distribution

https://bz.apache.org/bugzilla/report.cgi?z_axis_field=&format=pie&x_axis_field=bug_status&x_labels_vertical=1&no_redirect=1&query_format=report-graph&short_desc_type=allwordssubstr&short_desc=&product=POI&longdesc_type=allwordssubstr&longdesc=&bug_file_loc_type=allwordssubstr&bug_file_loc=&keywords_type=allwords&keywords=&bug_id=&bug_id_type=anyexact&votes=&votes_type=greaterthaneq&emailtype1=substring&email1=&emailtype2=substring&email2=&emailtype3=substring&email3=&chfieldvalue=&chfieldfrom=&chfieldto=Now&j_top=AND&f0=OP&j0=AND&f1=OP&j2=AND&f3=CP&f4=CP&f5=noop&o5=noop&v5=&action=wrap

# Status Counts Chart

https://bz.apache.org/bugzilla/reports.cgi?product=POI&datasets=UNCONFIRMED&datasets=NEW&datasets=ASSIGNED&datasets=REOPENED&datasets=NEEDINFO&datasets=RESOLVED&datasets=VERIFIED&datasets=CLOSED&datasets=FIXED&datasets=INVALID&datasets=WONTFIX&datasets=LATER&datasets=REMIND&datasets=DUPLICATE&datasets=WORKSFORME&datasets=MOVED

# Status Counts Chart - Only active states

https://bz.apache.org/bugzilla/reports.cgi?product=POI&datasets=UNCONFIRMED&datasets=NEW&datasets=ASSIGNED&datasets=REOPENED&datasets=NEEDINFO

# Requirements

* cairo development files (libcairo-dev on Debian/Ubuntu)
* R
* R packages
** parsedate
** reshape2
** ggplot2
** svglite

Install packages via 

```
$ R
> install.packages("<package>")
> quit()
```

# How to create stats/charts

```
./gradlew refresh
```
