package it.unifi.hierarchical.epew25;

import it.unifi.hierarchical.analysis.HierarchicalSMPAnalysis;
import it.unifi.hierarchical.analysis.NumericalValues;
import it.unifi.hierarchical.model.*;
import org.apache.commons.collections.map.HashedMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.oristool.eulero.modeling.Activity;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trees.DeterministicEnablingState;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.nio.file.Path;
import java.nio.file.Paths;


public class PostProceedingsSimulationTimeExperiment {

    private static final String RESULT_PATH = System.getProperty("user.dir") + "/results";
    private static String APPROXIMATION_PATH = System.getProperty("user.dir") + "/approximations/approximations.json";

    private static final double TIME_STEP = 0.1;
    private static final double TIME_LIMIT = 25.;
    private static final int SIMULATION_RUNS = 50;

    // private static final String GT_BASE_PATH = System.getProperty("user.dir") + "/POSTP_GT/";
    private static final double GT_TIME_STEP = 0.1;
    // private static final int GT_SIMULATION_RUNS = 1000000;

    private static String GT_BASE_PATH;
    private static int GT_SIMULATION_RUNS;
    static {
        GT_BASE_PATH = System.getProperty("user.dir") + "/GT/";

        String gtArg = System.getProperty("gt.path");

        if (gtArg != null) {
            GT_BASE_PATH = Paths.get(gtArg).toString() + "/";
        }
        System.out.println("GT path: " + GT_BASE_PATH);

        GT_SIMULATION_RUNS = 1000000;
        String runsArg = System.getProperty("gt.runs");

        if (runsArg != null) {
            try {
                GT_SIMULATION_RUNS = Integer.parseInt(runsArg);
            } catch (NumberFormatException e) {
                System.err.println("Invalid value for gt.runs: " + runsArg + ". Default used: " + GT_SIMULATION_RUNS);
            }
        }

        System.out.println("Using a GT obtained from a " + GT_SIMULATION_RUNS + " simulation runs");
    }


    // UTIL EXC
    public static class InvalidTimeStepException extends Exception {
        public InvalidTimeStepException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) throws InvalidTimeStepException {

        String resultDirectory = generateResultDirectory();

        List<DistributionParams> approximationsList = getDistributionParametersList(APPROXIMATION_PATH);
        Map<String, DistributionParams> distributionMapping = generateSipthDistributionMapping(approximationsList);

        double timeStep = TIME_STEP;
        double timeLimit = TIME_LIMIT;
        int simulationRuns = SIMULATION_RUNS;

        double gtTimeStep = GT_TIME_STEP;

        int parallelBranches = 2;
        int sequencesLenght = 2;
        int alternativeBranches = 8;
        
        String gtTestPath = GT_BASE_PATH + GT_SIMULATION_RUNS + "GT_PAR" + parallelBranches + "_SEQ" + sequencesLenght + "_ALT" + alternativeBranches + ".txt";
        double[] gtTestCDF = retrieveGT(gtTestPath, timeStep); // WARNING we assume the same distribution map!
        double JSTimeStep = 0.4;
        executePostProceedingsComparisonGTSimulationPyramis(parallelBranches,sequencesLenght,alternativeBranches, distributionMapping,0.1, timeLimit, gtTestCDF, gtTimeStep, JSTimeStep, resultDirectory, simulationRuns);

    }


    public static double[] retrieveGT(String filename, double timeStepCheck) throws InvalidTimeStepException {
        ArrayList<Double> firstCol = new ArrayList<>();
        ArrayList<Double> secondCol = new ArrayList<>();
        double gtStepSize = 0.0;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                String[] values = line.split("\t");
                firstCol.add(Double.parseDouble(values[0]));
                secondCol.add(Double.parseDouble(values[1]));
            }

