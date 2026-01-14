package com.craftingtree.recipedumper;

import com.craftingtree.recipedumper.command.RecipeDumpCommand;
import com.craftingtree.recipedumper.config.RecipeDumpConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(RecipeDumpMod.MOD_ID)
public class RecipeDumpMod {
    public static final String MOD_ID = "recipedumper";

    public RecipeDumpMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RecipeDumpConfig.SPEC);
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        RecipeDumpCommand.register(event.getDispatcher());
    }
}
