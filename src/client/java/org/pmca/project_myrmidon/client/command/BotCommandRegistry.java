package org.pmca.project_myrmidon.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import org.pmca.project_myrmidon.client.bot.BotConfig;
import org.pmca.project_myrmidon.client.bot.BotManager;
import org.pmca.project_myrmidon.client.bot.BotProcess;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

import java.io.IOException;
import java.util.Collection;

public class BotCommandRegistry {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("bot")
                    .then(ClientCommandManager.literal("create")
                            .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                    .executes(BotCommandRegistry::executeCreate)))
                    .then(ClientCommandManager.literal("remove")
                            .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                    .executes(BotCommandRegistry::executeRemove)))
                    .then(ClientCommandManager.literal("list")
                            .executes(BotCommandRegistry::executeList)));
        });
    }

    private static int executeCreate(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        BotManager manager = BotManager.getInstance();

        if (manager.getBot(name) != null) {
            source.sendError(Text.literal("Bot '" + name + "' already exists"));
            return 0;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        ServerInfo serverInfo = mc.getCurrentServerEntry();
        if (serverInfo == null) {
            source.sendError(Text.literal("You must be connected to a server to create a bot"));
            return 0;
        }

        ServerAddress serverAddress = ServerAddress.parse(serverInfo.address);

        BotConfig config = BotConfig.builder()
                .name(name)
                .serverAddress(serverAddress.getAddress())
                .serverPort(serverAddress.getPort())
                .build();

        try {
            manager.createBot(config);
            source.sendFeedback(Text.literal("Bot '" + name + "' created"));
            return Command.SINGLE_SUCCESS;
        } catch (IOException e) {
            source.sendError(Text.literal("Failed to create bot: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeRemove(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        BotManager manager = BotManager.getInstance();

        if (manager.removeBot(name)) {
            source.sendFeedback(Text.literal("Bot '" + name + "' removed"));
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendError(Text.literal("Bot '" + name + "' not found"));
            return 0;
        }
    }

    private static int executeList(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        Collection<BotProcess> bots = BotManager.getInstance().listBots();

        if (bots.isEmpty()) {
            source.sendFeedback(Text.literal("No active bots"));
            return Command.SINGLE_SUCCESS;
        }

        StringBuilder sb = new StringBuilder("Active bots:");
        for (BotProcess bot : bots) {
            sb.append("\n  - ").append(bot.getName());
            sb.append(bot.isAlive() ? " (running)" : " (stopped)");
        }
        source.sendFeedback(Text.literal(sb.toString()));
        return Command.SINGLE_SUCCESS;
    }
}
