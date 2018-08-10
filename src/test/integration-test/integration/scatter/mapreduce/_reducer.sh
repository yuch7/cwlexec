#!/usr/bin/env bash

for i in $@; do
    cat $i
done > out.out
