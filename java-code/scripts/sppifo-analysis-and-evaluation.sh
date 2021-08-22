#!/usr/bin/env bash

realpath=$(dirname "$(readlink -f "${0}")")
mode="${mode:-sequential}"

find "${realpath}/sppifo" -type f -name 'fig*' -print | \
    while read -r cmdline; do
        realpath="${realpath}/sppifo" ${cmdline};
    done
