package frc.robot.generic.elevators;

import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.SparkBase.ControlType;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkClosedLoopController;
import com.revrobotics.spark.SparkLowLevel;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig;
import com.revrobotics.spark.config.SparkMaxConfig;

public class GenericElevatorSystemIOSparkMax implements GenericElevatorSystemIO {
  private final SparkMax[] motors;
  private final RelativeEncoder encoder;
  private double restingAngle;
  private final double reduction;
  private SparkBaseConfig config;
  private SparkClosedLoopController controller;

  public GenericElevatorSystemIOSparkMax(
      int[] id,
      int currentLimitAmps,
      double restingAngle,
      boolean invert,
      boolean brake,
      double reduction,
      double kP,
      double kI,
      double kD) {
    this.reduction = reduction;
    this.restingAngle = restingAngle;
    motors = new SparkMax[id.length];
    config =
        new SparkMaxConfig()
            .smartCurrentLimit(currentLimitAmps)
            .inverted(invert)
            .idleMode(brake ? SparkBaseConfig.IdleMode.kBrake : SparkBaseConfig.IdleMode.kCoast);
    config
        .closedLoop
        .pid(kP, kI, kD)
        .maxMotion
        .maxAcceleration(6000)
        .maxVelocity(6000)
        .allowedClosedLoopError(0.2);

    for (int i = 0; i < id.length; i++) {
      motors[i] = new SparkMax(id[i], SparkLowLevel.MotorType.kBrushless);

      if (i == 0)
        motors[i].configure(config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
      else
        motors[i].configure(
            new SparkMaxConfig().apply(config).follow(motors[0]),
            ResetMode.kResetSafeParameters,
            PersistMode.kPersistParameters);
    }
    encoder = motors[0].getEncoder();
    controller = motors[0].getClosedLoopController();
  }

  @Override
  public void updateInputs(GenericElevatorSystemIOInputs inputs) {
    inputs.positionMeters = encoder.getPosition();
    inputs.velocityMetersPerSec = encoder.getVelocity();
    inputs.appliedVoltage = motors[0].getAppliedOutput() * motors[0].getBusVoltage();
    inputs.supplyCurrentAmps = motors[0].getOutputCurrent();
    inputs.tempCelsius = motors[0].getMotorTemperature();
  }

  @Override
  public void runPosition(double position) {
    controller.setReference(position, ControlType.kMAXMotionPositionControl);
  }
}
