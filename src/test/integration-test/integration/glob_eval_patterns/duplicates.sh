#!/usr/bin/env bash

NAME=$1
CONTENTS=$2
ONE="_1.txt"
TWO="_2.txt"

echo $CONTENTS > "$NAME$ONE"
echo $CONTENTS > "$NAME$TWO"

