package org.pmca.project_myrmidon.client.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BotLauncher {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BOT_MODS_DIR = "bot-mods";
    private static final String BOTS_DIR = "run/bots";
    private static final String BOT_CONFIG_FILE = "bot-config.json";
    private static final String FABRIC_MAIN_CLASS = "net.fabricmc.loader.impl.launch.knot.KnotClient";

    private final Path projectRoot;

    public BotLauncher(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public BotProcess launch(BotConfig config) throws IOException {
        Path botRunDir = projectRoot.resolve(BOTS_DIR).resolve(config.getName());
        Path botModsDir = botRunDir.resolve("mods");

        Files.createDirectories(botModsDir);
        copyMods(config, botModsDir);
        writeBotConfig(config, botRunDir);

        List<String> command = buildCommand(config, botRunDir);
        System.out.println("[BotLauncher] Starting bot '" + config.getName() + "' with command: " + String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(botRunDir.toFile());
        pb.inheritIO();

        Process process = pb.start();
        return new BotProcess(config.getName(), process);
    }

    private void copyMods(BotConfig config, Path botModsDir) throws IOException {
        Path sourceModsDir = projectRoot.resolve(BOT_MODS_DIR);
        if (!Files.isDirectory(sourceModsDir)) {
            System.out.println("[BotLauncher] bot-mods/ directory not found, skipping mod copy");
            return;
        }

        for (Path modPath : config.getModPaths()) {
            Path fileName = modPath.getFileName();
            Path target = botModsDir.resolve(fileName);
            Files.copy(modPath, target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[BotLauncher] Copied mod: " + fileName);
        }
    }

    private void writeBotConfig(BotConfig config, Path botRunDir) throws IOException {
        Map<String, Object> data = Map.of(
                "name", config.getName(),
                "serverAddress", config.getServerAddress(),
                "serverPort", config.getServerPort()
        );
        Path configFile = botRunDir.resolve(BOT_CONFIG_FILE);
        Files.writeString(configFile, GSON.toJson(data));
    }

    private List<String> buildCommand(BotConfig config, Path botRunDir) {
        List<String> cmd = new ArrayList<>();

        String javaHome = System.getProperty("java.home");
        cmd.add(Path.of(javaHome, "bin", "java").toString());

        cmd.add("-Dproject_myrmidon.bot=true");
        cmd.add("-Dproject_myrmidon.bot_name=" + config.getName());

        for (String jvmArg : config.getJvmArgs()) {
            cmd.add(jvmArg);
        }

        String classpath = System.getProperty("java.class.path");
        cmd.add("-cp");
        cmd.add(classpath);

        cmd.add(FABRIC_MAIN_CLASS);

        cmd.add("--username");
        cmd.add(config.getName());

        cmd.add("--server");
        cmd.add(config.getServerAddress());

        cmd.add("--port");
        cmd.add(String.valueOf(config.getServerPort()));

        cmd.add("--nogui");

        cmd.add("--gameDir");
        cmd.add(botRunDir.toAbsolutePath().toString());

        return cmd;
    }
}
