# Artifact evaluation for the paper "Comma Separated Vulnerabilities: Detecting Formula Injection in the Wild"

This repository contains source code and data to help reproduce the evaluation results of the paper [*Comma Separated Vulnerabilities: Detecting Formula Injection in the Wild*](https://www.ias.cs.tu-bs.de/publications/comma_separated_vulnerabilities.pdf) by [Manuel Karl](https://www.tu-braunschweig.de/en/ias/staff/manuel-karl), Louis Bettels, [Martin Johns](https://www.tu-braunschweig.de/en/ias/staff/martin-johns), and [David Klein](https://www.tu-braunschweig.de/en/ias/staff/david-klein)

This document explains the format and instructions to execute the evaluation.


## Requirements
- Java 17 and Docker

## Components
- **CSVScan**: The static analysis to detect the data flow of user-controlled data into CSV exports. For ease of use it is wrapped in a Dockerfile with a bash script (i.e., `build_container.sh`) to create a Docker container containing the analysis.
- **Eval**: Applications and execution scripts to reproduce selected results from the paper.
    - **applications**: Unpacked executables of 4 applications from the paper.
    - **intermediate_results**: Folder for storing intermediate results during the two phase analysis. The folder must be empty before each analysis in order to avoid influencing the results. The bash scripts cleans up the folder before each new execution to ensure a clean state.
    - **results**: A .txt file is created for each analysis with the flows found. End-to-end flows can be found at the end of the file, recognizable by “Found flow into Sink with Name: `printRecord`” and “Taint loaded from Repository: `com.example.RealTimeApp.repository.TaskRepo`” at the beginning of the flow. This specifies the repository (database table) from which the data was loaded and the name of the method for the CSV export. In the previous example, the data is loaded using `TaskRepo` and exported to a CSV using the `printRecord` method.
    - **scripts**: Bash scripts for running the analysis for the four included applications. 


## Setup
- Execute bash script `build_container.sh` inside the 'CSVScan' folder to create a Docker image containing the static analysis.


## Reproducing Results
- To demonstrate the functionality, we provide four different applications and ready-to-use bash scripts for each of them.
- Execute the following steps to reproduce the results:
    1. Build container with setup script `build_container.sh` under 'CSVScan'
    2. Reproduce the results for 'applications/account-management-system'
        1. Execute `ams_analysis.sh` under 'Eval/scripts'
        2. Validate result 'ams.txt' under 'Eval/results'. It should show a data flow from `TransactionRepository` into the method `println` which writes into CSV. Searching for `Transaction` in the txt file should show flows from user-controlled input into the repository identified by starting with 'Persist of Object Type: com.barclays.capstone.main.model.Transaction'.
    3. Reproduce results for 'applications/cuonganh-EmployeeManagement'
        1. Execute `employee_analysis.sh` under 'Eval/scripts'
        2. Validate result 'employee.txt' under 'Eval/results'. It should show a data flow from `EmployeeRepository` into the method `exportColumnsValue` which writes into CSV. Searching for `Employee` in the txt file shoud show flows from user-controlled input into the repository identified by starting with 'Persist of Object Type: com.example.employee.model.entity.Employees'.
    4. Reproduce results for 'applications/shopme'
        1. Execute `shope_analysis.sh` under 'Eval/scripts'
        2. Validate results 'shopme.txt' under 'Eval/results'. It should show a data flow from `CustomerRepository` into the method `write` which writes into a CSV. Searching for `Customer` in the txt file should show flows from user-controlled input into the repository identified by starting with 'Persist of Object Type: com.shopme.common.entity.Customer'. This example shows that our approach is able to handle applications consisting of separete frontend and backend parts by first analysing the frontend and afterward analyse the backend part.
    5. Reproduce results for 'applications/yurets1-telegram'
        1. Execute `yurets_analysis.sh` under 'Eval/scripts'
        2. Validate results 'yurets.txt' under 'Eval/results'. It should show a data flow from `TaskRepo` into the method `printRecord` which writes into CSV. Searching for `Task` should show flows from user-controlled input into the repository identified by starting with 'Persist of Object Type: com.example.RealTimeApp.entity.Task'.

## Dataset
Due to the size, we only supply 4 applications from the paper to show the functionality.
These are the unpacked builds of the respective applications.

1. applications/account-management-system - [Original](https://github.com/ags4436/account-management-system)
2. applications/cuonganh-EmployeeManagement - [Original](https://github.com/cuonganh/EmployeeManagement)
3. applications/shopme - [Original](https://github.com/Rapter1990/Shopme)
    - This application is divided into two parts, frontend and backend, and best demonstrates the two-phase analysis process. In the first case, the frontend is analyzed in order to find a flow of user-controlled data into the DB. Subsequently, the backend application (at the second start of the analysis) is analyzed to find flows from the database into a sink that writes CSVs. 
4. applications/yurets1-telegram - [Original](https://github.com/yurets1/telegram)

## Cite Us!
If you find this repository useful or are using any of it for your research, please cite us.

```bib
@inproceedings{KarBetJohKle25,
  author        = {Manuel Karl AND Louis Bettels AND Martin Johns AND David Klein},
  title         = {{Comma Separated Vulnerabilities: Detecting Formula Injection in the Wild}},
  booktitle     = {WOOT Conference on Offensive Technologies},
  year          = 2025,
}
```
