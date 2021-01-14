package com.keykeepers.boilbubble.client.data;

import com.keykeepers.boilbubble.BoilBubble;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.Objects;

public class BlockStates extends BlockStateProvider {
  public BlockStates(DataGenerator gen, ExistingFileHelper exFileHelper) {
    super(gen, BoilBubble.MODID, exFileHelper);
  }

  @Override
  protected void registerStatesAndModels() {
    // TODO
  }

  private ResourceLocation slabTexture(Block block) {
    String name = Objects.requireNonNull(block.getRegistryName()).getPath();
    return modLoc("block/" + name);
  }
}
