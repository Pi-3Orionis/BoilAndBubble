package com.keykeepers.boilbubble.server.fluids;

import net.minecraft.fluid.Fluid;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import java.util.TreeMap;

/**
 * A FractionalTank is a module designed to hold one or more fluids, sorted with the heaviest at the
 * bottom. You must always specify a number of tanks as 1 or more. You may optionally specify capacity
 * values for its tanks. You may specify one capacity, which will become the maximum for every tank,
 * or you may specify as many capacities as tanks. If no capacity is supplied, the capacity of all
 * tanks are set to the volume of one bucket.
 */
public class FractionalTank implements IFluidHandler {
  private final InternalTank[] tanks;

  public FractionalTank(int tanks, int... capacities) {
    if (tanks < 1)
      throw new IllegalArgumentException("Number of tanks must be positive.");
    int generalCapacity = -1; // Sentinal value for using per-tank capacity values
    if (capacities == null || capacities.length == 0)
      generalCapacity = FluidAttributes.BUCKET_VOLUME;
    else if (capacities.length == 1)
      generalCapacity = capacities[0];
    else if (capacities.length != tanks)
      throw new IllegalArgumentException("Must provide no capacity, one capacity or as many capacities as tanks.");

    this.tanks = new InternalTank[tanks];
    for (int i = 0; i < tanks; i++)
      this.tanks[i] = new InternalTank(i, generalCapacity == -1 ? capacities[i] : generalCapacity);
  }

  @Override
  public final int getTanks() {
    return tanks.length;
  }

  /**
   * The fluid at the bottom of the tank in question.
   *
   * @param tank The index of the internal tank
   * @return The fluid from the bottom of the tank
   */
  @Nonnull
  @Override
  public final FluidStack getFluidInTank(int tank) {
    InternalTank internalTank =  tanks[tank];
    return internalTank.getFluid();
  }

  @Override
  public final int getTankCapacity(int tank) {
    return tanks[tank].capacity;
  }

  /**
   * This method defaults to true, meaning it will accept all fluids. Can be overridden to restrict what kinds of
   * fluid will be allowed in the tanks.
   *
   * @param tank Tank ID
   * @param stack Fluid to test for adding to the specified tank
   * @return true only if the tank will ever accept the type of fluid
   */
  @Override
  public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
    return true;
  }

  @Override
  public final int fill(FluidStack resource, FluidAction action) {
    return 0;
  }

  @Nonnull
  @Override
  public final FluidStack drain(FluidStack resource, FluidAction action) {
    return null;
  }

  @Nonnull
  @Override
  public final FluidStack drain(int maxDrain, FluidAction action) {
    return null;
  }

  private class InternalTank extends TreeMap<Fluid, FluidStack> implements IFluidTank {
    private final int capacity;

    private InternalTank(int id, int capacity) {
      if (capacity < 1)
        throw new IllegalArgumentException("Capacity on tank " + id + " is not a positive value");
      this.capacity = capacity;
    }

    @Nonnull
    @Override
    public FluidStack getFluid() {
      return null;
    }

    @Override
    public int getFluidAmount() {
      return 0;
    }

    @Override
    public int getCapacity() {
      return capacity;
    }

    @Override
    public boolean isFluidValid(FluidStack stack) {
      return false;
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
      return 0;
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
      return null;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
      return null;
    }
  }
}
