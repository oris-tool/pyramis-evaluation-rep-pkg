package it.unifi.hierarchical.epew25;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.*;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Utils {


    public static PartitionedFunction getPartitionedFunction(String arisingPDF) {
        String typePDF = arisingPDF.toLowerCase().replaceAll("\\s*\\([^()]*\\)\\s*", "");
        String arguments = arisingPDF.substring(typePDF.length() + 1, arisingPDF.length() - 1);
        String[] args;

        switch (typePDF) {
            case "erlang":
                args = arguments.split(",");
                double lambda = checkDivision(args[1]);
                return new Erlang(Variable.X, Integer.parseInt(args[0]), new BigDecimal(lambda));
            case "dirac":
                return GEN.newDeterministic(new BigDecimal(arguments));
            case "exp":
                double rate = checkDivision(arguments);
                return new EXP(Variable.X, new BigDecimal(rate));
            case "uniform":
                args = arguments.split(",");
                return GEN.newUniform(new OmegaBigDecimal(args[0]), new OmegaBigDecimal(args[1]));
            case "gaussian":
                args = arguments.split(",");
                double factor = Math.sqrt(3 * Double.parseDouble(args[1]));
                String a = "" + (Double.parseDouble(args[0]) - factor);
                String b = "" + (Double.parseDouble(args[0]) + factor);
                return GEN.newUniform(new OmegaBigDecimal(a), new OmegaBigDecimal(b));
            case "truncated-exp":
                args = arguments.split(",");
                String truncated_rate = args[0];
                String truncated_eft = args[1];
                String truncated_lft = args[2];
//                GEN gen = GEN.newTruncatedExp(Variable.X, new BigDecimal(truncated_rate), new OmegaBigDecimal(truncated_eft), new OmegaBigDecimal(truncated_lft));
//                gen.normalize();
                return generateTruncatedExp(Variable.X, new BigDecimal(truncated_rate), new OmegaBigDecimal(truncated_eft), new OmegaBigDecimal(truncated_lft));
            case "expoly":
                args = arguments.split(",");
                String density = args[0];
                OmegaBigDecimal eft = new OmegaBigDecimal(args[1]);
                OmegaBigDecimal lft = new OmegaBigDecimal(args[2]);
                if (Expolynomial.isValid(density))
                    return GEN.newExpolynomial(density, eft, lft);
                else
                    throw new UnsupportedOperationException("Function not well formed");
            case "piecewise":
                String[] functs = arguments.split(";");
                List<GEN> functions = new ArrayList<>();
                for (String funct : functs) {
                    args = funct.split(",");
                    String densityi = args[0];
                    OmegaBigDecimal efti = new OmegaBigDecimal(args[1]);
                    OmegaBigDecimal lfti = new OmegaBigDecimal(args[2]);
                    if (Expolynomial.isValid(densityi)) {
                        functions.add(GEN.newExpolynomial(densityi, efti, lfti));
                    } else
                        throw new UnsupportedOperationException("Function not well formed");
                }
                return new PartitionedGEN(functions);

            default:
                throw new UnsupportedOperationException("PDF not supported");
        }
    }

    private static double checkDivision(String arg) {
        if (arg.contains("/")) {
            String[] factors = arg.split("/");
            Double num = Double.parseDouble(factors[0]);
            Double denom = Double.parseDouble(factors[1]);
            return num / denom;
        } else {
            return Double.parseDouble(arg);
        }
    }

    public static PartitionedFunction generateTruncatedExp(Variable v, BigDecimal rate,
                                      OmegaBigDecimal eft, OmegaBigDecimal lft) {
        double c = rate.doubleValue() > 0 ? eft.doubleValue() : lft.doubleValue();
        StochasticTransitionFeature stochasticTransitionFeature = StochasticTransitionFeature.newExpolynomial(
                Math.abs(rate.doubleValue()) * Math.exp(rate.doubleValue() * c) / (1 - Math.exp(-Math.abs(rate.doubleValue()) * (lft.doubleValue() - eft.doubleValue()))) + " * Exp[" + (-rate.doubleValue()) + " x]",
                new OmegaBigDecimal(eft.toString()), new OmegaBigDecimal(lft.toString()));
        PartitionedFunction density = stochasticTransitionFeature.density();

//        GEN densityGen = (GEN) density;
//        OmegaBigDecimal integrate = densityGen.integrateOverDomain();
//        System.out.println("Integrate over domain: " + integrate);
        return stochasticTransitionFeature.density();
    }


    public static double jsDistance(double[] pdf, double[] otherPDF) {
        if (pdf.length != otherPDF.length)
            throw new IllegalArgumentException("Should have the same number of samples");

        double result = 0.0;
        for (int t = 0; t < otherPDF.length; ++t) {
            double x = pdf[t];
            double y = otherPDF[t];
            double m = (x + y)/2.0;
            result += (klDivergence(x, m) + klDivergence(y, m)) / 2.0;
        }

        return result * 0.01;
    }

    public static double klDivergence(double px, double py) {
        if (px > 0.0 && py > 0.0) {
            return px * Math.log(px / py);
        } else {
            return 0.0;
        }
    }

    public static double[] subsampleOLD(double[] originalArray, int factor) {
        int newLength = originalArray.length / factor;
        double[] result = new double[newLength];

        for (int i = 0; i < newLength; i++) {
            result[i] = originalArray[i * factor];
        }
        return result;
    }

    public static double[] subsample(double[] originalArray, double originalStep, double newStep) {
        if (newStep == originalStep) {
            return originalArray;
        }
        if (newStep < originalStep) {
            throw new IllegalArgumentException("New step should be greater than the original step");
        }
        int samplingInterval = (int) Math.round(newStep / originalStep);

        int newSize = (originalArray.length + samplingInterval - 1) / samplingInterval;

        double[] subsampledArray = new double[newSize];

        for (int i = 0; i < newSize; i++) {
            subsampledArray[i] = originalArray[i * samplingInterval];
        }

        return subsampledArray;
    }


}
