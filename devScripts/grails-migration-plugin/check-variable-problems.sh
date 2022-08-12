#
# Copyright 2011-2022 The OTP authors
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#


# Problems with sql containing variable reference: "${variable}", for example ${pid}
#
# The liquibase library provides variable substitution.
# https://docs.liquibase.com/concepts/changelogs/property-substitution.html
# There are multiple possibilities to define such variables, one of them is the JAVA properties.
# And there are many default properties, one of these defaults is 'pid' for process id.
#
# On the other hand roddy also uses variable substitution, which for example allows simple definition of file structures.
# Therefore, the default configuration inserted via liquibase contains also many variable definition.
# One important value is 'pid' for pseudonym id. Therefore this variable shouldn't be substituted by liquibase.
#
# As long as liquibase isn't aware of a variable, it is passed on unchanged. But if the value is known, it is replaced.
# Since 'pid' is known by liquibase, it is replaced, which is wrong, since it should be inserted unchanged.
#
# To avoid substitution, the string should be splited and connected by sql: '${p'||'id}'
# This way, it is not recognized by the plugin, but sql execution connects both strings, so the value in the database is correct.
#
# This script checks for this variable to ensure, that new values do not contain the variable by accident.

mkdir -p tmp

grep -r "\${pid}" migrations/changelogs/defaultValues/ > tmp/pids.txt

set -e

if [ -s  tmp/pids.txt ]; then
  echo "found variable '\${pid}' in migrations/changelogs/defaultValues/*"
  echo ""
  cat tmp/pids.txt
  echo ""
  echo "found variable '\${pid}' in migrations/changelogs/defaultValues/*"
  echo "That makes problems, since it will already replaced by liquibase and is therefore not available for roddy"
  exit 1
fi
