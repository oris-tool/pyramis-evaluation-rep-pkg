# The Pyramis Library: Efficient Numerical Evaluation of Hierarchical UML Statecharts applied to Stochastic Workflows

This repository is a companion page for the following submitted publication:

> Laura Carnevali, Reinhard German, Leonardo Montecchi, Leonardo Scommegna, and Enrico Vicario. 2025. CThe Pyramis Library: Efficient Numerical Evaluation of Hierarchical UML Statecharts applied to Stochastic Workflows. (Currently under review).

This replication package includes all resources needed to reproduce our experimental results.

## Prerequisites

- Java 23 or higher
- Maven 3.9.9 or higher

## Setup Instructions

1. Clone this repository:
```
git clone git@github.com:oris-tool/pyramis-efficient-numerical-evaluation-rep-pkg.git
cd pyramis-efficient-numerical-evaluation-rep-pkg
```

2. Run the installation script:
```
./install.sh
```

## Running the Experiments

### Ground Truth Generation

Execute from the root directory:
```
mvn exec:java -Dexec.mainClass="it.unifi.hierarchical.epew25.GTGenerator"
```
This command will generate the Ground Truth (GT) in the "POSTP_GT" folder.

**Important Notes**:
- To skip processing time, use the pre-computed GT files in `GT`

### Pyramis Analysis Experiment

```
mvn exec:java -Dexec.mainClass="it.unifi.hierarchical.epew25.PostProceedingsEpewExperiment" \
    -Dgt.path="[GT_PATH]" \
    -Dgt.runs="[GT_SIMULATION_RUNS]"
```

This command will perform the analysis with Pyramis for each configuration.

Default values for `gt.path` and `gt.runs` are set in order to work with the pre-calculated GT files.
If you want to perform the analysis with the files in the GT directory, both parameters can be omitted.

If you want to perform the analysis with a different GT, please specify the path to the folder where the files are located (the `[GT_PATH]` parameter) and how many runs were used to obtain them (the `[GT_SIMULATION_RUNS]` parameter). By default, the generation script outlined in the previous step uses 1,000,000 simulation runs.

### Pyramis Time Experiments

```
mvn exec:java -Dexec.mainClass="it.unifi.hierarchical.epew25.PostProceedingsTimeExperiment"
```
This command will calculate the mean time required to analyse each workflow configuration

## Results Visualization

To visualize the experimental results, which will be stored in the `results` directory:

1. **Install the required Python dependencies**:
    From the `scripts` directory, run:
    ```
    pip install -r requirements.txt
    ```

2. **Generate plots**:
   Relies on the `post-proceedings-single-plot.ipynb` Python Notebook


## File Structure

```
pyramis-efficient-numerical-evaluation-rep-pkg/
      |
      ├── install.sh                   # Installation script
      ├── src/                         # Source code 
      ├── GT/                          # Pre-calculated 1-million run Ground Truth CDFs
      └── scripts/                     # Python scripts for results visualization
```

## Troubleshooting

If you encounter any issues during the replication process, please:
1. Ensure all prerequisites are correctly installed
2. Verify all required files are in the correct locations
3. Check the console output for error messages
