package it.unifi.hierarchical.epew25;

import it.unifi.hierarchical.epew25.DistributionParams;
import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.Composite;
import org.oristool.eulero.modeling.ModelFactory;
import org.oristool.eulero.modeling.Simple;
import org.oristool.eulero.modeling.stochastictime.UniformTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EuleroModelBuilder {

    private int parallelismA = 2;
    private int parallelismB = 2;
    private int sequenceA = 1;
    private int sequenceB = 1;
    private int swicthCardinalityM = 2;


    public Activity generateSipthModel(Map<String, DistributionParams> paramsMap) {


        Activity A = generateBlockofParallelSequences("A", "A", parallelismA, sequenceA, paramsMap);
        Activity B = generateBlockofParallelSequences("B", "B", parallelismB, sequenceB, paramsMap);

        Activity C = new Simple("C", paramsMap.get("C").getCorrespondingStochatsticTimeDistribution());
        Activity D = new Simple("D", paramsMap.get("D").getCorrespondingStochatsticTimeDistribution());

        Composite AC = ModelFactory.sequence(A, C);
        Composite BD = ModelFactory.sequence(B, D);

        Activity F = new Simple("F", paramsMap.get("F").getCorrespondingStochatsticTimeDistribution());
        Activity G = new Simple("G", paramsMap.get("G").getCorrespondingStochatsticTimeDistribution());
        Activity H = new Simple("H", paramsMap.get("H").getCorrespondingStochatsticTimeDistribution());
        Activity I = new Simple("I", paramsMap.get("I").getCorrespondingStochatsticTimeDistribution());

        Activity L1 = new Simple("L1", paramsMap.get("L1").getCorrespondingStochatsticTimeDistribution());
        Activity L2 = new Simple("L2", paramsMap.get("L2").getCorrespondingStochatsticTimeDistribution());
        Composite L = ModelFactory.forkJoin(L1, L2);

        Activity M = generateBlockofAlternativeActivities("M","M", swicthCardinalityM , paramsMap);

        Composite LM = ModelFactory.sequence(L, M);

        Composite FM = ModelFactory.forkJoin(F, G, H, I, LM);

        FM.addPrecondition(AC);
        FM.addPrecondition(BD);

        Activity E = new Simple("E", paramsMap.get("E").getCorrespondingStochatsticTimeDistribution());

        E.addPrecondition(AC);

        Composite dag = ModelFactory.DAG(AC, BD, E, FM);

        Activity N = new Simple("N", paramsMap.get("N").getCorrespondingStochatsticTimeDistribution());
        Activity O = new Simple("O", paramsMap.get("O").getCorrespondingStochatsticTimeDistribution());

        Composite NO = ModelFactory.sequence(N, O);
        Composite model = ModelFactory.sequence(dag, NO);

        return model;
    }

    private Activity generateBlock(String baseName, int numOfParallelActivities,
            Map<String, DistributionParams> paramsMap) {
        return generateBlock(baseName, baseName, numOfParallelActivities, paramsMap);
    }

    private Activity generateBlock(String baseName, String paramKey, int numOfParallelActivities,
            Map<String, DistributionParams> paramsMap) {
        if (numOfParallelActivities <= 0) {
            throw new IllegalArgumentException("Number of parallel activities must be positive");
        }
        List<Activity> activities = new ArrayList<>();
        for (int i = 1; i <= numOfParallelActivities; i++) {
            String activityName = baseName + "_" + i;
            DistributionParams params = paramsMap.get(paramKey + i);
            if (params == null) {
                throw new IllegalArgumentException("Parameters not found for activity: " + activityName);
            }
            Activity activity = new Simple(activityName, params.getCorrespondingStochatsticTimeDistribution());
            activities.add(activity);
        }

        return ModelFactory.forkJoin(activities.toArray(new Activity[0]));
    }

    private Activity generateBlockofParallelSequences(String baseName, String paramKey, int numOfParallelActivities, int sequencesLenght, Map<String, DistributionParams> paramsMap){
        if (numOfParallelActivities <= 0) {
            throw new IllegalArgumentException("Number of parallel activities must be positive");
        }
        if (sequencesLenght <= 0) {
            throw new IllegalArgumentException("Lenght of sequences must be positive");
        }

        List<Activity> sequences = new ArrayList<>();
        for (int i = 1; i <= numOfParallelActivities; i++) {
            List<Activity> activities = new ArrayList<>();
            for(int j = 1; j <= sequencesLenght; j++){
                String activityName = baseName + "_" + i + "." + j;
                DistributionParams params = paramsMap.get(paramKey + i + "."  + j);
                if (params == null) {
                    throw new IllegalArgumentException("Parameters not found for activity: " + activityName);
                }
                Activity activity = new Simple(activityName, params.getCorrespondingStochatsticTimeDistribution());
                activities.add(activity);
            }
            sequences.add(ModelFactory.sequence(activities.toArray(new Activity[0])));
        }
        return ModelFactory.forkJoin(sequences.toArray(new Activity[0]));
    }

    private Activity generateBlockofAlternativeActivities(String baseName, String paramKey, int numOfAlternativeActivities, Map<String, DistributionParams> paramsMap){
        if (numOfAlternativeActivities <= 0) {
            throw new IllegalArgumentException("Number of alternative activities must be positive");
        }

        List<Activity> activities = new ArrayList<>();
        for(int i =1; i <= numOfAlternativeActivities; i++){
            String activityName = baseName + i;
            String activityKey = paramKey + i;
            Activity alternativeAvivity = new Simple(activityName, paramsMap.get(activityKey).getCorrespondingStochatsticTimeDistribution());
            activities.add(alternativeAvivity);
        }
       return ModelFactory.XOR(Collections.nCopies(numOfAlternativeActivities, 1./numOfAlternativeActivities), activities.toArray(new Activity[0]));
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
