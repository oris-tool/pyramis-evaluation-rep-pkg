package it.unifi.hierarchical.epew25;

import it.unifi.hierarchical.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PyramisModelBuilder {

        private int parallelismA = 2;
        private int parallelismB = 2;
        private int sequenceA = 1;
        private int sequenceB = 1;
        private int swicthCardinalityM = 2;

        public Step generateModel(Map<String, DistributionParams> distributionMapping, double timestep) {
                return generateModel(distributionMapping, timestep, 16, 4);
        }

        public Step generateSipthModel(Map<String, DistributionParams> distributionMapping,
                        double timestep) {
                // ACE
                // Step A1 = generateNParallelActivities("A1", parallelismA, sequenceA,
                // distributionMapping, 1, timestep);
                Step A1 = generateNParallelSequences("A1_", "A", parallelismA, sequenceA, distributionMapping, 1,
                                timestep);
                FinalLocation ACEFinalLocation = new FinalLocation("AC1_final");
                SimpleStep E = new SimpleStep("E", distributionMapping.get("E").getCorrespondingPartitionedFunction(),
                                List.of(ACEFinalLocation), List.of(1.0), 1);
                SimpleStep C1 = new SimpleStep("C1", distributionMapping.get("C").getCorrespondingPartitionedFunction(),
                                List.of(E), List.of(1.0), 1);
                A1.addNextLocation(C1, 1.0);
                Region ACERegion = new Region(A1, RegionType.ENDING, timestep, false);

                // AC
                // Step A2 = generateNParallelActivities("A2", parallelismA, sequenceA,
                // distributionMapping, 2, timestep);
                Step A2 = generateNParallelSequences("A2_", "A", parallelismA, sequenceA, distributionMapping, 2,
                                timestep);
                FinalLocation AC2FinalLocation = new FinalLocation("AC2_final");
                SimpleStep C2 = new SimpleStep("C2", distributionMapping.get("C").getCorrespondingPartitionedFunction(),
                                List.of(AC2FinalLocation), List.of(1.0), 2);
                A2.addNextLocation(C2, 1.0);

                // BD
                // Step B = generateNParallelActivities("B", parallelismB, paramsB, 2,
                // timestep);
                Step B = generateNParallelSequences("B", "B", parallelismB, sequenceB, distributionMapping, 2,
                                timestep);
                FinalLocation BDFinalLocation = new FinalLocation("BD_final");
                SimpleStep D = new SimpleStep("D", distributionMapping.get("D").getCorrespondingPartitionedFunction(),
                                List.of(BDFinalLocation), List.of(1.0), 2);
                B.addNextLocation(D, 1.0);

                // ACBD
                CompositeStep AC2BD = new CompositeStep("AC2BD", CompositeStepType.LAST, 1, timestep);
                Region AC2Region = new Region(A2, RegionType.ENDING, timestep, false);
                Region BDRegion = new Region(B, RegionType.ENDING, timestep, false);
                AC2BD.addRegions(List.of(AC2Region, BDRegion));

                // FM
                FinalLocation FFinalLocation = new FinalLocation("F_final");
                SimpleStep F = new SimpleStep("F", distributionMapping.get("F").getCorrespondingPartitionedFunction(),
                                List.of(FFinalLocation), List.of(1.0), 2);
                Region FRegion = new Region(F, RegionType.ENDING, timestep, false);

                FinalLocation GFinalLocation = new FinalLocation("G_final");
                SimpleStep G = new SimpleStep("G", distributionMapping.get("G").getCorrespondingPartitionedFunction(),
                                List.of(GFinalLocation), List.of(1.0), 2);
                Region GRegion = new Region(G, RegionType.ENDING, timestep, false);

                FinalLocation HFinalLocation = new FinalLocation("H_final");
                SimpleStep H = new SimpleStep("H", distributionMapping.get("H").getCorrespondingPartitionedFunction(),
                                List.of(HFinalLocation), List.of(1.0), 2);
                Region HRegion = new Region(H, RegionType.ENDING, timestep, false);

                FinalLocation IFinalLocation = new FinalLocation("I_final");
                SimpleStep I = new SimpleStep("I", distributionMapping.get("I").getCorrespondingPartitionedFunction(),
                                List.of(IFinalLocation), List.of(1.0), 2);
                Region IRegion = new Region(I, RegionType.ENDING, timestep, false);


                FinalLocation LMFinalLocation = new FinalLocation("LM_final");
                Step L = generateNParallelActivities("L", 2, distributionMapping, 2, timestep);

                // SimpleStep M1 = new SimpleStep("M1",
                //                 distributionMapping.get("M1").getCorrespondingPartitionedFunction(),
                //                 List.of(LMFinalLocation), List.of(1.0), 2);
                // SimpleStep M2 = new SimpleStep("M2",
                //                 distributionMapping.get("M2").getCorrespondingPartitionedFunction(),
                //                 List.of(LMFinalLocation), List.of(1.0), 2);
                // L.addNextLocation(M1, 0.3);
                // L.addNextLocation(M2, 0.7);
                
                appendNAlternativeActivities(L, LMFinalLocation, "M", swicthCardinalityM, distributionMapping, 2);

                Region LMRegion = new Region(L, RegionType.ENDING, timestep, false);

                CompositeStep FM = new CompositeStep("FM", CompositeStepType.LAST, 1, timestep);
                FM.addRegions(List.of(FRegion, GRegion, HRegion, IRegion, LMRegion));

                // ACBDFM
                FinalLocation ACBDFMFinalLocation = new FinalLocation("ACBDFM_final");
                FM.addNextLocation(ACBDFMFinalLocation, 1.0);
                AC2BD.addNextLocation(FM, 1.0);
                Region ACBDFMRegion = new Region(AC2BD, RegionType.ENDING, timestep, false);

                // Top Block
                CompositeStep topBlock = new CompositeStep("top", CompositeStepType.LAST, 0, timestep);
                topBlock.addRegions(List.of(ACERegion, ACBDFMRegion));

                FinalLocation topFinalLocation = new FinalLocation("top_final");
                SimpleStep O = new SimpleStep("O", distributionMapping.get("O").getCorrespondingPartitionedFunction(),
                                List.of(topFinalLocation), List.of(1.0), 0);
                SimpleStep N = new SimpleStep("N", distributionMapping.get("N").getCorrespondingPartitionedFunction(),
                                List.of(O), List.of(1.0), 0);
                topBlock.addNextLocation(N, 1.0);
                Region topRegion = new Region(topBlock, RegionType.ENDING, timestep, false);

                CompositeStep finalTopBlock = new CompositeStep("final_top", CompositeStepType.LAST, 0, timestep);
                finalTopBlock.addRegions(List.of(topRegion));

                return finalTopBlock;
        }

        public Step generateModel(Map<String, DistributionParams> distributionMapping, double timestep, int dimA,
                        int dimB) {
                // ACE
                Map<String, DistributionParams> subMapA = new HashMap<>(distributionMapping);
                subMapA.keySet().retainAll(
                                List.of("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10", "A11", "A12",
                                                "A13", "A14", "A15", "A16"));
                List<DistributionParams> paramsA = new ArrayList<>(subMapA.values());
                Step A1 = generateNParallelActivities("A1", dimA, paramsA, 1, timestep);
                FinalLocation ACEFinalLocation = new FinalLocation("AC1_final");
                SimpleStep E = new SimpleStep("E", distributionMapping.get("E").getCorrespondingPartitionedFunction(),
                                List.of(ACEFinalLocation), List.of(1.0), 1);
                SimpleStep C1 = new SimpleStep("C1", distributionMapping.get("C").getCorrespondingPartitionedFunction(),
                                List.of(E), List.of(1.0), 1);
                A1.addNextLocation(C1, 1.0);
                Region ACERegion = new Region(A1, RegionType.ENDING, timestep, false);

                // AC
                Step A2 = generateNParallelActivities("A2", dimA, paramsA, 2, timestep);
                FinalLocation AC2FinalLocation = new FinalLocation("AC2_final");
                SimpleStep C2 = new SimpleStep("C2", distributionMapping.get("C").getCorrespondingPartitionedFunction(),
                                List.of(AC2FinalLocation), List.of(1.0), 2);
                A2.addNextLocation(C2, 1.0);

                // BD
                Map<String, DistributionParams> subMapB = new HashMap<>(distributionMapping);
                subMapA.keySet().retainAll(List.of("B1", "B2", "B3", "B4"));
                List<DistributionParams> paramsB = new ArrayList<>(subMapB.values());
                Step B = generateNParallelActivities("B", dimB, paramsB, 2, timestep);
                FinalLocation BDFinalLocation = new FinalLocation("BD_final");
                SimpleStep D = new SimpleStep("D", distributionMapping.get("D").getCorrespondingPartitionedFunction(),
                                List.of(BDFinalLocation), List.of(1.0), 2);
                B.addNextLocation(D, 1.0);

                // ACBD
                CompositeStep AC2BD = new CompositeStep("AC2BD", CompositeStepType.LAST, 1, timestep);
                Region AC2Region = new Region(A2, RegionType.ENDING, timestep, false);
                Region BDRegion = new Region(B, RegionType.ENDING, timestep, false);
                AC2BD.addRegions(List.of(AC2Region, BDRegion));

                // FM
                FinalLocation FFinalLocation = new FinalLocation("F_final");
                SimpleStep F = new SimpleStep("F", distributionMapping.get("F").getCorrespondingPartitionedFunction(),
                                List.of(FFinalLocation), List.of(1.0), 2);
                Region FRegion = new Region(F, RegionType.ENDING, timestep, false);

                FinalLocation GFinalLocation = new FinalLocation("G_final");
                SimpleStep G = new SimpleStep("G", distributionMapping.get("G").getCorrespondingPartitionedFunction(),
                                List.of(GFinalLocation), List.of(1.0), 2);
                Region GRegion = new Region(G, RegionType.ENDING, timestep, false);

                FinalLocation HFinalLocation = new FinalLocation("H_final");
                SimpleStep H = new SimpleStep("H", distributionMapping.get("H").getCorrespondingPartitionedFunction(),
                                List.of(HFinalLocation), List.of(1.0), 2);
                Region HRegion = new Region(H, RegionType.ENDING, timestep, false);

                FinalLocation IFinalLocation = new FinalLocation("I_final");
                SimpleStep I = new SimpleStep("I", distributionMapping.get("I").getCorrespondingPartitionedFunction(),
                                List.of(IFinalLocation), List.of(1.0), 2);
                Region IRegion = new Region(I, RegionType.ENDING, timestep, false);

                Map<String, DistributionParams> subMapL = new HashMap<>(distributionMapping);
                subMapL.keySet().retainAll(List.of("L1", "L2"));
                List<DistributionParams> paramsL = new ArrayList<>(subMapB.values());
                Step L = generateNParallelActivities("L", 2, paramsL, 2, timestep);
                FinalLocation LMFinalLocation = new FinalLocation("LM_final");

                // SimpleStep M = new SimpleStep("M", getPartitionedFunction("uniform(0,1)"),
                // List.of(LMFinalLocation), List.of(1.0), 2);
                // L.addNextLocation(M, 1.0);

                SimpleStep M1 = new SimpleStep("M1",
                                distributionMapping.get("M1").getCorrespondingPartitionedFunction(),
                                List.of(LMFinalLocation), List.of(1.0), 2);
                SimpleStep M2 = new SimpleStep("M2",
                                distributionMapping.get("M2").getCorrespondingPartitionedFunction(),
                                List.of(LMFinalLocation), List.of(1.0), 2);
                L.addNextLocation(M1, 0.3);
                L.addNextLocation(M2, 0.7);

                Region LMRegion = new Region(L, RegionType.ENDING, timestep, false);

                CompositeStep FM = new CompositeStep("FM", CompositeStepType.LAST, 1, timestep);
                FM.addRegions(List.of(FRegion, GRegion, HRegion, IRegion, LMRegion));

                // ACBDFM
                FinalLocation ACBDFMFinalLocation = new FinalLocation("ACBDFM_final");
                FM.addNextLocation(ACBDFMFinalLocation, 1.0);
                AC2BD.addNextLocation(FM, 1.0);
                Region ACBDFMRegion = new Region(AC2BD, RegionType.ENDING, timestep, false);

                // Top Block
                CompositeStep topBlock = new CompositeStep("top", CompositeStepType.LAST, 0, timestep);
                topBlock.addRegions(List.of(ACERegion, ACBDFMRegion));

                FinalLocation topFinalLocation = new FinalLocation("top_final");
                SimpleStep O = new SimpleStep("O", distributionMapping.get("O").getCorrespondingPartitionedFunction(),
                                List.of(topFinalLocation), List.of(1.0), 0);
                SimpleStep N = new SimpleStep("N", distributionMapping.get("N").getCorrespondingPartitionedFunction(),
                                List.of(O), List.of(1.0), 0);
                topBlock.addNextLocation(N, 1.0);
                Region topRegion = new Region(topBlock, RegionType.ENDING, timestep, false);

                CompositeStep finalTopBlock = new CompositeStep("final_top", CompositeStepType.LAST, 0, timestep);
                finalTopBlock.addRegions(List.of(topRegion));

                return finalTopBlock;
        }

        public Step generateNParallelActivities(String rootName, int numOfParallelActivities,
                        List<DistributionParams> distributionParams, int depth, double timestep) {
                CompositeStep compositeStep = new CompositeStep(rootName, CompositeStepType.LAST, depth, timestep);
                List<Region> regionList = new ArrayList<>();
                for (int i = 0; i < numOfParallelActivities; i++) {
                        FinalLocation finalLocation = new FinalLocation(rootName + "_" + i + "_final");
                        SimpleStep simpleActivity = new SimpleStep(rootName + "_" + i,
                                        distributionParams.get(i).getCorrespondingPartitionedFunction(),
                                        List.of(finalLocation),
                                        List.of(1.0), depth + 1);
                        Region region = new Region(simpleActivity, RegionType.ENDING, timestep, false);
                        regionList.add(region);
                }
                compositeStep.addRegions(regionList);
                return compositeStep;
        }

        public Step generateNParallelActivities(String rootName, int numOfParallelActivities,
                        Map<String, DistributionParams> distributionParams, int depth, double timestep) {
                return generateNParallelActivities(rootName, rootName, numOfParallelActivities, distributionParams,
                                depth, timestep);
        }


        public void appendNAlternativeActivities(Step forkStep, LogicalLocation joinLocation, String rootName, int numOfActivities, Map<String, DistributionParams> distributionParams, int depth){
                double branchingProb = 1./numOfActivities;
                for(int activityNumber = 0; activityNumber < numOfActivities;  activityNumber++){
                        String activityName = rootName + String.valueOf(activityNumber+1);
                        SimpleStep activity = new SimpleStep(activityName,
                                distributionParams.get(activityName).getCorrespondingPartitionedFunction(),
                                List.of(joinLocation), List.of(1.0), depth);
                        forkStep.addNextLocation(activity, branchingProb);
                }
        }

        public Step generateNParallelActivities(String rootName, String keyName, int numOfParallelActivities,
                        Map<String, DistributionParams> distributionParams, int depth, double timestep) {
                CompositeStep compositeStep = new CompositeStep(rootName, CompositeStepType.LAST, depth, timestep);
                List<Region> regionList = new ArrayList<>();
                for (int i = 1; i <= numOfParallelActivities; i++) {
                        FinalLocation finalLocation = new FinalLocation(rootName + String.valueOf(i) + "_final");
                        SimpleStep simpleActivity = new SimpleStep(rootName + String.valueOf(i),
                                        distributionParams.get(keyName + String.valueOf(i))
                                                        .getCorrespondingPartitionedFunction(),
                                        List.of(finalLocation),
                                        List.of(1.0), depth + 1);
                        Region region = new Region(simpleActivity, RegionType.ENDING, timestep, false);
                        regionList.add(region);
                }
                compositeStep.addRegions(regionList);
                return compositeStep;
        }

        public Step generateNParallelSequences(String rootName, String rootKey, int numOfParallelActivities,
                        int sequenceLenght,
                        Map<String, DistributionParams> distributionParams, int depth, double timestep) {

                CompositeStep compositeStep = new CompositeStep(rootName, CompositeStepType.LAST, depth, timestep);
                List<Region> regionList = new ArrayList<>();
                for (int branchIndex = 1; branchIndex <= numOfParallelActivities; branchIndex++) {
                        regionList.add(generateNSequentialActivities(rootName + String.valueOf(branchIndex),
                                        rootKey + String.valueOf(branchIndex),
                                        sequenceLenght,
                                        distributionParams, depth, timestep));
                }
                compositeStep.addRegions(regionList);
                return compositeStep;
        }

        private Region generateNSequentialActivities(String rootName, String rootKey, int sequenceLenght,
                        Map<String, DistributionParams> distributionMapping, int depth, double timestep) {
                FinalLocation finalLocation = new FinalLocation(rootName + "_final");
                LogicalLocation previousLocation = finalLocation;
                for (int activityIndex = sequenceLenght; activityIndex > 0; activityIndex--) {
                        String simpleActivityName = rootName + "." + activityIndex;
                        String simpleActivityKey = rootKey + "." + activityIndex;
                        SimpleStep simpleActivity = new SimpleStep(simpleActivityName,
                                        distributionMapping.get(simpleActivityKey)
                                                        .getCorrespondingPartitionedFunction(),
                                        List.of(previousLocation),
                                        List.of(1.0), depth + 1);
                        previousLocation = simpleActivity;
                }
                return new Region(previousLocation, RegionType.ENDING, timestep, false);
        }

        // Param List Version
        @Deprecated
        public Region generateNSequenceActivities(String rootName, int sequenceLenght,
                        List<DistributionParams> distributionParams, int depth, double timestep) {
                FinalLocation finalLocation = new FinalLocation(rootName + "_final");
                LogicalLocation previousLocation = finalLocation;
                for (int activityIndex = sequenceLenght; activityIndex > 0; activityIndex--) {
                        SimpleStep simpleActivity = new SimpleStep(rootName + "." + activityIndex,
                                        distributionParams.get(activityIndex).getCorrespondingPartitionedFunction(),
                                        List.of(previousLocation),
                                        List.of(1.0), depth + 1);
                        previousLocation = simpleActivity;
                }
                return new Region(previousLocation, RegionType.ENDING, timestep, false);
        }

        public Step generateSequentialModel(double timestep, List<DistributionParams> distributionParams) {
                CompositeStep finalCompositeStep = new CompositeStep("A_final", CompositeStepType.LAST, 0, timestep);
                finalCompositeStep.addRegions(
                                List.of(generateNSequenceActivities("A", 2, distributionParams, 0, timestep)));
                return finalCompositeStep;
        }

        public Step generateSequentialModelUniform(double timestep, List<DistributionParams> distributionParams) {
                CompositeStep finalCompositeStep = new CompositeStep("A_final", CompositeStepType.LAST, 0, timestep);
                finalCompositeStep.addRegions(
                                List.of(generateNSequenceUniformActivities("A", 3, 0, timestep)));
                return finalCompositeStep;
        }

        public Step generateParallelSequencesModel(double timestep, int numOfParallelActivities, int sequenceLenght,
                        Map<String, DistributionParams> distributionParams) {
                return generateNParallelSequences("A1_", "A", numOfParallelActivities, sequenceLenght, distributionParams, 0,
                                timestep);
        }

        public Region generateNSequenceUniformActivities(String rootName, int sequenceLenght, int depth,
                        double timestep) {
                FinalLocation finalLocation = new FinalLocation(rootName + "_final");
                LogicalLocation previousLocation = finalLocation;
                for (int activityIndex = sequenceLenght; activityIndex > 0; activityIndex--) {
                        SimpleStep simpleActivity = new SimpleStep(rootName + "." + activityIndex,
                                        Utils.getPartitionedFunction("uniform(0,1)"),
                                        List.of(previousLocation),
                                        List.of(1.0), depth + 1);
                        previousLocation = simpleActivity;
                }
                return new Region(previousLocation, RegionType.ENDING, timestep, false);
        }

        @Deprecated
        public Step generateSimpleModel(double timestep) {
                FinalLocation AFinalLocation = new FinalLocation("A_final");
                SimpleStep A = new SimpleStep("A", Utils.getPartitionedFunction("uniform(0,1)"),
                                List.of(AFinalLocation),
                                List.of(1.0), 1);
                Region ARegion = new Region(A, RegionType.ENDING, timestep, false);

                CompositeStep finalCompositeStep = new CompositeStep("final_composite", CompositeStepType.LAST, 0,
                                timestep);
                finalCompositeStep.addRegions(List.of(ARegion));

                return finalCompositeStep;
        }

        @Deprecated
        public Step generateParallelSimpleModel(double timestep) {
                FinalLocation AFinalLocation = new FinalLocation("A_final");
                SimpleStep A = new SimpleStep("A", Utils.getPartitionedFunction("uniform(0,1)"),
                                List.of(AFinalLocation),
                                List.of(1.0), 1);
                Region ARegion = new Region(A, RegionType.ENDING, timestep, false);

                FinalLocation BFinalLocation = new FinalLocation("B_final");
                SimpleStep B = new SimpleStep("B", Utils.getPartitionedFunction("uniform(0,1)"),
                                List.of(BFinalLocation),
                                List.of(1.0), 1);
                Region BRegion = new Region(B, RegionType.ENDING, timestep, false);

                CompositeStep finalCompositeStep = new CompositeStep("final_composite", CompositeStepType.LAST, 0,
                                timestep);
                finalCompositeStep.addRegions(List.of(ARegion, BRegion));

                return finalCompositeStep;
        }

        @Deprecated
        public Step generateParallelSimpleModelTruncatedExp(double timestep) {
                FinalLocation AFinalLocation = new FinalLocation("A_final");
                SimpleStep A = new SimpleStep("A", Utils.getPartitionedFunction("truncated-exp(1,0,2)"),
                                List.of(AFinalLocation), List.of(1.0), 1);
                Region ARegion = new Region(A, RegionType.ENDING, timestep, false);

                FinalLocation BFinalLocation = new FinalLocation("B_final");
                SimpleStep B = new SimpleStep("B", Utils.getPartitionedFunction("truncated-exp(1,0,2)"),
                                List.of(BFinalLocation), List.of(1.0), 1);
                Region BRegion = new Region(B, RegionType.ENDING, timestep, false);

                CompositeStep finalCompositeStep = new CompositeStep("final_composite", CompositeStepType.LAST, 0,
                                timestep);
                finalCompositeStep.addRegions(List.of(ARegion, BRegion));

                return finalCompositeStep;
        }

        public int getParallelismA() {
                return parallelismA;
        }

        public void setParallelismA(int parallelismA) {
                this.parallelismA = parallelismA;
        }

        public int getParallelismB() {
                return parallelismB;
        }

        public void setParallelismB(int parallelismB) {
                this.parallelismB = parallelismB;
        }

        public int getSequenceA() {
                return sequenceA;
        }

        public void setSequenceA(int sequenceA) {
                this.sequenceA = sequenceA;
        }

        public int getSequenceB() {
                return sequenceB;
        }

        public void setSequenceB(int sequenceB) {
                this.sequenceB = sequenceB;
        }

        public int getSwicthCardinalityM() {
                return swicthCardinalityM;
        }

        public void setSwicthCardinalityM(int swicthCardinalityM) {
                this.swicthCardinalityM = swicthCardinalityM;
        }

}
