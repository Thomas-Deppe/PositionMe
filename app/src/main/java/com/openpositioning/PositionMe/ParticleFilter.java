package com.openpositioning.PositionMe;

import com.google.android.gms.maps.model.LatLng;
import com.openpositioning.PositionMe.sensors.SensorFusion;

import java.util.Random;

public class ParticleFilter {
    // Constants, may need to be tuned
    private static final int NUM_PARTICLES = 500;
    private static final double PARTICLE_STD_DEV = 0.0005; // Standard deviation for generating particles

    // Parameters
    private Particle[] particles;
    private final Random random;
    private final double refLatitude;
    private final double refLongitude;
    private final double refAlt;
    private final double initialTrueEasting;
    private final double initialTrueNorthing;
    private final double initialTrueAlt;


    public ParticleFilter(double startLat, double startLong, double startAlt) {
        this.refLatitude = startLat;
        this.refLongitude = startLong;
        this.refAlt = startAlt;

        double[] enucoords = CoordinateTransform.geodeticToEnu(refLatitude, refLongitude, refAlt, refLatitude, refLongitude, refAlt);
        this.initialTrueEasting = enucoords[0];
        this.initialTrueNorthing = enucoords[1];
        this.initialTrueAlt = enucoords[2];

        particles = new Particle[NUM_PARTICLES];
        random = new Random();

        initializeParticles();
    }

    private void initializeParticles() {
        for (int i = 0; i < NUM_PARTICLES; i++) {
            double easting = initialTrueEasting + (random.nextGaussian() * PARTICLE_STD_DEV);
            double northing = initialTrueNorthing + (random.nextGaussian() * PARTICLE_STD_DEV);
            particles[i] = new Particle(easting, northing, 1.0 / NUM_PARTICLES);
        }
    }

    private void updateMotionModel() {
        // Update particles based on random walk motion model
        for (Particle particle : particles) {
            particle.update(random.nextGaussian(), random.nextGaussian());
        }
    }

    private void updateMeasurementModel(double measuredEast, double measuredNorth) {
        // Update particle weights based on measurement likelihood
        for (Particle particle : particles) {
            double probability = calculateDistanceProbability(measuredEast, measuredNorth, particle.getEasting(), particle.getNorthing());
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

    private double calculateDistanceProbability(double measuredEast, double measuredNorth, double particleEast, double particleNorth) {
        // Example of a simple distance-based likelihood calculation
        double distance = Math.sqrt(Math.pow(measuredEast - particleEast, 2) + Math.pow(measuredNorth - particleNorth, 2));
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
            newParticles[i] = new Particle(particles[index].getEasting(), particles[index].getNorthing(), 1.0 / NUM_PARTICLES);
        }

        particles = newParticles;
    }

    public void update(double measuredLat, double measuredLong) {
        updateMotionModel();
        double[] enucoords = CoordinateTransform.geodeticToEnu(measuredLat,measuredLong,refAlt,refLatitude,refLongitude,refAlt);
        updateMeasurementModel(enucoords[0], enucoords[1]);
        resampleParticles();

        SensorFusion.getInstance().notifyParticleUpdate(predict());
//        return predict();
    }

    public LatLng predict() {
        // Not sure if this is needed, depends on how the calls are made
        double estimatedEasting = 0;
        double estimatedNorthing = 0;

        for (Particle particle : particles) {
            estimatedEasting += particle.getEasting() * particle.getWeight();
            estimatedNorthing += particle.getNorthing() * particle.getWeight();
        }

        return CoordinateTransform.enuToGeodetic(estimatedEasting,estimatedNorthing,refAlt,initialTrueEasting,initialTrueNorthing, initialTrueAlt);
    }
}

class Particle {
    private double easting;
    private double northing;
    private double weight;

    public Particle(double easting, double northing, double weight) {
        this.easting = easting;
        this.northing = northing;
        this.weight = weight;
    }

    public void update(double deltaEasting, double deltaNorthing) {
        this.easting += deltaEasting;
        this.northing += deltaNorthing;
    }

    public double getEasting() {
        return easting;
    }

    public double getNorthing() {
        return northing;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
