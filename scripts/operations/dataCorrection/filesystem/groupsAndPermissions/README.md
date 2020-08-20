<!--
  ~ Copyright 2011-2020 The OTP authors
  ~
  ~ Permission is hereby granted, free of charge, to any person obtaining a copy
  ~ of this software and associated documentation files (the "Software"), to deal
  ~ in the Software without restriction, including without limitation the rights
  ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  ~ copies of the Software, and to permit persons to whom the Software is
  ~ furnished to do so, subject to the following conditions:
  ~
  ~ The above copyright notice and this permission notice shall be included in all
  ~ copies or substantial portions of the Software.
  ~
  ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  ~ SOFTWARE.
  -->


# Group and permission cleanup scripts

These scripts can be used to do a mass cleanup of all files in project. It cleans the groups and permissions.

Note that the project mass submission script uses 'bsub' as its submission command and thus is LSF specific.

## WARNING

**NEVER** correct the permissions/groups of files, on which processes work. This will kill the associated jobs and we
will have to restart from the beginning.

Wait for a project to be finished, change it pid based or manually.


## Script description and call

### Correction scripts

There are two scripts for correcting groups and permissions. One is Project based and one is PID/SeqType based.

```
correct-unix-group-and-permission.sh
correct-unix-group-and-permission-on-pid-directory.sh
```

Parameters between the script are the same, so I will only list one call here

```bash
bash correct-unix-group-and-permission.sh \ # (or: `correct-unix-group-and-permission-on-pid-directory.sh`)
     "<mode>" \                             # see section 'Modes'
     "<absolute project directory>" \       # the project directory of the wanted project
     "<look-for group>" \                   # the group of which the files should be changed
     "<replace-with group>";                # the group to change the found files to
```

### Mass submission scripts

The scripts above can be used manually but the project based script relies on parallelized execution on a cluster. An
LSF cluster to be specific.

```
execute-changes-on-all-projects.sh
execute-changes-on-all-pids.sh
```

The expected call for these scripts is structured as follows:

```
bash execute-changes-on-all-projects.sh <MODE> <project file> <group to change>
eg.:
bash execute-changes-on-all-projects.sh projects.csv OTP-GROUP
```

The parameter structure for both scripts is the same, so they are interchangeable.


## Modes

The script support multiple modes.

- **list**: find and list the files to be changed, but do not do any changes to them

- **clean**: find and list the files to be changed and execute the changes


## What and how do these scripts clean?

The scripts consist of multiple find commands with each having multiple -exec parameters for different types of files.
The PID specific script is just a specialized version of what we do to the sequencing directory in the Project script.
Because of this I will not list it separately here.

The script cleans a project in four steps, one for each subdirectory. Each step sets the group and adapts directory and
file permission based on where they are located and what type of file they are:

  - **config directory:**
    - directories are set to 2750
    - config files should have the permission 440
  - **project info directory:**
    - directories are set to 2700
    - project infos should have the permission 400
  - **sequencing directory:**
    - directories are set to 2750
    - all files are set to 440
    - an exception to this rule are the following files:
      - `*.bam` and `*.bai` which are set to 444
      - single cell mapping files `0_all/*_mapping.tsv` are set to 640
      - `.roddyExecCache.txt` and `zippedAnalysesMD5.txt` which are ignored
  - **submission directory:**
    - directories are set to 2700
    - all submission files should be 600

## Input File Structure

### projects.csv

Expected format in project file:
```
<project name>,<project unix group>,<relative project path>
```

Example Content:
```
Project_A01,A01-GROUP,a01
```

Generate the file content via the OTP console like this:

```groovy
Project.list().sort { it.name }.each {
    println ([it.name, it.unixGroup, it.dirName].join(","))
}
''
```

### pids.csv

Expected format in PID file:
```
<pid>,<seqType descriptor>,<project unix group>,<vbp path>
```

Example content:
```
A01-A1B2C3,WGS PAIRED bulk,A01-GROUP,a01/sequencing/whole_genome_sequencing/view-by-pid/A01-A1B2C3
```
