/*
 * Copyright 2011-2021 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import groovy.json.JsonSlurper
import groovy.text.SimpleTemplateEngine

import java.time.*
import java.time.format.DateTimeFormatter


// read CSV file
List<List<String>> c = new File("review/final.csv").readLines()*.split(";")

List<String> header = c.head() as List
List<List<String>> data = c.tail() as List
int themeIdx = header.indexOf("THEME")
int pointsIdx = header.indexOf("STORY_POINTS")
int lastSprintIdx = header.indexOf("IS_LAST_SPRINT")
int summaryIdx = header.indexOf("SUMMARY")
int issueNoIdx = header.indexOf("OTP")
final String LAST = "LAST"
final String OTHER = "Other"

Closure<String> get = { String[] line, int index ->
    if (index < 0) {
        return ""
    }
    try {
        line[index]
    } catch (ArrayIndexOutOfBoundsException e) {
        ""
    }
}

Map<String, List<String[]>> th = data.groupBy { get(it, themeIdx) }.sort { it.value.size() }


// get sprint name, start and end dates
Object json = new JsonSlurper().parse(new URL("https://one-touch-pipeline.myjetbrains.com/youtrack/api/agiles/106-4?fields=currentSprint(name,start,finish)"))

String sprintName = json.currentSprint.name

LocalDate startDate = Instant.ofEpochMilli(json.currentSprint.start).atZone(ZoneId.systemDefault()).toLocalDate()
LocalDate finishDate = Instant.ofEpochMilli(json.currentSprint.finish).atZone(ZoneId.systemDefault()).toLocalDate()
LocalDate startDateNext = finishDate
LocalDate finishDateNext = startDateNext.plusDays(21)

String duration = "${startDate} – ${finishDate}"
String durationNext = "${startDateNext} – ${finishDateNext}"


// get releases
StringBuilder stdOut = new StringBuilder()
Process proc = ["git", "for-each-ref", "refs/tags/", "--format=%(creatordate:short) %(refname)"].execute()
proc.consumeProcessOutputStream(stdOut)
proc.waitForOrKill(1000)
String releases = stdOut.readLines().collectEntries {
    String[] s = it.split(" ")
    [LocalDate.parse(s[0], DateTimeFormatter.ISO_LOCAL_DATE), s[1].drop(10)]
}.findAll {
    it.value.startsWith("v")
}.findAll {
    it.key > startDate && it.key < finishDate
}.sort {
    it.key
}.collect {
    it.value
}.join(", ")


// create overview
String overview = """
# Theme Overview

<table class="table table-striped">
  <tr>
    <th>Theme</th>
    <th>Tasks</th>
    <th>Size/Story Points</th>
  </tr>
"""

int tasksTotal = 0
int pointsTotal = 0
overview += th.collect { theme ->
    int tasks = theme.value.size()
    tasksTotal += tasks
    int points = theme.value.sum { issue ->
        try {
            get(issue, pointsIdx) as int
        } catch (NumberFormatException e) {
            0
        }
    }
    pointsTotal += points

    return """
    <tr>
      <td>${theme.key.trim() ?: OTHER}</td>
      <td>${tasks}</td>
      <td>${points}</td>
    </tr>
    """
}.join()

overview += """
  <tr>
    <th>Total</th>
    <th>${tasksTotal}</th>
    <th>${pointsTotal}</th>
  </tr>
</table>
"""


// create details
List<String> details = []
int i = 0
String lastTheme = null
th.each { theme ->
    theme.value.each { issue ->
        if (i % 10 == 0 || lastTheme != theme.key) {
            if (i != 0) {
                details << "\n*) Issue finished in last sprint after sprint review"
                details << ""
                details << "---"
                details << ""
                i = 0
            }
            details << "# Details"
            details << ""
            details << "## ${theme.key.trim() ?: OTHER}"
            details << ""
        }
        int indent = (9 - get(issue, issueNoIdx).length()) * 2
        details << " - [${get(issue, issueNoIdx)}]:${"&nbsp;" * indent}${get(issue, summaryIdx)} ${(get(issue, lastSprintIdx) == LAST) ? "*" : ""}"
        i++
        lastTheme = theme.key
    }
}
details << "\n*) Issue finished in last sprint after sprint review"


// add data to template
new File("review/presentation.html").text = new SimpleTemplateEngine()
        .createTemplate(new File("bashScripts/sprintReview/template.html"))
        .make([
                OVERVIEW     : overview,
                DETAILS      : details.join("\n"),
                SPRINT_NAME  : sprintName,
                DURATION     : duration,
                DURATION_NEXT: durationNext,
                RELEASES     : releases,
        ])
        .toString()