            if (firstCol.size() >= 2) { // We asseme at least 2 steps
                gtStepSize = firstCol.get(1) - firstCol.get(0);
                if (gtStepSize != timeStepCheck) {
                    throw new InvalidTimeStepException(
                            String.format("Expected TimeStep : %f, actual GT TimeStep: %f",
                                    timeStepCheck, gtStepSize));
                }
            }
            double[] secondColumn = new double[secondCol.size()];
            for (int i = 0; i < secondCol.size(); i++) {
                secondColumn[i] = secondCol.get(i);
            }

            return secondColumn;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void executePostProceedingsComparisonGTSimulationPyramis(int concurrentBranches, int sequencesLenght, int alternativeBranches, Map<String, DistributionParams> distributionMapping,
            double timeStep, double timeLimit, double[] gtCdf, double gtTimestep, double JSTimestep,
            String resultDirectory, int simulationRuns) {

        PyramisModelBuilder pyramisBuilder = new PyramisModelBuilder();
        pyramisBuilder.setParallelismA(concurrentBranches);
        pyramisBuilder.setParallelismB(concurrentBranches);
        pyramisBuilder.setSequenceA(sequencesLenght);
        pyramisBuilder.setSequenceB(sequencesLenght);
        pyramisBuilder.setSwicthCardinalityM(alternativeBranches);

        long pyramisStartTime = System.currentTimeMillis();
        // double[] pyramisCDF = pyramisExperiment(distributionMapping, timeStep, timeLimit);
        double[] pyramisCDF = pyramisPostProceedingsExperiment(pyramisBuilder, distributionMapping, timeStep, timeLimit);
        long pyramisEndTime = System.currentTimeMillis();
        Long pyramisElapsedTime = pyramisEndTime - pyramisStartTime;
        System.out.println("Pyramis with timestep: " + timeStep + " elapsed time: " + pyramisElapsedTime);

        EuleroModelBuilder euleroBuilder = new EuleroModelBuilder();
        euleroBuilder.setParallelismA(concurrentBranches);
        euleroBuilder.setParallelismB(concurrentBranches);
        euleroBuilder.setSequenceA(sequencesLenght);
        euleroBuilder.setSequenceB(sequencesLenght);
        euleroBuilder.setSwicthCardinalityM(alternativeBranches);

        System.out.println("Simulation Started at: " + System.currentTimeMillis());
        long simulationStartTime = System.currentTimeMillis();
        // double[][][] simulationResult = simulationExperiment(distributionMapping, timeStep, timeLimit, simulationRuns);
        double[][][] simulationResult = simulationExperiment(euleroBuilder, distributionMapping, timeStep, timeLimit, simulationRuns);
        long simulationEndTime = System.currentTimeMillis();
        Long simulationElapsedTime = simulationEndTime - simulationStartTime;
        System.out.println("Simulation elapsed time: " + simulationElapsedTime);

        double[] simulationCDF = extractPlainCDFFromSimulationSolution(simulationResult);

        double[] gtSubsample = Utils.subsample(gtCdf, gtTimestep, JSTimestep);
        double[] pyramisSubsample = Utils.subsample(pyramisCDF, timeStep, JSTimestep);
        double[] simulationSubsample = Utils.subsample(simulationCDF, timeStep, JSTimestep);
        double jsDistancePyramis = Utils.jsDistance(gtSubsample, pyramisSubsample);
        double jsDistanceSimulation = Utils.jsDistance(gtSubsample, simulationSubsample);

        Map<String, Double> jsDistances = new HashedMap();
        System.out.println("Simulation (#Runs: " + simulationRuns + ") JS distance wrt GT: " + jsDistanceSimulation);
        System.out.println("Pyramis JS distance wrt GT: " + jsDistancePyramis);
        // jsDistances.put("Simulation", jsDistanceSimulation);
        // jsDistances.put("Pyramis", jsDistancePyramis);

        String expName = "PAR" + concurrentBranches + "_SEQ" + sequencesLenght + "_ALT"
                + alternativeBranches;
        saveArrayToFile(simulationCDF, timeStep, resultDirectory + File.separator + "simulationCDF_" + expName + "_ts" + timeStep + ".txt");
        saveArrayToFile(pyramisCDF, timeStep, resultDirectory + File.separator + "pyramisCDF_" + expName + "_ts" + timeStep + ".txt");

    }


