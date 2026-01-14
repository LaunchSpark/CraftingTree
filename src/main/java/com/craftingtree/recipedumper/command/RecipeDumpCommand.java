package com.craftingtree.recipedumper.command;

import com.craftingtree.recipedumper.config.RecipeDumpConfig;
import com.craftingtree.recipedumper.dump.RecipeDumpService;
import com.craftingtree.recipedumper.dump.RecipeDumpSummary;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class RecipeDumpCommand {
    private RecipeDumpCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("recipe_dump")
                .requires(source -> source.hasPermission(2))
                .executes(context -> runDump(context, RecipeDumpService.DumpMode.ALL))
                .then(Commands.literal("recipes")
                        .executes(context -> runDump(context, RecipeDumpService.DumpMode.RECIPES)))
                .then(Commands.literal("tags")
                        .executes(context -> runDump(context, RecipeDumpService.DumpMode.TAGS)))
                .then(Commands.literal("help")
                        .executes(RecipeDumpCommand::sendHelp))
        );
    }

    private static int sendHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(Component.literal("/recipe_dump [recipes|tags|help]")
                .withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int runDump(CommandContext<CommandSourceStack> context, RecipeDumpService.DumpMode mode) {
        CommandSourceStack source = context.getSource();
        Path gameDir = source.getServer().getServerDirectory();
        String dumpPath = RecipeDumpConfig.DUMP_PATH.get();
        boolean prettyPrint = RecipeDumpConfig.PRETTY_PRINT.get();
        Path outputRoot = gameDir.resolve(dumpPath);
        source.sendSystemMessage(Component.literal("Starting recipe dump...")
                .withStyle(ChatFormatting.GRAY));

        CompletableFuture.runAsync(() -> {
            RecipeDumpSummary summary = RecipeDumpService.dumpAll(source.getServer(), outputRoot, mode, prettyPrint);
            source.getServer().execute(() -> source.sendSystemMessage(Component.literal(summary.toUserMessage())
                    .withStyle(summary.hasFailures() ? ChatFormatting.RED : ChatFormatting.GREEN)));
        }, RecipeDumpService.EXECUTOR);

        return 1;
    }
}
