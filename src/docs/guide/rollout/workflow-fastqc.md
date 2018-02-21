
fastqc workflow
===============

It is not necessary to install the software on this way. It has only be available
on the cluster.


local
-----

- Download [fastqc](http://www.bioinformatics.babraham.ac.uk/projects/download.html#fastqc)
  We tested it with version 0.11.7


- copy downloaded file to the submission host
    ```
    scp $DOWNLOAD_PATH/$FASTQC_ZIP $DATA_USER@$SUBMISSION_HOST:
    ```


remote as data user
-------------------


- login on $SUBMISSION_HOST as $DATA_USER
    ```
    ssh $DATA_USER@$SUBMISSION_HOST
    ```

- create directory for fastq program
    ```
    mkdir -p $WORK_DIRECTORY/program/fastq
    ```

- change to the created directory
    ```
    cd $WORK_DIRECTORY/program/fastq
    ```

- move fastqc archive into this directory
    ```
    mv ~/$FASTQC_ZIP .
    ```

- unzip the fastqc zip
    ```
    unzip ~/$FASTQC_ZIP
    ```


[Back to Rollout Overview](index.md)
