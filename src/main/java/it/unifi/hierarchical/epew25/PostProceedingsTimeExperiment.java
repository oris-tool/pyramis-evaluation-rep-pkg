package it.unifi.hierarchical.epew25;

import it.unifi.hierarchical.analysis.HierarchicalSMPAnalysis;
import it.unifi.hierarchical.analysis.NumericalValues;
import it.unifi.hierarchical.model.*;
import org.apache.commons.collections.map.HashedMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class PostProceedingsTimeExperiment {

    private static final String RESULT_PATH = System.getProperty("user.dir") + "/results";
    private static String APPROXIMATION_PATH = System.getProperty("user.dir") + "/approximations/approximations.json";

    private static final int repetitions = 100;
    private static final double TIME_LIMIT = 25.;


    private static final List<Double> TIMESTEPS_COMPARED = List.of(0.4, 0.2, 0.1);


    private static final int[] PARALLEL_COMBINATIONS = {  4, 8 };
    private static final int[] SEQUENCE_COMBINATIONS = {  4, 8 };
    private static final int[] ALTERNATIVES_COMBINATIONS = { 4, 8 };

    // UTIL EXC
    public static class InvalidTimeStepException extends Exception {
        public InvalidTimeStepException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) throws InvalidTimeStepException {

        List<DistributionParams> approximationsList = getDistributionParametersList(APPROXIMATION_PATH);
        Map<String, DistributionParams> distributionMapping = generateSipthDistributionMapping(approximationsList);

        double timeLimit = TIME_LIMIT;

        PyramisModelBuilder builder;
        builder = new PyramisModelBuilder();
        
        builder.setParallelismA(2);
        builder.setParallelismB(2);
        builder.setSequenceA(2);
        builder.setSequenceB(2);
        builder.setSwicthCardinalityM(2);
        System.out.println("Baseline Combination Config");
        repeatPostProceedingsExperiment(builder, distributionMapping, TIMESTEPS_COMPARED, timeLimit);
        
        System.out.println("Parallel Combination Experiments");
        for (int parallelConfig : PARALLEL_COMBINATIONS) {
            builder = new PyramisModelBuilder();
            builder.setSequenceA(2);
            builder.setSequenceB(2);
            builder.setSwicthCardinalityM(2);

            builder.setParallelismA(parallelConfig);
            builder.setParallelismB(parallelConfig);

            System.out.println("Parallel Combination Config: " + parallelConfig);
            repeatPostProceedingsExperiment(builder, distributionMapping, TIMESTEPS_COMPARED, timeLimit);

        }

        System.out.println("Sequence Combination Experiments");
        for (int sequenceConfing : SEQUENCE_COMBINATIONS) {
            builder = new PyramisModelBuilder();
            builder.setParallelismA(2);
            builder.setParallelismB(2);
            builder.setSwicthCardinalityM(2);

            builder.setSequenceA(sequenceConfing);
            builder.setSequenceB(sequenceConfing);
            System.out.println("Sequence Combination Config: " + sequenceConfing);

            repeatPostProceedingsExperiment(builder, distributionMapping, TIMESTEPS_COMPARED, timeLimit);

        }

        System.out.println("Alternative Activities Combination Experiments");
        for (int alternativeConfig : ALTERNATIVES_COMBINATIONS) {
            builder = new PyramisModelBuilder();
            builder.setSequenceA(2);
            builder.setSequenceB(2);
            builder.setParallelismA(2);
            builder.setParallelismB(2);

            builder.setSwicthCardinalityM(alternativeConfig);

            System.out.println("Alternative Activities Combination Config: " + alternativeConfig);

            repeatPostProceedingsExperiment(builder, distributionMapping, TIMESTEPS_COMPARED, timeLimit);
        }


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

    public static void repeatPostProceedingsExperiment(PyramisModelBuilder builder,
            Map<String, DistributionParams> distributionMapping,
            List<Double> timeSteps, double timeLimit) {

        Map<Double, double[]> timeStepAnalysisResults = new HashedMap();
        Map<Double, List<Long>> timeStepCompletionTimes = new HashedMap();
        for (double timeStep : timeSteps) {
            List<Long> repetitionTimeList = new ArrayList<>();

            // this is just to warm up the experiment avoiding cold starts in timing.
            pyramisPostProceedingsExperiment(builder, distributionMapping, timeStep, timeLimit);

            for (int i = 0; i < repetitions; i++) {
                long pyramisStartTime = System.currentTimeMillis();
                timeStepAnalysisResults.put(timeStep,
                        pyramisPostProceedingsExperiment(builder, distributionMapping, timeStep, timeLimit));
                long pyramisEndTime = System.currentTimeMillis();
                Long pyramisElapsedTime = pyramisEndTime - pyramisStartTime;
                repetitionTimeList.add(pyramisElapsedTime);
                // System.out.println("Pyramis with timestep: " + timeStep + ", repetition number:" + i
                //         + ", elapsed time: " + pyramisElapsedTime);
            }
            timeStepCompletionTimes.put(timeStep, repetitionTimeList);
        }
        printAverageCompletionTimes(timeStepCompletionTimes);
    }

    private static void printAverageCompletionTimes(Map<Double, List<Long>> timeStepCompletionTimes) {
        timeStepCompletionTimes.entrySet().stream()
                .forEach(entry -> {
                    Double timeStep = entry.getKey();
                    List<Long> times = entry.getValue();
                    if (times != null && !times.isEmpty()) {
                        double average = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                        System.out.printf("Time Step %.2f: Average = %.2f ms%n", timeStep, average);
                    } else {
                        System.out.printf("Time Step %.2f: No data available%n", timeStep);
                    }
                });
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

}
