#!/bin/bash

DIR=$(dirname $(realpath $0))

INPUT=$1
FIRST_PAGE=${2:-7}

if [[ -z $INPUT ]]; then
    >&2 echo "Usage: update-catalogue.bash {AZF|BZF}.pdf [FIRST_PAGE(7)]"
    exit 1
fi

if [[ ! -e $INPUT ]]; then
    >&2 echo "Cannot generate catalogue, $INPUT does not exist"
    exit 1
fi

if ! command -v pdftotext  &> /dev/null ; then
    >&2 echo "Executable 'pdftotext' does not exist on PATH. Is it installed?"
    exit 1
fi

VARIANT=$(echo $INPUT | sed -E 's/\.pdf$//' | tr '[:upper:]' '[:lower:]')
TXT=$VARIANT.txt
echo "converting $INPUT -> $TXT"

pdftotext -f $FIRST_PAGE $INPUT $TXT

## Replace space footers
if [[ $(uname -s) == "Linux" ]]; then
    SED=sed
elif [[ $(uname -s) == "Darwin" ]]; then
    if ! command -v gsed &> /dev/null ; then
        >&2 echo "Executable 'gsed' does not exist on PATH. Install it with 'brew install gnu-sed'"
        exit 1
    fi
    SED=gsed
else
    echo "Unsupported OS $(uname -s)"
    exit 1
fi
$SED -i \
    -e 's///' \
    -e 's/^Stand[ :].*$//' \
    -e 's/richtige Antwort immer A.*//' \
    -e 's/correct answer always A.*//' \
    -e 's/^Seite.*von.*$//' \
    -e 's/Prüfungsfragen.*$//' \
    $TXT

$SED -i -E \
     -e 's/^[0-9]{1,3}$/\0::/gm' \
     -e 's/^[A-D]$/\0::/gm' \
     $TXT

$SED -i -E -z \
     -e 's/([A-D]|[0-9]{1,3})::\n\n/\1::/gm' \
     -e 's/([A-D]|[0-9]{1,3})::(.+)\n(.+)\n/\1::\2 \3/gm' \
     $TXT


# Nicen empty lines
$SED -i -E -z \
     -e 's/\n{2,}/\n/gm' \
     -e 's/[0-9]+::.*/\n\0/gm' \
     $TXT



QUESTIONS=$DIR/../src/main/res/values/${VARIANT}_questions.xml

echo "updating $QUESTIONS..."

cat <<EOT > $QUESTIONS
<?xml version="1.0" encoding="utf-8"?>

<resources>

    <string-array name="${VARIANT}_questions">

EOT


$SED -E \
     -e '/[A-D]::/d' -e '/^$/d' \
     -e 's/"/\\"/g' \
     -e 's/([0-9]{1,3})::(.+)/        <item>\1. \2\<\/item\>/' \
     -e 's/\.\.\./…/g' \
     -e 's/…\./…/g' \
     -e 's/ - /–/g' \
     -e 's/FL ([0-9]+)/FL\1/g' \
     -e 's/lAS/IAS/g' \
     -e 's/ …/\\u00A0…/g' \
     -e 's/([0-9]{3,4}(\.[0-9])?) hPa/\1\\u00A0hPa/g' \
     -e 's/ MHz/\\u00A0MHz/g' \
     -e 's/([0-9]{3}),([0-9]{3})/\1\.\2/g' \
     $TXT >> $QUESTIONS

cat <<EOT >> $QUESTIONS
    </string-array>

</resources>
EOT


ANSWERS=$DIR/../src/main/res/values/${VARIANT}_original.xml

echo "updating $ANSWERS..."

cat <<EOF > $ANSWERS
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string-array name="${VARIANT}_original" formatted="false">
        <!-- Correct is always the first answer -->

EOF

$SED -E \
     -e '/[0-9]{1,3}::/d' -e '/^$/d' \
     -e 's/"/\\"/g' \
     -e 's/\.\.\./…/g' \
     -e 's/…\./…/g' \
     -e 's/FL ([0-9]+)/FL\1/g' \
     -e 's/lAS/IAS/g' \
     -e 's/A::(.+)/        <item>\1<\/item>/' \
     -e 's/B::(.+)/        <item>\1<\/item>/' \
     -e 's/C::(.+)/        <item>\1<\/item>/' \
     -e 's/D::(.+)/        <item>\1<\/item>\n/' \
     $TXT >> $ANSWERS

cat <<EOF >> $ANSWERS
    </string-array>
</resources>
EOF

echo "shuffling answers..."
$DIR/shuffle-answers-and-solutions.py --${VARIANT}
