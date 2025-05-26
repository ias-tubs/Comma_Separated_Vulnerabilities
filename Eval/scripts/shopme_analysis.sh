#!/bin/bash

rm ../intermediate_results/classes_with_tainted_repo.json
rm ../intermediate_results/sources.json

# execute analysis of shopme frontend
docker run --rm -v "$(pwd)/../applications/shopme/shopmeF/BOOT-INF:/artifact/" -v "$(pwd)/../intermediate_results:/intermediate_results/" csvscan "shopme" /artifact/classes /artifact/lib

sleep 5

docker run --rm -v "$(pwd)/../applications/shopme/shopmeB/BOOT-INF:/artifact/" -v "$(pwd)/../intermediate_results:/intermediate_results/" csvscan "shopme" /artifact/classes /artifact/lib | tee ../results/shopme.txt

