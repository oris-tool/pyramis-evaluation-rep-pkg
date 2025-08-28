package it.unifi.hierarchical.epew25;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.oristool.eulero.modeling.Activity;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trees.DeterministicEnablingState;


import java.io.*;
import java.util.*;


public class GTGenerator {

    private static String APPROXIMATION_PATH = System.getProperty("user.dir") + "/approximations/approximations.json";


    private static final String GT_BASE_PATH = System.getProperty("user.dir") + "/POSTP_GT/";
    private static final double GT_TIME_STEP = 0.1;
    private static final double GT_TIME_LIMIT = 25.;
    private static final int GT_SIMULATION_RUNS = 1000000;


    private static final int[] PARALLEL_COMBINATIONS = { 4, 8 };
    private static final int[] SEQUENCE_COMBINATIONS = { 4, 8 };
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

        double gtTimeStep = GT_TIME_STEP;
        double gtTimeLimit = GT_TIME_LIMIT;
        int gtSimulationRuns = GT_SIMULATION_RUNS;


        String gtBasePath = GT_BASE_PATH + gtSimulationRuns;

        File gtFile = new File(gtBasePath);
        if (!gtFile.getParentFile().exists()) {
            gtFile.getParentFile().mkdirs();
        }

        
        EuleroModelBuilder builder;

        builder = new EuleroModelBuilder();
        builder.setSequenceA(2);
        builder.setSequenceB(2);
        builder.setSwicthCardinalityM(2);

        builder.setParallelismA(2);
        builder.setParallelismB(2);
        
        System.out.println("Generating base Combination");
        double[] basegtCDF = generateGroundTruth(builder,distributionMapping, gtTimeStep, gtTimeLimit, gtSimulationRuns);
        
        String baseGtPath = gtBasePath + "GT_PAR2_SEQ2_ALT2.txt";
        saveArrayToFile(basegtCDF, gtTimeStep, baseGtPath);

        System.out.println("Generating Parallel Combination GTs");
        for (int parallelConfig : PARALLEL_COMBINATIONS) {
            builder = new EuleroModelBuilder();
            builder.setSequenceA(2);
            builder.setSequenceB(2);
            builder.setSwicthCardinalityM(2);

            builder.setParallelismA(parallelConfig);
            builder.setParallelismB(parallelConfig);
            
            System.out.println("Generating Parallel COmbination GT: " + parallelConfig);
            double[] gtCDF = generateGroundTruth(builder,distributionMapping, gtTimeStep, gtTimeLimit, gtSimulationRuns);
            
            String gtPath = gtBasePath + "GT_PAR" + parallelConfig + "_SEQ2_ALT2.txt";
            saveArrayToFile(gtCDF, gtTimeStep, gtPath);
        }

        System.out.println("Generating Sequence Combination GTs");
        for (int sequenceConfig : SEQUENCE_COMBINATIONS) {
            builder = new EuleroModelBuilder();
            builder.setParallelismA(2);
            builder.setParallelismB(2);
            builder.setSwicthCardinalityM(2);

            builder.setSequenceA(sequenceConfig);
            builder.setSequenceB(sequenceConfig);
            
            System.out.println("Generating Sequence COmbination GT: " + sequenceConfig);
            double[] gtCDF = generateGroundTruth(builder, distributionMapping, gtTimeStep, gtTimeLimit, gtSimulationRuns);
            
            String gtPath = gtBasePath + "GT_PAR2_SEQ" + sequenceConfig + "_ALT2.txt";
            saveArrayToFile(gtCDF, gtTimeStep, gtPath);
        }

        System.out.println("Generating Alternative Activities Combination GTs");
        for (int alternativeConfig : ALTERNATIVES_COMBINATIONS) {
            builder = new EuleroModelBuilder();
            builder.setParallelismA(2);
            builder.setParallelismB(2);
            builder.setSequenceA(2);
            builder.setSequenceB(2);

            builder.setSwicthCardinalityM(alternativeConfig);
            
            System.out.println("Generating Alternatice Activities Combination GT: " + alternativeConfig);
            double[] gtCDF = generateGroundTruth(builder, distributionMapping, gtTimeStep, gtTimeLimit, gtSimulationRuns);
            
            String gtPath = gtBasePath + "GT_PAR2_SEQ2_ALT" + alternativeConfig + ".txt";
            saveArrayToFile(gtCDF, gtTimeStep, gtPath);
        }

    }

    public static double[] generateGroundTruth(EuleroModelBuilder builder, Map<String, DistributionParams> distributionMapping, double timeStep,
            double timeLimit, int simulationRuns) {

        System.out.println("Simulation Started at: " + System.currentTimeMillis());
        long simulationStartTime = System.currentTimeMillis();
        double[][][] simulationResult = simulationExperiment(builder, distributionMapping, timeStep, timeLimit, simulationRuns);
        long simulationEndTime = System.currentTimeMillis();
        Long simulationElapsedTime = simulationEndTime - simulationStartTime;
        System.out.println("Simulation elapsed time: " + simulationElapsedTime);

        double[] simulationCDF = extractPlainCDFFromSimulationSolution(simulationResult);

        return simulationCDF;
    }


    public static double[][][] simulationExperiment(EuleroModelBuilder builder,
            Map<String, DistributionParams> distributionMapping,
            double timeStep, double timeLimit, int runs) {
        Activity model = builder.generateSipthModel(distributionMapping);
        TransientSolution<DeterministicEnablingState, RewardRate> simulate = model.simulate(String.valueOf(timeLimit),
                String.valueOf(timeStep), runs);
        return simulate.getSolution();
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

}
