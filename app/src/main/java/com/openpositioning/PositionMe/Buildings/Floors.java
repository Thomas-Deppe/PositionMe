package com.openpositioning.PositionMe.Buildings;

/**
 * A simple Enum class, used as a type safe and readable method to denote which floors are available. Integer values, could potentially lead to the
 * application attempting to set floors it does not support.
 *
 * @author Thomas Deppe
 */
public enum Floors {
    Lower_Ground,
    Ground,
    First,
    Second,
    Third
}
