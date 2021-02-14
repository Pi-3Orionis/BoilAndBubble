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
    int generalCapacity = -1; // Sentinel value for using per-tank capacity values
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

  public final FluidStack[] contents(int tank) { return tanks[tank].contents(); }

  @Override
  public final int getTanks() {
    return tanks.length;
  }

  @Nonnull
  public final FluidStack getFluidStack(int tank, Fluid fluid) {
    FluidStack stack = tanks[tank].get(fluid);
    if (stack == null)
      return FluidStack.EMPTY;
    return stack;
  }

  public final FluidStack getFluidInTank(int tank, TankAccessType type) {
    InternalTank internalTank = tanks[tank];
    return internalTank.getAccess(type).getFluid();
  }

  public final int getFluidAmount(int tank) {
    int count = 0;
    for (FluidStack stack : tanks[tank].contents())
      count += stack.getAmount();
    return count;
  }

  @Nonnull
  @Override
  public final FluidStack getFluidInTank(int tank) { return getFluidInTank(tank, TankAccessType.BOTTOM); }

  @Override
  public final int getTankCapacity(int tank) {
    return tanks[tank].capacity;
  }

  public abstract boolean isFluidValid(int tank, TankAccessType type, @Nonnull FluidStack stack);

  @Override
  public final boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
    return isFluidValid(tank, TankAccessType.BOTTOM, stack);
  }

  public final int fill(FluidStack resource, TankAccessType type, FluidAction action) {
    FluidStack excessStack = resource.copy();
    for (int i = 0; i < tanks.length; i++) {
      if (!isFluidValid(i, resource))
        continue;
      InternalTank tank = tanks[i];
      excessStack.shrink(tank.getAccess(type).fill(excessStack, action));
      if (excessStack.getAmount() < 1)
        break;
    }
    return resource.getAmount() - excessStack.getAmount();
  }

  @Override
  public final int fill(FluidStack resource, FluidAction action) {
    return fill(resource, TankAccessType.BOTTOM, action);
  }

  public final FluidStack drain(TankAccessType type, FluidStack resource, FluidAction action) {
    FluidStack remainingStack = resource.copy();
    for (InternalTank tank : tanks) {
      FluidStack tankDrainStack = tank.getAccess(type).drain(remainingStack, action);
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
  public final FluidStack drain(FluidStack resource, FluidAction action) {
    return drain(TankAccessType.BOTTOM, resource, action);
  }

  @Nonnull
  public final FluidStack drain(TankAccessType type, int maxDrain, FluidAction action) {
    FluidStack drainStack = null;
    for (InternalTank tank : tanks) {
      if (drainStack == null) {
        drainStack = tank.getAccess(type).drain(maxDrain, action);
        if (drainStack.equals(FluidStack.EMPTY))
          drainStack = null;
      } else if (maxDrain > drainStack.getAmount()) {
        FluidStack tankDrainStack = drainStack.copy();
        tankDrainStack.setAmount(maxDrain - drainStack.getAmount());
        tankDrainStack = tank.getAccess(type).drain(tankDrainStack, action);
        drainStack.grow(tankDrainStack.getAmount());
      } else
        break;
    }
    if (drainStack == null)
      return FluidStack.EMPTY;
    return drainStack;
  }

  @Nonnull
  @Override
  public final FluidStack drain(int maxDrain, FluidAction action) {
    return drain(TankAccessType.BOTTOM, maxDrain, action);
  }

  public enum TankAccessType {
    BOTTOM,
    TOP
  }

  public class TankAccess implements IFluidTank {
    private final int id;
    private final TankAccessType type;

    protected TankAccess(int id, TankAccessType type) {
      this.id = id;
      this.type = type;
    }

    public final int id() { return id; }

    public final TankAccessType type() { return type; }

    public final FluidStack[] contents() { return tanks[id].contents(); }

    protected final InternalTank tank() { return tanks[id]; }

    @Nonnull
    @Override
    public final FluidStack getFluid() {
      if (tank().isEmpty())
        return FluidStack.EMPTY;

      FluidStack[] contents = tank().contents();
      if (type == TankAccessType.TOP)
        return contents[contents.length - 1];
      else
        return contents[0];
    }

    @Override
    public final int getFluidAmount() {
      int amount = 0;
      for (FluidStack stack : tank().values())
        amount += stack.getAmount();
      return amount;
    }

    @Override
    public final int getCapacity() {
      return tank().capacity;
    }

    /**
     * Handled via the enclosing class.
     *
     * @param stack The potential input to test
     * @return true if the tank can hold the fluid
     */
    @Override
    public final boolean isFluidValid(FluidStack stack) {
      return FractionalTank.this.isFluidValid(id, type, stack);
    }

    @Override
    public final int fill(FluidStack resource, IFluidHandler.FluidAction action) {
      int availVolume = getCapacity() - getFluidAmount();
      if (availVolume < 1)
        return 0;
      int fillAmount = Math.min(resource.getAmount(), availVolume);
      if (action.execute())
        if (tank().containsKey(resource.getFluid())) {
          FluidStack tankStack = tank().get(resource.getFluid());
          tankStack.grow(fillAmount);
        }
        else {
          FluidStack newStack = resource.copy();
          newStack.setAmount(fillAmount);
          tank().put(newStack.getFluid(), newStack);
        }
      return fillAmount;
    }

    @Nonnull
    @Override
    public final FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
      if (tank().isEmpty())
        return FluidStack.EMPTY;
      FluidStack tankStack = tank().values().iterator().next();
      FluidStack drainStack = tankStack.copy();
      if (tankStack.getAmount() > maxDrain)
        drainStack.setAmount(maxDrain);
      if (action.execute()) {
        if (tankStack.getAmount() > maxDrain)
          tankStack.shrink(maxDrain);
        else
          tank().remove(tankStack.getFluid());
      }
      return drainStack;
    }

    @Nonnull
    @Override
    public final FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
      if (!tank().containsKey(resource.getFluid()))
        return FluidStack.EMPTY;
      FluidStack tankStack = tank().get(resource.getFluid());
      FluidStack drainStack = resource.copy();
      if (drainStack.getAmount() > tankStack.getAmount())
        drainStack.setAmount(tankStack.getAmount());
      if (action.execute()) {
        if (tankStack.getAmount() > drainStack.getAmount())
          tankStack.shrink(drainStack.getAmount());
        else
          tank().remove(tankStack.getFluid());
      }
      return drainStack;
    }
  }

  private class InternalTank extends TreeMap<Fluid, FluidStack> {
    private final int id;
    private final int capacity;

    private InternalTank(int id, int capacity) {
      super((o1, o2) -> Integer.compare(o2.getAttributes().getDensity(), o1.getAttributes().getDensity()));
      this.id = id;
      if (capacity < 1)
        throw new IllegalArgumentException("Capacity on tank " + id + " is not a positive value");
      this.capacity = capacity;
    }

    private final FluidStack[] contents() {
      return values().toArray(new FluidStack[0]);
    }

    private TankAccess getAccess(TankAccessType type) {
      return new TankAccess(id, type);
    }
  }
}
