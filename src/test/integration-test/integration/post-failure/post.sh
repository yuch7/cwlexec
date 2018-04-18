#!/bin/bash

JOB_ID=$CWLEXEC_JOB_ID
brequeue -aH $JOB_ID
bmod -Z "exit 0" $JOB_ID
bresume $JOB_ID
