#!/bin/bash

rm ../intermediate_results/classes_with_tainted_repo.json
rm ../intermediate_results/sources.json

# execute analysis of shopme frontend
docker run --rm -v "$(pwd)/../applications/yurets1-telegram/BOOT-INF:/artifact/" -v "$(pwd)/../intermediate_results:/intermediate_results/" csvscan "yurets" /artifact/classes /artifact/lib

sleep 5

docker run --rm -v "$(pwd)/../applications/yurets1-telegram/BOOT-INF:/artifact/" -v "$(pwd)/../intermediate_results:/intermediate_results/" csvscan "yurets" /artifact/classes /artifact/lib | tee ../results/yurets.txt

