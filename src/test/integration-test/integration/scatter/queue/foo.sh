#!/usr/bin/env bash

filename=$1
nFoos=$( cat $filename )

for i in $(seq $nFoos); do
  if [ $i = $nFoos ]; then
    echo "$i sheep"
  else
    echo -n "$i sheep, "
  fi
done > $nFoos.foo.txt
