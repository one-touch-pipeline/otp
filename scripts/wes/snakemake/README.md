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

#Info
A wes server for snakemake.

Please note that it does not compile with python 2.7, therefore python 3 is needed.

# Used documentation:
https://gitlab.com/one-touch-pipeline/wesnake

#Installation:
Following steps need to be done

## install mongo database
create using docker:
````
docker run --name mongo --publish 27017:27017 -d mongo
````

## prepare python 3 with virtual environment
```
module load python/3.7.0
virtualenv ~/venv-snakemake-python3
source ~/venv-snakemake-python3/bin/activate

pip install connexion
pip install pymongo
pip install connexion[swagger-ui]
```

## prepare WESnake
Change to the directory, in which wessnake should be downloaded.

Please Note: the current master (e246246) is broken, therefore an older commit is used.

```
git clone https://gitlab.com/one-touch-pipeline/wesnake.git
cd wesnake

#curent master is broken, use older commit
git checkout 9197cf3

python setup.py install
```

##Adapt config:
In the config the connection to the database is missed. The following changes ad the necessary information:

change file `config.yaml`
Adapt in section `mongo_server`:
- host: localhost
- port: 27017

#start database and server:
please go into the wesnake directory 

```
docker start mongo

module load python/3.7.0
source ~/venv-snakemake-python3/bin/activate
wesnake --config config.yaml
```

# Known Problem
- The response for service-info and listRun is not in the expected JSON format and throw therefore parse exception
- post request throw exception, that 'snakemake' directory missed. Why?