    public static void executePostProceedingsComparisonGTPyramis(PyramisModelBuilder builder,
            Map<String, DistributionParams> distributionMapping,
            List<Double> timeSteps, double timeLimit, double[] gtCdf, double gtTimestep, String resultDirectory) {

        Map<Double, double[]> timeStepAnalysisResults = new HashedMap();
        Map<Double, Long> timeStepCompletionTimes = new HashedMap();
        for (double timeStep : timeSteps) {
            long pyramisStartTime = System.currentTimeMillis();
            timeStepAnalysisResults.put(timeStep,
                    pyramisPostProceedingsExperiment(builder, distributionMapping, timeStep, timeLimit));
            long pyramisEndTime = System.currentTimeMillis();
            Long pyramisElapsedTime = pyramisEndTime - pyramisStartTime;
            timeStepCompletionTimes.put(timeStep, pyramisElapsedTime);
            System.out.println("Pyramis with timestep: " + timeStep + " elapsed time: " + pyramisElapsedTime);
        }
        String expName = "PAR" + builder.getParallelismA() + "_SEQ" + builder.getSequenceA() + "_ALT"
                + builder.getSwicthCardinalityM();
        Map<Double, Double> jsDistances = new HashedMap();
        for (double timeStep : timeStepAnalysisResults.keySet()) {
            double[] pyramisCDF = timeStepAnalysisResults.get(timeStep);
            // int subsamplingFactor = (int) (timeStep / gtTimestep);
            double[] gtSubsample = Utils.subsample(gtCdf, gtTimestep, 0.4);
            double[] pyramisSubsample = Utils.subsample(pyramisCDF, timeStep, 0.4); // FIXME FIXED SUBSAMPLING
            // double jsDistance = Utils.jsDistance(gtSubsample, pyramisCDF);
            // jsDistances.put(timeStep, jsDistance);
            double jsDistance = Utils.jsDistance(gtSubsample, pyramisSubsample);
            // jsDistances.put(timeStep, jsDistance);
            jsDistances.put(timeStep, jsDistance);
            saveArrayToFile(pyramisCDF, timeStep,
                    resultDirectory + File.separator + "pyramisCDF_" + expName + "_ts" + timeStep + ".txt");
        }
        saveJSResults(jsDistances, resultDirectory, expName);
    }




    public static double[] pyramisPostProceedingsExperiment(PyramisModelBuilder builder,
            Map<String, DistributionParams> distributionMapping, double timeStep,
            double timeLimit) {
        HSMP hsmp = new HSMP(builder.generateSipthModel(distributionMapping, timeStep));
        HierarchicalSMPAnalysis analysis = new HierarchicalSMPAnalysis(hsmp, 0);
        Map<String, Double> evaluate = analysis.evaluate(timeStep, timeLimit);
        // System.out.println("CDF: " + analysis.cdf);
        NumericalValues cdf = analysis.cdf;
        return cdf.getValues();
    }



