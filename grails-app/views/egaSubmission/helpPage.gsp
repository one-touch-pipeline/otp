<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <title><g:message code="egaSubmission.title"/></title>
    </head>

    <body>
        <div class="body">
            <h2>User documentation</h2>
            <p>
                To start a new submission click on the menu item "EGA".
                If you would like to create one submission for several projects, please create them individually for each project with the same name.
                After that you will be taken to the overview page with all existing submissions for your chosen project.
                This documentation explains the OTP part of a submission.
                For more information on EGA see their <a href="https://ega-archive.org/submission/quickguide" target="_blank">Submission Quick Guide</a>
                or <a href="https://ega-archive.org/submission/FAQ" target="_blank">FAQ</a>.
            </p>

            <h3 id="overview">Overview page</h3>
            <p>
                You can choose three actions:
                <ul>
                    <li>change project</li>
                    <li>start new submission -> click New Submission</li>
                    <li>continue existing submission -> click Continue with submission</li>
                </ul>
            </p>
            <img class="centerImage" alt="overview" src="${assetPath(src: 'egaSubmission/overview_select.png')}">
            <figcaption>Overview page</figcaption>
            <p>
                If you edit an existing submission you will be taken to the last unsaved step.<br>
                If you create a new submission you will be taken to the following page.
            </p>

            <h3 id="new">New submission page</h3>
            <p>
                Please enter the following information on this page:
                <ul>
                    <li>Ega Box -> submitter account for your project, <a href="https://ega-archive.org/submission-form.php" target="_blank">request one at EGA</a> if you do not have one yet</li>
                    <li>Name of submission -> short informal label for internal communication</li>
                    <li>Study name -> title of the study to display on EGA (usually publication title)</li>
                    <li>Study type -> type of data you are submitting</li>
                    <li>Study abstract -> description of the study to display on EGA (usually publication abstract)</li>
                    <li>(optional) PubMed-ID -> if already assigned, your publications PubMed-ID (e.g. <a href="https://www.ncbi.nlm.nih.gov/pubmed/28803971" target="_blank">28803971</a>)</li>
                </ul>
            </p>
            <img class="centerImage" alt="new" src="${assetPath(src: 'egaSubmission/new_leer.png')}">
            <figcaption>Create a new submission</figcaption>
            <p>
                If you click the Submit button the submission will be created and you will be taken to the next page.
            </p>

            <h3 id="selectSamples">Select samples page</h3>
            <p>
                On this page all samples assigned to the selected project are displayed.
                By clicking on the checkboxes you can select samples.
                Above the table is a filter for sequence types to facilitate the selection.
            </p>
            <img class="centerImage" alt="select_samples" src="${assetPath(src: 'egaSubmission/select_samples.png')}">
            <figcaption>Select your samples</figcaption>
            <p>
                If you click the Next button, all samples will be linked to the submission and you will be taken to the next page.
            </p>

            <h3 id="sampleInformation">Sample information page</h3>
            <p>
                On this page you should complete the following information for each sample:
                <ul>
                    <li>Ega Sample Alias -> an alias for the sample which will be published and displayed on EGA</li>
                    <li>FASTQ or BAM -> select which file type do you want to submit</li>
                </ul>
                Here you can choose whether you want to configure the information on the web interface or rather in a CSV file.
                If you want to work with the CSV file click on the button 'Download CSV'.
                Now you can open and edit the file in a spreadsheet program.
                Then you can transfer the information from the CSV file with the upload mask above the table.
            </p>
            <img class="centerImage" alt="samples_info" src="${assetPath(src: 'egaSubmission/samples_info.png')}">
            <figcaption>Display of selected samples and configuration of aliases</figcaption>
            <p>
                If you click the Next button, all information will be stored and you will be taken to the next page.
            </p>

            <h3 id="selectFastq">Select FASTQ Files page</h3>
            <p>
                If you choose FASTQ in the previous step, you will enter this page.
                First, you have to choose what data you want to submit.
                Please make sure to have at least one file per sample selected.
                Then please click on "Confirm with file selection".
                Now you can configure filenames which will be published on EGA.
                Again, you have the choice between Web interface and CSV file.

            </p>
            <img class="centerImage" alt="select_files" src="${assetPath(src: 'egaSubmission/select_files.png')}">
            <figcaption>Selection of files</figcaption>
            <p>
                If you click the Save aliased button, all information will be stored and you will be taken to the next page.
                If you have not selected any BAM files, your submission processing is finished and we can start with the upload now.
            </p>

            <h3 id="selectBam">Select BAM Files page</h3>
            <p>
                If you choose BAM on the sample information page, you will enter this page.
                At the moment there is the restriction that only internally processed BAM files can be uploaded.
                Therefore there is a deactivated preselection.
                First click on "Confirm with file selection" and than you can configure filenames similar to those on the FASTQ files page.
                You can use the web interface or the CSV file.
            </p>
            <p>
                If you click the Save aliased button, all information will be stored and you will be redirected to the overview page.
                Now we can start with the upload.
            </p>
        </div>
    </body>
</html>
