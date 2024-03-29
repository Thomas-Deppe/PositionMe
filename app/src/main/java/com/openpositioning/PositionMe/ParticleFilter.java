package com.openpositioning.PositionMe;
import com.google.android.gms.maps.model.LatLng;
import com.openpositioning.PositionMe.sensors.SensorFusion;

import java.util.Random;

public class ParticleFilter {
    // Constants, may need to be tuned
    private static final int NUM_PARTICLES = 1000;
    private static final double PARTICLE_STD_DEV = 0.01; // Standard deviation for generating particles

    // Parameters
    private Particle[] particles;
    private final Random random;
    private final double initialTrueLatitude;
    private final double initialTrueLongitude;

    public ParticleFilter(double initialTrueLatitude, double initialTrueLongitude) {
        this.initialTrueLatitude = initialTrueLatitude;
        this.initialTrueLongitude = initialTrueLongitude;
        particles = new Particle[NUM_PARTICLES];
        random = new Random();

        initializeParticles();
    }

    private void initializeParticles() {
        for (int i = 0; i < NUM_PARTICLES; i++) {
            double latitude = initialTrueLatitude + (random.nextGaussian() * PARTICLE_STD_DEV);
            double longitude = initialTrueLongitude + (random.nextGaussian() * PARTICLE_STD_DEV);
            particles[i] = new Particle(latitude, longitude, 1.0 / NUM_PARTICLES);
        }
    }

    private void updateMotionModel() {
        // Update particles based on random walk motion model
        for (Particle particle : particles) {
            particle.update(random.nextGaussian(), random.nextGaussian());
        }
    }

    private void updateMeasurementModel(double measuredLatitude, double measuredLongitude) {
        // Update particle weights based on measurement likelihood
        for (Particle particle : particles) {
            double probability = calculateDistanceProbability(measuredLatitude, measuredLongitude, particle.getLatitude(), particle.getLongitude());
            particle.setWeight(particle.getWeight() * probability);
        }

        // Normalize weights
        double totalWeight = 0;
        for (Particle particle : particles) {
            totalWeight += particle.getWeight();
        }
        for (Particle particle : particles) {
            particle.setWeight(particle.getWeight() / totalWeight);
        }
    }

    private double calculateDistanceProbability(double measuredLatitude, double measuredLongitude, double particleLatitude, double particleLongitude) {
        // Example of a simple distance-based likelihood calculation
        double distance = Math.sqrt(Math.pow(measuredLatitude - particleLatitude, 2) + Math.pow(measuredLongitude - particleLongitude, 2));
        return Math.exp(-0.5 * distance);
    }

    private void resampleParticles() {
        // Resample particles based on their weights
        Particle[] newParticles = new Particle[NUM_PARTICLES];
        double[] cumulativeWeights = new double[NUM_PARTICLES];

        cumulativeWeights[0] = particles[0].getWeight();
        for (int i = 1; i < NUM_PARTICLES; i++) {
            cumulativeWeights[i] = cumulativeWeights[i - 1] + particles[i].getWeight();
        }

        for (int i = 0; i < NUM_PARTICLES; i++) {
            double sample = random.nextDouble();
            int index = 0;
            for (int j = 0; j < NUM_PARTICLES - 1; j++) {
                if (sample < cumulativeWeights[j]) {
                    index = j;
                    break;
                }
            }
            newParticles[i] = new Particle(particles[index].getLatitude(), particles[index].getLongitude(), 1.0 / NUM_PARTICLES);
        }

        particles = newParticles;
    }

    public void update(double measuredLatitude, double measuredLongitude) {
        updateMotionModel();
        updateMeasurementModel(measuredLatitude, measuredLongitude);
        resampleParticles();

        // updates the observer - notifies the RecordingFragment
        SensorFusion.getInstance().notifyFusionUpdate(predict());
    }

    public LatLng predict() {
        // Not sure if this is needed, depends on how the calls are made
        double estimatedLatitude = 0;
        double estimatedLongitude = 0;

        for (Particle particle : particles) {
            estimatedLatitude += particle.getLatitude() * particle.getWeight();
            estimatedLongitude += particle.getLongitude() * particle.getWeight();
        }

        return new LatLng(estimatedLatitude, estimatedLongitude);
    }
}

class Particle {
    private double latitude;
    private double longitude;
    private double weight;

    public Particle(double latitude, double longitude, double weight) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.weight = weight;
    }

    public void update(double deltaLatitude, double deltaLongitude) {
        this.latitude += deltaLatitude;
        this.longitude += deltaLongitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
