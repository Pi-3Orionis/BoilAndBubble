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
public abstract class FractionalTank implements IFluidHandler {
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
    InternalTank internalTank = tanks[tank];
    return internalTank.getFluid();
  }

  @Override
  public final int getTankCapacity(int tank) {
    return tanks[tank].capacity;
  }

  @Override
  public abstract boolean isFluidValid(int tank, @Nonnull FluidStack stack);

  @Override
  public final int fill(FluidStack resource, FluidAction action) {
    FluidStack excessStack = resource.copy();
    for (int i = 0; i < tanks.length; i++) {
      if (!isFluidValid(i, resource))
        continue;
      InternalTank tank = tanks[i];
      excessStack.shrink(tank.fill(excessStack, action));
      if (excessStack.getAmount() < 1)
        break;
    }
    return resource.getAmount() - excessStack.getAmount();
  }

  @Nonnull
  @Override
  public final FluidStack drain(FluidStack resource, FluidAction action) {
    FluidStack remainingStack = resource.copy();
    for (InternalTank tank : tanks) {
      FluidStack tankDrainStack = tank.drain(remainingStack, action);
      remainingStack.shrink(tankDrainStack.getAmount());
      if (remainingStack.getAmount() < 1)
        break;
    }
    if (remainingStack.getAmount() < 1)
      return resource.copy();
    FluidStack drainStack = resource.copy();
    drainStack.shrink(remainingStack.getAmount());
    return drainStack;
  }

  @Nonnull
  @Override
  public final FluidStack drain(int maxDrain, FluidAction action) {
    FluidStack drainStack = null;
    for (InternalTank tank : tanks) {
      if (drainStack == null) {
        drainStack = tank.drain(maxDrain, action);
        if (drainStack.equals(FluidStack.EMPTY))
          drainStack = null;
      } else if (maxDrain > drainStack.getAmount()) {
        FluidStack tankDrainStack = drainStack.copy();
        tankDrainStack.setAmount(maxDrain - drainStack.getAmount());
        tankDrainStack = tank.drain(tankDrainStack, action);
        drainStack.grow(tankDrainStack.getAmount());
      } else
        break;
    }
    if (drainStack == null)
      return FluidStack.EMPTY;
    return drainStack;
  }

  private class InternalTank extends TreeMap<Fluid, FluidStack> implements IFluidTank {
    private final int id;
    private final int capacity;

    private InternalTank(int id, int capacity) {
      super((o1, o2) -> Integer.compare(o2.getAttributes().getDensity(), o1.getAttributes().getDensity()));
      this.id = id;
      if (capacity < 1)
        throw new IllegalArgumentException("Capacity on tank " + id + " is not a positive value");
      this.capacity = capacity;

    }

    @Nonnull
    @Override
    public FluidStack getFluid() {
      return isEmpty() ? FluidStack.EMPTY : values().iterator().next();
    }

    @Override
    public int getFluidAmount() {
      int amount = 0;
      for (FluidStack stack : values())
        amount += stack.getAmount();
      return amount;
    }

    @Override
    public int getCapacity() {
      return capacity;
    }

    /**
     * Handled via the enclosing class.
     *
     * @param stack The potential input to test
     * @return true if the tank can hold the fluid
     */
    @Override
    public boolean isFluidValid(FluidStack stack) {
      return FractionalTank.this.isFluidValid(id, stack);
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
      int availVolume = capacity - getFluidAmount();
      if (availVolume < 1)
        return 0;
      int fillAmount = Math.min(resource.getAmount(), availVolume);
      if (action.execute())
        if (containsKey(resource.getFluid())) {
          FluidStack tankStack = get(resource.getFluid());
          tankStack.grow(fillAmount);
        }
        else {
          FluidStack newStack = resource.copy();
          newStack.setAmount(fillAmount);
          put(newStack.getFluid(), newStack);
        }
      return fillAmount;
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
      if (isEmpty())
        return FluidStack.EMPTY;
      FluidStack tankStack = values().iterator().next();
      FluidStack drainStack = tankStack.copy();
      if (tankStack.getAmount() > maxDrain)
        drainStack.setAmount(maxDrain);
      if (action.execute()) {
        if (tankStack.getAmount() > maxDrain)
          tankStack.shrink(maxDrain);
        else
          remove(tankStack.getFluid());
      }
      return drainStack;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
      if (!containsKey(resource.getFluid()))
        return FluidStack.EMPTY;
      FluidStack tankStack = get(resource.getFluid());
      FluidStack drainStack = resource.copy();
      if (drainStack.getAmount() > tankStack.getAmount())
        drainStack.setAmount(tankStack.getAmount());
      if (action.execute()) {
        if (tankStack.getAmount() > drainStack.getAmount())
          tankStack.shrink(drainStack.getAmount());
        else
          remove(tankStack.getFluid());
      }
      return drainStack;
    }
  }
}
