package edu.reins.mongocloud.tmp.agent;

public interface Predictor {
    double predict(String metric, long time, double value);
}
