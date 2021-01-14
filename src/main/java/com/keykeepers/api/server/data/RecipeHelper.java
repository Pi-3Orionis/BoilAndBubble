package com.keykeepers.api.server.data;

import com.keykeepers.api.common.data.TagsProviders;
import net.minecraft.data.*;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITag;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class RecipeHelper extends RecipeProvider {

  protected static ResourceLocation forgeLoc(String path) {
    return new ResourceLocation("forge", path);
  }

  protected final TagsProviders tagsProviders;

  public RecipeHelper(DataGenerator generatorIn, TagsProviders tagsProviders) {
    super(generatorIn);
    this.tagsProviders = tagsProviders;
  }

  protected abstract void recipes(Consumer<IFinishedRecipe> consumer);

  protected ResourceLocation modLoc(String path) {
    return new ResourceLocation(tagsProviders.modId(), path);
  }

  protected String pathName(Item item) {
    return Objects.requireNonNull(item.getRegistryName()).getPath();
  }

  protected void packingRecipes(Consumer<IFinishedRecipe> consumer,
                                Item storageItem,
                                Item componentItem,
                                ITag.INamedTag<Item> componentTag) {
    ShapedRecipeBuilder.shapedRecipe(storageItem)
        .key('i', componentItem)
        .key('t', componentTag)
        .patternLine("ttt")
        .patternLine("tit") // lulz ^_^
        .patternLine("ttt")
        .addCriterion("has_" + itemPath(componentItem), hasItem(componentItem))
        .build(consumer, conversionRecipeName(storageItem, componentItem));

    ShapelessRecipeBuilder.shapelessRecipe(componentItem, 9)
        .addIngredient(storageItem)
        .addCriterion("has_" + itemPath(storageItem), hasItem(storageItem))
        .build(consumer, conversionRecipeName(componentItem, storageItem));
  }

  protected void slabRecipes(Consumer<IFinishedRecipe> consumer, Item blockItem, Item slabItem) {
    ShapedRecipeBuilder.shapedRecipe(slabItem, 6)
        .key('b', blockItem)
        .patternLine("bbb")
        .addCriterion("has_" + itemPath(blockItem), hasItem(blockItem))
        .build(consumer, conversionRecipeName(slabItem, blockItem));
    ShapedRecipeBuilder.shapedRecipe(blockItem)
        .key('s', slabItem)
        .patternLine("s")
        .patternLine("s")
        .addCriterion("has_" + itemPath(blockItem), hasItem(blockItem))
        .build(consumer, conversionRecipeName(blockItem, slabItem));
  }

  protected void smeltingRecipes(Consumer<IFinishedRecipe> consumer,
                                 IItemProvider input,
                                 IItemProvider output,
                                 float xp,
                                 int smeltingTime) {
    CookingRecipeBuilder.smeltingRecipe(Ingredient.fromItems(input), output, xp, smeltingTime)
        .addCriterion("has_" + itemPath(input), hasItem(input))
        .build(consumer, smeltingRecipeName(output));
    CookingRecipeBuilder.blastingRecipe(Ingredient.fromItems(input), output, xp, smeltingTime / 2)
        .addCriterion("has_" + itemPath(input), hasItem(input))
        .build(consumer, blastingRecipeName(output));
  }

  protected void smeltingRecipes(Consumer<IFinishedRecipe> consumer,
                                 IItemProvider input,
                                 IItemProvider output,
                                 float xp) {
    smeltingRecipes(consumer, input, output, xp, 200);
  }

  protected String itemPath(IItemProvider item) {
    return Objects.requireNonNull(item.asItem().getRegistryName()).getPath();
  }

  protected ITag.INamedTag<Item> itemTag(IItemProvider item) {
    return tagsProviders.getItemTag(item.asItem());
  }

  protected ResourceLocation conversionRecipeName(IItemProvider output, IItemProvider input) {
    return new ResourceLocation(tagsProviders.modId(), itemPath(output) + "_from_" + itemPath(input));
  }

  protected ResourceLocation smeltingRecipeName(IItemProvider output) {
    return new ResourceLocation(tagsProviders.modId(), "smelting/" + itemPath(output) + "_smelting");
  }

  protected ResourceLocation blastingRecipeName(IItemProvider output) {
    return new ResourceLocation(tagsProviders.modId(), "smelting/" + itemPath(output) + "_blasting");
  }

  @Override
  @ParametersAreNonnullByDefault
  protected final void registerRecipes(Consumer<IFinishedRecipe> consumer) {
    recipes(consumer);
  }
}
