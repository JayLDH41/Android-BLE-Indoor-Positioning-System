package my.edu.utar.fyp;

import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class KalmanFilterHelper {
    private KalmanFilter kalmanFilter;
    private RealVector state;


    //constructor
    public KalmanFilterHelper() {
        double[][] stateTransition = {{1.0}};   //how the state evolves from one to another
        double[][] control = {{0.0}};   //control set to 0 as there aren't any external controls
        double[][] processNoise = {{0.01}}; //noises during process
        double[][] initialErrorCovariance = {{1.0}};    //initial error / covariance of state estimate
        DefaultProcessModel pModel = new DefaultProcessModel(stateTransition, control, processNoise);

        double[][] measure = {{1.0}};   //how the state corresponds to the observed measurements
        double[][] measurementNoise = {{0.01}}; //noise in measurements (how much different from actual measurements)
        DefaultMeasurementModel mModel = new DefaultMeasurementModel(measure, measurementNoise);

        kalmanFilter = new KalmanFilter(pModel, mModel);
    }

    //smoothen the rssi value only after the rssi has been filtered for the first time
    //to make sure that the internal state is not reset
    public int smoothenRssi(int rssi) {
        kalmanFilter.predict();

        RealVector actualVector = new ArrayRealVector(new double[]{rssi});
        kalmanFilter.correct(actualVector);

        RealVector smoothedState = kalmanFilter.getStateEstimationVector();

        double result = smoothedState.getEntry(0);
        return (int)result;
    }

    //for first time rssi filtering only
    //to make sure that the internal state is not reset
    public int smoothenRssiFirstTime(int rssi) {
        RealVector firstState = new ArrayRealVector(new double[]{rssi});

        kalmanFilter.predict();

        kalmanFilter.correct(firstState);

        RealVector smoothedState = kalmanFilter.getStateEstimationVector();

        double result = smoothedState.getEntry(0);
        return (int)result;
    }
}
