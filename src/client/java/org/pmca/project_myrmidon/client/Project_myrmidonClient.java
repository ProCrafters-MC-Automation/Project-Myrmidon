package org.pmca.project_myrmidon.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.pmca.project_myrmidon.client.bot.BotManager;
import org.pmca.project_myrmidon.client.command.BotCommandRegistry;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public class Project_myrmidonClient implements ClientModInitializer {

    private static final Gson GSON = new Gson();
    private static final String BOT_CONFIG_FILE = "bot-config.json";
    private static boolean connectAttempted = false;

    @Override
    public void onInitializeClient() {
        if (isBotMode()) {
            String botName = System.getProperty("project_myrmidon.bot_name", "unknown");
            System.out.println("[Project Myrmidon] Bot mode detected: " + botName);
            registerBotAutoConnect();
            return;
        }

        Path projectRoot = findProjectRoot();
        BotManager.init(projectRoot);
        BotCommandRegistry.register();
        System.out.println("[Project Myrmidon] Bot system initialized");
    }

    private void registerBotAutoConnect() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!connectAttempted) {
                if (client.currentScreen instanceof TitleScreen) {
                    connectAttempted = true;
                    connectToServer(client);
                } else if (client.currentScreen != null) {
                    client.setScreen(null);
                }
            }

            if (client.player != null) {
                executePendingCommand(client);
            }
        });
    }

    private void executePendingCommand(MinecraftClient client) {
        Path commandFile = client.runDirectory.toPath().resolve("pending_command.json");
        if (!Files.exists(commandFile)) {
            return;
        }

        try {
            String json = Files.readString(commandFile);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            String type = obj.get("type").getAsString();
            String content = obj.get("content").getAsString();

            if ("chat".equals(type)) {
                client.getNetworkHandler().sendChatMessage(content);
                System.out.println("[Project Myrmidon] Sent chat: " + content);
            } else if ("command".equals(type)) {
                String cmd = content.startsWith("/") ? content.substring(1) : content;
                client.getNetworkHandler().sendChatCommand(cmd);
                System.out.println("[Project Myrmidon] Ran command: " + content);
            }

            Files.delete(commandFile);
        } catch (Exception e) {
            System.err.println("[Project Myrmidon] Failed to execute command: " + e.getMessage());
            try { Files.deleteIfExists(commandFile); } catch (Exception ignored) {}
        }
    }

    private void connectToServer(MinecraftClient client) {
        Path gameDir = client.runDirectory.toPath();
        Path configPath = gameDir.resolve(BOT_CONFIG_FILE);

        if (!Files.exists(configPath)) {
            System.out.println("[Project Myrmidon] bot-config.json not found at " + configPath);
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            String serverAddress = json.get("serverAddress").getAsString();
            int serverPort = json.get("serverPort").getAsInt();
            String botName = json.get("name").getAsString();

            String address = serverPort == 25565 ? serverAddress : serverAddress + ":" + serverPort;

            ServerInfo serverInfo = new ServerInfo(botName, address, ServerInfo.ServerType.OTHER);
            ServerAddress parsed = ServerAddress.parse(address);

            System.out.println("[Project Myrmidon] Bot '" + botName + "' connecting to " + address);
            ConnectScreen.connect(null, client, parsed, serverInfo, false, null);
        } catch (IOException e) {
            System.out.println("[Project Myrmidon] Failed to read bot-config.json: " + e.getMessage());
        }
    }

    private boolean isBotMode() {
        return "true".equals(System.getProperty("project_myrmidon.bot"));
    }

    private Path findProjectRoot() {
        String classpath = System.getProperty("java.class.path");
        if (classpath != null && !classpath.isEmpty()) {
            String firstEntry = classpath.split(java.io.File.pathSeparator)[0];
            Path path = Path.of(firstEntry);
            Path current = path;
            while (current != null) {
                if (current.resolve("build.gradle").toFile().exists()
                        || current.resolve("settings.gradle").toFile().exists()) {
                    return current;
                }
                current = current.getParent();
            }
        }
        return Path.of("").toAbsolutePath();
    }
}
