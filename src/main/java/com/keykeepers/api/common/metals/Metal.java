package com.keykeepers.api.common.metals;

import com.keykeepers.api.common.Registry;
import com.keykeepers.api.common.data.TagsProviders;
import com.keykeepers.api.server.data.RecipeHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.data.ShapelessRecipeBuilder;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag.INamedTag;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.ToolType;

import java.util.function.Consumer;

public abstract class Metal {
  private static final INamedTag<Item> hammer = ItemTags.makeWrapperTag("immersiveengineering:hammer");

  private final TagsProviders tagsProviders;
  private final String name;
  private final Registry.BlockAndItem<Block> storageBlock;
  private final Registry.BlockAndItem<SlabBlock> storageSlab;
  private final Registry.BlockAndItem<Block> sheetmetalBlock;
  private final Registry.BlockAndItem<SlabBlock> sheetmetalSlab;
  private final Item ingot;
  private final Item nugget;
  private final Item dust;
  private final Item plate;
  private final Item rod;

  public Metal(TagsProviders tagsProviders, Registry registry, String name) {
    this.tagsProviders = tagsProviders;
    this.name = name;
    AbstractBlock.Properties storageProperties = AbstractBlock.Properties.create(Material.IRON)
        .sound(SoundType.METAL)
        .hardnessAndResistance(5, 10)
        .setRequiresTool()
        .harvestTool(ToolType.PICKAXE)
        .harvestLevel(2);
    storageBlock = registry.registerBlock("storage_" + name, storageProperties);
    tagsProviders.addBlockTag(storageBlock.block, "storage_blocks/" + name);
    storageSlab = registry.registerSlab("slab_storage_" + name, storageProperties);
    AbstractBlock.Properties sheetmetalProperties = AbstractBlock.Properties.create(Material.IRON)
        .sound(SoundType.METAL)
        .hardnessAndResistance(3, 10);
    sheetmetalBlock = registry.registerBlock("sheetmetal_" + name, sheetmetalProperties);
    tagsProviders.addBlockTag(sheetmetalBlock.block, "sheetmetals/" + name);
    sheetmetalSlab = registry.registerSlab("slab_sheetmetal_" + name, sheetmetalProperties);
    ingot = registry.registerItem("ingot_" + name);
    tagsProviders.addItemTag(ingot, "ingots/" + name);
    nugget = registry.registerItem("nugget_" + name);
    tagsProviders.addItemTag(nugget, "nuggets/" + name);
    dust = registry.registerItem("dust_" + name);
    tagsProviders.addItemTag(dust, "dusts/" + name);
    plate = registry.registerItem("plate_" + name);
    tagsProviders.addItemTag(plate, "plates/" + name);
    rod = registry.registerItem("stick_" + name);
    INamedTag<Item> rodTag = tagsProviders.addItemTag(rod, "rods/" + name);
    tagsProviders.addItemSubgroupTag(rodTag, "rods/all_metal");
  }

  public final String name() { return name; }
  public final Block storageBlock() { return storageBlock.block; }
  public final Item storageBlockItem() { return storageBlock.item; }
  public final SlabBlock storageSlab() { return storageSlab.block; }
  public final Item storageSlabItem() { return storageSlab.item; }
  public final Block sheetmetalBlock() { return sheetmetalBlock.block; }
  public final Item sheetmetalBlockItem() { return sheetmetalBlock.item; }
  public final SlabBlock sheetmetalSlab() { return sheetmetalSlab.block; }
  public final Item sheetmetalSlabItem() { return sheetmetalSlab.item; }
  public final Item ingot() { return ingot; }
  public final Item nugget() { return nugget; }
  public final Item dust() { return dust; }
  public final Item plate() { return plate; }
  public final Item rod() { return rod; }

  public final MetalRecipes metalRecipeProvider(DataGenerator generatorIn) {
    return new MetalRecipes(generatorIn);
  }

  public class MetalRecipes extends RecipeHelper {

    public MetalRecipes(DataGenerator generatorIn) {
      super(generatorIn, Metal.this.tagsProviders);
    }

    @Override
    protected final void recipes(Consumer<IFinishedRecipe> consumer) {
      slabRecipes(consumer, storageBlock.item, storageSlab.item);
      slabRecipes(consumer, sheetmetalBlock.item, sheetmetalSlab.item);

      packingRecipes(consumer, ingot, nugget,
          itemTag(nugget));
      packingRecipes(consumer, storageBlock.item, ingot,
          itemTag(ingot));

      ShapedRecipeBuilder.shapedRecipe(rod, 4)
          .key('i', itemTag(ingot))
          .patternLine("i")
          .patternLine("i")
          .addCriterion("has_" + pathName(ingot), hasItem(ingot))
          .build(consumer, modLoc(pathName(rod)));

      ShapelessRecipeBuilder.shapelessRecipe(plate)
          .addIngredient(itemTag(ingot))
          .addIngredient(hammer)
          .addCriterion("has_" + pathName(ingot), hasItem(itemTag(ingot)))
          .build(consumer, modLoc(pathName(plate) + "_hammering"));

      ShapedRecipeBuilder.shapedRecipe(sheetmetalBlock.item, 4)
          .key('p', itemTag(plate))
          .patternLine(" p ")
          .patternLine("p p")
          .patternLine(" p ")
          .addCriterion("has_" + pathName(plate), hasItem(plate))
          .build(consumer, modLoc("sheetmetal_" + name));

      smeltingRecipes(consumer, dust, ingot, 0.0f);
    }
  }
}
