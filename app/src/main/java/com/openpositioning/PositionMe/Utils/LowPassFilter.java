package com.openpositioning.PositionMe.Utils;

public final class LowPassFilter {

    private static final float ALPHA = 0.8f;

    private LowPassFilter() {
    }

    public static float[] applyFilter(float[] inputs, float[] outputs){
        if (outputs == null) return inputs;

        for (int i = 0; i < inputs.length; i++){
            outputs[i] = outputs[i] + ALPHA * (inputs[i] - outputs[i]);
        }
        return outputs;
    }
}