    public static List<DistributionParams> getDistributionParametersList(String approximationsPath) {
        List<DistributionParams> distributions = new ArrayList<>();
        try {
            JSONArray jsonArray = (JSONArray) new JSONParser().parse(new FileReader(approximationsPath));
            for (Object obj : jsonArray) {
                JSONObject entry = (JSONObject) obj;
                distributions.add(new DistributionParams((Double) entry.get("lambda"), (Double) entry.get("a"),
                        (Double) entry.get("b")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return distributions;
    }

    public static Map<String, DistributionParams> generateSipthDistributionMapping(
            List<DistributionParams> paramsList) {
        LinkedList<DistributionParams> paramsListClone = new LinkedList<>(paramsList);
        Map<String, DistributionParams> mapping = new HashMap<>();
        mapping.put("A1.1", paramsListClone.poll());
        mapping.put("A1.2", paramsListClone.poll());
        mapping.put("A1.3", paramsListClone.poll());
        mapping.put("A1.4", paramsListClone.poll());
        mapping.put("A1.5", paramsListClone.poll());
        mapping.put("A1.6", paramsListClone.poll());
        mapping.put("A1.7", paramsListClone.poll());
        mapping.put("A1.8", paramsListClone.poll());
        mapping.put("A2.1", paramsListClone.poll());
        mapping.put("A2.2", paramsListClone.poll());
        mapping.put("A2.3", paramsListClone.poll());
        mapping.put("A2.4", paramsListClone.poll());
        mapping.put("A2.5", paramsListClone.poll());
        mapping.put("A2.6", paramsListClone.poll());
        mapping.put("A2.7", paramsListClone.poll());
        mapping.put("A2.8", paramsListClone.poll());
        mapping.put("B1.1", paramsListClone.poll());
        mapping.put("B1.2", paramsListClone.poll());
        mapping.put("B1.3", paramsListClone.poll());
        mapping.put("B1.4", paramsListClone.poll());
        mapping.put("B1.5", paramsListClone.poll());
        mapping.put("B1.6", paramsListClone.poll());
        mapping.put("B1.7", paramsListClone.poll());
        mapping.put("B1.8", paramsListClone.poll());
        mapping.put("B2.1", paramsListClone.poll());
        mapping.put("B2.2", paramsListClone.poll());
        mapping.put("B2.3", paramsListClone.poll());
        mapping.put("B2.4", paramsListClone.poll());
        mapping.put("B2.5", paramsListClone.poll());
        mapping.put("B2.6", paramsListClone.poll());
        mapping.put("B2.7", paramsListClone.poll());
        mapping.put("B2.8", paramsListClone.poll());
        mapping.put("A3.1", paramsListClone.poll());
        mapping.put("A3.2", paramsListClone.poll());
        mapping.put("A4.1", paramsListClone.poll());
        mapping.put("A4.2", paramsListClone.poll());
        mapping.put("A5.1", paramsListClone.poll());
        mapping.put("A5.2", paramsListClone.poll());
        mapping.put("A6.1", paramsListClone.poll());
        mapping.put("A6.2", paramsListClone.poll());
        mapping.put("A7.1", paramsListClone.poll());
        mapping.put("A7.2", paramsListClone.poll());
        mapping.put("A8.1", paramsListClone.poll());
        mapping.put("A8.2", paramsListClone.poll());
        mapping.put("B3.1", paramsListClone.poll());
        mapping.put("B3.2", paramsListClone.poll());
        mapping.put("B4.1", paramsListClone.poll());
        mapping.put("B4.2", paramsListClone.poll());
        mapping.put("B5.1", paramsListClone.poll());
        mapping.put("B5.2", paramsListClone.poll());
        mapping.put("B6.1", paramsListClone.poll());
        mapping.put("B6.2", paramsListClone.poll());
        mapping.put("B7.1", paramsListClone.poll());
        mapping.put("B7.2", paramsListClone.poll());
        mapping.put("B8.1", paramsListClone.poll());
        mapping.put("B8.2", paramsListClone.poll());
        mapping.put("C", paramsListClone.poll());
        mapping.put("D", paramsListClone.poll());
        mapping.put("E", paramsListClone.poll());
        mapping.put("F", paramsListClone.poll());
        mapping.put("G", paramsListClone.poll());
        mapping.put("H", paramsListClone.poll());
        mapping.put("I", paramsListClone.poll());
        mapping.put("E", paramsListClone.poll());
        mapping.put("L1", paramsListClone.poll());
        mapping.put("L2", paramsListClone.poll());
        mapping.put("M1", paramsListClone.poll());
        mapping.put("M2", paramsListClone.poll());
        mapping.put("M3", paramsListClone.poll());
        mapping.put("M4", paramsListClone.poll());
        mapping.put("M5", paramsListClone.poll());
        mapping.put("M6", paramsListClone.poll());
        mapping.put("M7", paramsListClone.poll());
        mapping.put("M8", paramsListClone.poll());
        mapping.put("N", paramsListClone.poll());
        mapping.put("O", paramsListClone.poll());
        return mapping;
    }



    public static String generateResultDirectory() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String dateString = now.format(formatter);
        String newDirPath = RESULT_PATH + File.separator + dateString;
        File newDir = new File(newDirPath);

        if (!newDir.exists()) {
            newDir.mkdirs();
        }
        return newDirPath;
    }

    public static double[] extractPlainCDFFromSimulationSolution(double[][][] simulationSolution) {
        return Arrays.stream(simulationSolution)
                .mapToDouble(array -> array[0][0])
                .toArray();
    }

    public static void saveArrayToFile(double[] array, double timestep, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = 0; i < array.length; i++) {
                double timeValue = timestep * i;
                double arrayValue = array[i];

                writer.write(String.format("%.3f\t%.3f%n", timeValue, arrayValue));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveJSResults(Map<Double, Double> jsResults, String basePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(basePath + File.separator + "js_results.txt"))) {
            for (Map.Entry<Double, Double> result : jsResults.entrySet()) {
                writer.write("timestep: " + result.getKey() + ", JS distance: " + result.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveJSResults(Map<Double, Double> jsResults, String basePath, String filename) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(basePath + File.separator + filename + ".txt"))) {
            for (Map.Entry<Double, Double> result : jsResults.entrySet()) {
                writer.write("timestep: " + result.getKey() + ", JS distance: " + result.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveMetaData(List<Long> pyramisDurations,
            List<Long> simulationDurations,
            double pyramisTimestep,
            double simulationTimestep,
            int simulationRuns,
            double meanPyramid,
            double meanSimulation,
            String basePath) {
        // Salva le pyramid durations
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(basePath + File.separator + "pyramid_durations.txt"))) {
            for (Long duration : pyramisDurations) {
                writer.write(String.format("%.3f%n", duration.doubleValue()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Salva le simulation durations
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(basePath + File.separator + "simulation_durations.txt"))) {
            for (Long duration : simulationDurations) {
                writer.write(String.format("%.3f%n", duration.doubleValue()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Salva le medie
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(basePath + File.separator + "metadata.txt"))) {
            writer.write(String.format("pyramis timestep: %.3f", pyramisTimestep));
            writer.write(String.format("simulation timestep: %.3f", simulationTimestep));
            writer.write(String.format("simulation runs: %d", simulationRuns));
            writer.write(String.format("mean pyramis duration: %.3f%n", meanPyramid));
            writer.write(String.format("mean simulation duration: %.3f", meanSimulation));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printDistributions(HSMP hsmp) {
        LogicalLocation initialStep = hsmp.getInitialStep();
        printDistributions(initialStep);
    }

    private static void printDistributions(LogicalLocation step) {
        if (step instanceof SimpleStep) {
            SimpleStep castedStep = (SimpleStep) step;
            System.out
                    .println("Step Name:" + castedStep.getName() + " distribution: " + castedStep.getDensityFunction());
        }

        if (step instanceof CompositeStep) {
            CompositeStep castedCompositeStep = (CompositeStep) step;
            List<Region> regions = castedCompositeStep.getRegions();
            for (Region region : regions) {
                printDistributions(region.getInitialStep());
            }
        }

        List<LogicalLocation> nextLocations = step.getNextLocations();
        for (LogicalLocation nextlogicalLocation : nextLocations) {
            printDistributions(nextlogicalLocation);
        }
    }

    public static double[][][] simulationExperiment(EuleroModelBuilder builder,
            Map<String, DistributionParams> distributionMapping,
            double timeStep, double timeLimit, int runs) {
        Activity model = builder.generateSipthModel(distributionMapping);
        TransientSolution<DeterministicEnablingState, RewardRate> simulate = model.simulate(String.valueOf(timeLimit),
                String.valueOf(timeStep), runs);
        return simulate.getSolution();
    }

}
