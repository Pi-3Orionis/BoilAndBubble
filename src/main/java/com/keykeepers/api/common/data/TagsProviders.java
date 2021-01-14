package com.keykeepers.api.common.data;

import net.minecraft.block.Block;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag.INamedTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;

public class TagsProviders {
  protected static ResourceLocation forgeLoc(String name) {
    return new ResourceLocation("forge", name);
  }

  private final String modId;
  private final HashMap<Block, INamedTag<Block>> blockTagsMap = new HashMap<>();
  private final HashMap<Item, INamedTag<Item>> itemTagsMap = new HashMap<>();
  private final HashMap<String, ArrayList<INamedTag<Item>>> subgroupItemsMap = new HashMap<>();
  private final HashMap<String, INamedTag<Item>> subgroupTagsMap = new HashMap<>();
  private final HashMap<String, ArrayList<ResourceLocation>> subgroupOptionalTagsMap = new HashMap<>();
  private ModBlockTagsProvider blockTagsProvider;
  private ModItemTagsProvider itemTagsProvider;

  public TagsProviders(String modId) {
    this.modId = modId;
  }

  public String modId() { return modId; }

  public INamedTag<Block> addBlockTag(Block block, String tagString) {
    INamedTag<Block> tag = BlockTags.makeWrapperTag(forgeLoc(tagString).toString());
    blockTagsMap.put(block, tag);
    return tag;
  }

  public INamedTag<Block> getBlockTag(Block block) { return blockTagsMap.get(block); }

  public INamedTag<Item> getItemTag(Item item) { return itemTagsMap.get(item); }

  public INamedTag<Item> addItemTag(Item item, String tagString) {
    INamedTag<Item> tag = ItemTags.makeWrapperTag(forgeLoc(tagString).toString());
    itemTagsMap.put(item, tag);
    return tag;
  }

  public INamedTag<Item> addItemSubgroupTag(INamedTag<Item> itemTag, String subgroup) {
    ArrayList<INamedTag<Item>> items = subgroupItemsMap.computeIfAbsent(subgroup, k -> new ArrayList<>());
    items.add(itemTag);
    if (!subgroupTagsMap.containsKey(subgroup))
      subgroupTagsMap.put(subgroup, ItemTags.makeWrapperTag(forgeLoc(subgroup).toString()));
    return subgroupTagsMap.get(subgroup);
  }

  public INamedTag<Item> addItemSubgroupOptionalTag(ResourceLocation optionalTag, String subgroup) {
    ArrayList<ResourceLocation> optionalTags = subgroupOptionalTagsMap.computeIfAbsent(subgroup,
        k -> new ArrayList<>());
    optionalTags.add(optionalTag);
    if (!subgroupTagsMap.containsKey(subgroup))
      subgroupTagsMap.put(subgroup, ItemTags.makeWrapperTag(forgeLoc(subgroup).toString()));
    return subgroupTagsMap.get(subgroup);
  }

  public void initializeProviders(DataGenerator generator, @Nullable ExistingFileHelper fileHelper) {
    blockTagsProvider = new ModBlockTagsProvider(generator, modId, fileHelper);
    itemTagsProvider = new ModItemTagsProvider(generator, blockTagsProvider, modId, fileHelper);
  }

  public BlockTagsProvider blockTagsProvider() { return blockTagsProvider; }

  public ItemTagsProvider itemTagsProvider() { return itemTagsProvider; }

  private class ModBlockTagsProvider extends BlockTagsProvider {
    private ModBlockTagsProvider(DataGenerator generatorIn,
                                 String modId,
                                 @Nullable ExistingFileHelper existingFileHelper) {
      super(generatorIn, modId, existingFileHelper);
    }

    @Override
    protected void registerTags() {
      for (Block block : blockTagsMap.keySet()) {
        INamedTag<Block> tag = blockTagsMap.get(block);
        getOrCreateBuilder(tag).add(block);
      }
    }
  }

  private class ModItemTagsProvider extends ItemTagsProvider {

    private ModItemTagsProvider(DataGenerator dataGenerator,
                                BlockTagsProvider blockTagProvider,
                                String modId,
                                @Nullable ExistingFileHelper existingFileHelper) {
      super(dataGenerator, blockTagProvider, modId, existingFileHelper);
    }

    @Override
    protected void registerTags() {
      for (Block block : blockTagsMap.keySet()) {
        INamedTag<Block> blockTag = blockTagsMap.get(block);
        copy(blockTag, ItemTags.makeWrapperTag(forgeLoc(blockTag.getName().getPath()).toString()));
      }

      for (Item item : itemTagsMap.keySet()) {
        INamedTag<Item> tag = itemTagsMap.get(item);
        getOrCreateBuilder(tag).add(item);
      }

      for (String subGroup : subgroupTagsMap.keySet()) {
        INamedTag<Item> tag = subgroupTagsMap.get(subGroup);
        if (subgroupItemsMap.containsKey(subGroup))
          for (INamedTag<Item> itemTag : subgroupItemsMap.get(subGroup))
            getOrCreateBuilder(tag).addTag(itemTag);
        if (subgroupOptionalTagsMap.containsKey(subGroup))
          for (ResourceLocation loc : subgroupOptionalTagsMap.get(subGroup))
            getOrCreateBuilder(tag).addOptionalTag(loc);
      }
    }
  }
}
