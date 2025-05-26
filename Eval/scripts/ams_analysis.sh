#!/bin/bash

rm ../intermediate_results/classes_with_tainted_repo.json
rm ../intermediate_results/sources.json

# execute analysis of shopme frontend
docker run --rm -v "$(pwd)/../applications/account-management-system/WEB-INF:/artifact/" -v "$(pwd)/../intermediate_results:/intermediate_results/" csvscan "ams" /artifact/classes /artifact/lib

sleep 5

docker run --rm -v "$(pwd)/../applications/account-management-system/WEB-INF:/artifact/" -v "$(pwd)/../intermediate_results:/intermediate_results/" csvscan "ams" /artifact/classes /artifact/lib | tee ../results/ams.txt

