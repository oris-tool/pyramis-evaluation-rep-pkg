package it.unifi.hierarchical.epew25;

import org.oristool.eulero.modeling.stochastictime.StochasticTime;
import org.oristool.eulero.modeling.stochastictime.TruncatedExponentialTime;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


public record DistributionParams(Double rate, Double eft, Double lft) {

    public PartitionedFunction getCorrespondingPartitionedFunction() {
        return Utils.getPartitionedFunction("truncated-exp(" + this.rate + "," + this.eft + "," + this.lft + ")");
    }


    public StochasticTime getCorrespondingStochatsticTimeDistribution() {
        return new TruncatedExponentialTime(this.eft, this.lft, this.rate);
    }

}
