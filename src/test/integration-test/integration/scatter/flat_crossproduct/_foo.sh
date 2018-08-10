#!/usr/bin/env bash

filename=$1
foo=$2

echo "$1, $2"

nFoos=$( cat $filename )
echo "$nFoos"
echo "------------>"

for i in $(seq $nFoos); do
  if [ $i = $nFoos ]; then
    echo "$i $foo"
  else
    echo -n "$i $foo, "
  fi
done > $nFoos.$foo.txt
