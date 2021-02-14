package com.keykeepers.boilbubble.server.fluids;

import net.minecraft.fluid.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.HashMap;

/**
 * A ThermalTank extends the functionality of FractionalTank to add temperature changes to its fluids. The enclosing
 * class can set an external temperature for each tank, which represents the prevailing temperature that the tank will
 * settle at due to the biome temperature, the presence of heat sources around the block, the thermostat of a built-in
 * heater, etc.
 *
 * Every tick, the tank's current temperature -- specifically, the temperature of the enclosing walls and not the
 * fluids within -- will gain or lose thermal energy as it moves toward the external temperature. Then, the tank is
 * compared to the fluid at the bottom of the tank and trades thermal energy with it if they are at different
 * temperatures. This process continues between a layer of fluid and the next highest layer until the top layer has
 * been traded with.
 *
 * The effective temperature of the tank exterior and each layer of fluid depends on two factors assigned to each:
 * its specific heat, and its thermal conductivity. Specific heat affects the rate at which thermal energy added to
 * or removed from the subject translates into a change of temperature. The equation below:
 *
 * dT = e / s / V
 *
 * Where 'dT' is the temperature adjustment from default (per fluid attributes, or 300 kelvin), 'e' is the the amount
 * of thermal energy added or taken, 's' is the specific heat value of the material, and 'V' is the volume of material
 * in mB. The higher the value of specific heat, the more energy it takes to raise the temperature of a given volume.
 *
 * Thermal conductivity determines how quickly thermal energy migrates from a higher temperature material to a lower
 * temperature one.
 *
 * de = (T1 - T2) * ((C1 + C2) / 2)
 *
 * Where 'T1' and 'C1' are the temperature and thermal conductivity of the first material, and 'T2' and 'C2' are the
 * temperature and conductivity of the material that is tested against.
 */
public abstract class ThermalTank extends FractionalTank {
  private final TankThermals[] tankThermals;

  public ThermalTank(int tanks, int[] capacities, int[] thermalConductivities) {
    super(tanks, capacities);

    int generalThermConduc = -1; // Sentinel value for using per-tank conductivities
    if (thermalConductivities == null || thermalConductivities.length == 0)
      generalThermConduc = 5;
    else if (thermalConductivities.length == 1)
      generalThermConduc = thermalConductivities[0];
    else if (thermalConductivities.length != tanks)
      throw new IllegalArgumentException("Moust provide no thermal conductivities, one conductivity or as many as tanks.");

    tankThermals = new TankThermals[tanks];
    for (int i = 0; i < tanks; i++) {
      tankThermals[i] = new TankThermals(i,
          generalThermConduc == -1 ? thermalConductivities[i] : generalThermConduc);
    }
  }

  public ThermalTank(int tanks, int... capacities) {
    this(tanks, capacities, null);
  }

  public final int targetTemperature(int tank) { return tankThermals[tank].targetTemperature; }

  public final ThermalTank targetTemperature(int tank, int temperature) {
    tankThermals[tank].targetTemperature = temperature;
    return this;
  }

  public final int currentTemperature(int tank) {
    return tankThermals[tank].currentTemperature();
  }

  public final int fluidTemperature(int tank, @Nonnull Fluid fluid) {
    if (!tankThermals[tank].containsKey(fluid))
      throw new IllegalArgumentException(fluid.toString() + " not present in tank " + tank);
    double defaultTemperature = fluid.getAttributes().getTemperature();
    double thermalEnergy = tankThermals[tank].get(fluid);
    int count = getFluidStack(tank, fluid).getAmount();
    return (int) (thermalEnergy / count + defaultTemperature);
  }

  public final float fluidConductivity(Fluid fluid) {
    // TODO
    return 1.0f;
  }

  public final void processThermal() {
    for (int i = 0; i < getTanks(); i++) {
      TankThermals thermal = tankThermals[i];

      // External to tank
      int tankTemperature = thermal.currentTemperature();
      if (tankTemperature != thermal.targetTemperature) {
        int difference = thermal.targetTemperature - tankTemperature;
        int energyChange = (int) (difference * thermal.conductivity);
        int maxChange = difference * thermal.tankShell;
        if (tankTemperature < thermal.targetTemperature)
          thermal.tankThermalEnergy += Math.min(Math.max(energyChange, 1), maxChange);
        else
          thermal.tankThermalEnergy += Math.max(Math.min(energyChange, -1), maxChange);
      }

      FluidStack[] contents = contents(i);
      tankTemperature = thermal.currentTemperature();

      // Tank to first fluid
      if (thermal.size() < 1)
        continue;
      Fluid priorFluid = contents[0].getFluid();
      int priorFluidTemperature = fluidTemperature(i, priorFluid);
      if (priorFluidTemperature != tankTemperature) {
        int difference = tankTemperature - priorFluidTemperature;
        int energyChange = (int) (difference * (thermal.conductivity + fluidConductivity(priorFluid)) / 2);
        int fluidThermalEnergy = thermal.get(priorFluid);
        int actualChange;
        if (priorFluidTemperature < tankTemperature)
          actualChange = Math.max(energyChange, 1);
        else
          actualChange = Math.min(energyChange, -1);
        fluidThermalEnergy += actualChange;
        thermal.put(priorFluid, fluidThermalEnergy);
        thermal.tankThermalEnergy -= actualChange;
      }

      // Iterate up through fluids in the tank
      priorFluidTemperature = fluidTemperature(i, priorFluid);
      for (int j = 1; j < contents.length; j++) {
        Fluid nextFluid = contents[j].getFluid();
        int nextFluidTemperature = fluidTemperature(i, nextFluid);
        int difference = priorFluidTemperature - nextFluidTemperature;
        int energyChange = (int) (difference * (fluidConductivity(priorFluid) + fluidConductivity(nextFluid)) / 2);
        int priorThermalEnergy = thermal.get(priorFluid);
        int nextThermalEnergy = thermal.get(nextFluid);
        int actualChange;
        if (nextFluidTemperature < priorFluidTemperature)
          actualChange = Math.max(energyChange, 1);
        else
          actualChange = Math.min(energyChange, -1);
        nextThermalEnergy += actualChange;
        thermal.put(nextFluid, nextThermalEnergy);
        priorThermalEnergy -= actualChange;
        thermal.put(priorFluid, priorThermalEnergy);
        priorFluid = nextFluid;
        priorFluidTemperature = fluidTemperature(i, priorFluid);
      }

      // Process water and aqueous solution states.
      // TODO
    }
  }

  private class TankThermals extends HashMap<Fluid, Integer> {
    private int targetTemperature;
    private int tankThermalEnergy;
    private final int tankShell;
    private final float conductivity;

    private TankThermals(int tank, int conductivity) {
      if (conductivity < 1)
        throw new IllegalArgumentException("Tank " + tank + " thermal conductivity must be positive.");
      targetTemperature = 300;
      this.conductivity = conductivity;

      // Estimated volume of tank exterior for calculation purposes
      // Assume a cube that can enclose the capacity.
      int capacity = getTankCapacity(tank);
      double length = Math.pow(capacity, 1.0 / 3.0) / 0.95;
      tankShell = (int) (Math.pow(length, 3) - capacity);

      tankThermalEnergy = 0;
    }

    private int currentTemperature() { return 300 + tankThermalEnergy / tankShell; }
  }
}
