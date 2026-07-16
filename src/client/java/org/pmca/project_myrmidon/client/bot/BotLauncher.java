package org.pmca.project_myrmidon.client.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

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
        copyClasspathMods(botModsDir);
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

        try (var stream = Files.list(sourceModsDir)) {
            for (Path modFile : stream.toList()) {
                if (Files.isRegularFile(modFile) && modFile.toString().endsWith(".jar")) {
                    Path fileName = modFile.getFileName();
                    Path target = botModsDir.resolve(fileName);
                    Files.copy(modFile, target, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[BotLauncher] Copied bot-mod: " + fileName);
                }
            }
        }
    }

    private void copyClasspathMods(Path botModsDir) throws IOException {
        String classpath = System.getProperty("java.class.path");
        if (classpath == null || classpath.isEmpty()) {
            return;
        }

        Set<String> copied = new HashSet<>();
        String separator = System.getProperty("path.separator");

        for (String entry : classpath.split(separator)) {
            Path path = Path.of(entry);
            if (!Files.isRegularFile(path) || !path.toString().endsWith(".jar")) {
                continue;
            }
            if (!isFabricMod(path)) {
                continue;
            }
            String fileName = path.getFileName().toString();
            if (copied.contains(fileName)) {
                continue;
            }
            Path target = botModsDir.resolve(fileName);
            Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
            copied.add(fileName);
            System.out.println("[BotLauncher] Copied classpath mod: " + fileName);
        }

        List<Path> directories = new ArrayList<>();
        for (String entry : classpath.split(separator)) {
            Path path = Path.of(entry);
            if (Files.isDirectory(path)) {
                directories.add(path);
            }
        }

        for (Path dir : directories) {
            if (!Files.isRegularFile(dir.resolve("fabric.mod.json"))) {
                continue;
            }
            String modId = readModId(dir);
            if (modId == null || copied.contains(modId + ".jar")) {
                continue;
            }
            List<Path> members = findProjectClasspathDirs(dir, directories);
            String jarName = modId + "-dev-mod.jar";
            Path target = botModsDir.resolve(jarName);
            packageDirectoriesIntoJar(members, target);
            copied.add(jarName);
            System.out.println("[BotLauncher] Packaged directory mod '" + modId + "' into: " + jarName);
        }
    }

    private List<Path> findProjectClasspathDirs(Path modRoot, List<Path> allDirs) {
        Path projectRoot = findProjectRoot(modRoot);
        if (projectRoot == null) {
            return List.of(modRoot);
        }
        Path buildDir = projectRoot.resolve("build");
        List<Path> result = new ArrayList<>();
        for (Path dir : allDirs) {
            if (dir.startsWith(buildDir)) {
                result.add(dir);
            }
        }
        return result.isEmpty() ? List.of(modRoot) : result;
    }

    private static Path findProjectRoot(Path start) {
        Path current = start;
        while (current != null) {
            if (Files.isDirectory(current.resolve("build")) && Files.isDirectory(current.resolve("src"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static String readModId(Path dir) {
        try {
            Path fabricModJson = dir.resolve("fabric.mod.json");
            if (!Files.isRegularFile(fabricModJson)) {
                return null;
            }
            String content = Files.readString(fabricModJson);
            Matcher match = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
            return match.find() ? match.group(1) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static void packageDirectoriesIntoJar(List<Path> directories, Path jarPath) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            Set<String> added = new HashSet<>();
            for (Path dir : directories) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                packageDirectory(dir, dir, jos, added);
            }
        }
    }

    private static void packageDirectory(Path root, Path current, JarOutputStream jos, Set<String> added) throws IOException {
        if (Files.isDirectory(current)) {
            try (var stream = Files.list(current)) {
                for (Path child : stream.toList()) {
                    packageDirectory(root, child, jos, added);
                }
            }
        } else {
            String entryName = root.relativize(current).toString().replace('\\', '/');
            if (added.add(entryName)) {
                jos.putNextEntry(new ZipEntry(entryName));
                Files.copy(current, jos);
                jos.closeEntry();
            }
        }
    }

    private static boolean isFabricMod(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            ZipEntry entry = jar.getEntry("fabric.mod.json");
            return entry != null;
        } catch (IOException e) {
            return false;
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

        String namespace = detectNamespace();
        if (namespace != null) {
            cmd.add("-Dfabric.runtimeMappingNamespace=" + namespace);
        }

        for (String jvmArg : config.getJvmArgs()) {
            cmd.add(jvmArg);
        }

        String classpath = System.getProperty("java.class.path");
        cmd.add("-cp");
        cmd.add(classpath);

        cmd.add(FABRIC_MAIN_CLASS);

        cmd.add("--username");
        cmd.add(config.getName());

        cmd.add("--gameDir");
        cmd.add(botRunDir.toAbsolutePath().toString());

        return cmd;
    }

    private static String detectNamespace() {
        try {
            return net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getMappingResolver().getCurrentRuntimeNamespace();
        } catch (Exception e) {
            return null;
        }
    }
}
