package com.keykeepers.boilbubble.common.data;

import com.keykeepers.api.common.data.TagsProviders;
import com.keykeepers.boilbubble.BoilBubble;
import com.keykeepers.boilbubble.client.data.BlockStates;
import com.keykeepers.boilbubble.client.data.ItemModels;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber(modid = BoilBubble.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGatherer {

  @SubscribeEvent
  public static void gatherData(GatherDataEvent event) {
    DataGenerator generator = event.getGenerator();
    ExistingFileHelper fileHelper = event.getExistingFileHelper();

    TagsProviders tagsProviders = BoilBubble.tagProviders;
    tagsProviders.initializeProviders(generator, fileHelper);
    generator.addProvider(tagsProviders.blockTagsProvider());
    generator.addProvider(tagsProviders.itemTagsProvider());

    if (event.includeServer()) {
      // TODO generator.addProvider();
    }

    if (event.includeClient()) {
      generator.addProvider(blockStateProvider(generator, fileHelper));
      generator.addProvider(itemModelProvider(generator, fileHelper));
    }
  }

  private static BlockStateProvider blockStateProvider(DataGenerator generator, ExistingFileHelper fileHelper) {
    return new BlockStates(generator, fileHelper);
  }

  private static ItemModelProvider itemModelProvider(DataGenerator generator, ExistingFileHelper fileHelper) {
    return new ItemModels(generator, fileHelper);
  }
}
