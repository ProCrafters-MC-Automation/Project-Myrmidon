package org.pmca.project_myrmidon.client;

import net.fabricmc.api.ClientModInitializer;
import org.pmca.project_myrmidon.client.bot.BotManager;
import org.pmca.project_myrmidon.client.command.BotCommandRegistry;

import java.nio.file.Path;

public class Project_myrmidonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (isBotMode()) {
            System.out.println("[Project Myrmidon] Bot mode detected, skipping client command registration");
            return;
        }

        Path projectRoot = findProjectRoot();
        BotManager.init(projectRoot);
        BotCommandRegistry.register();
        System.out.println("[Project Myrmidon] Bot system initialized");
    }

    private boolean isBotMode() {
        return "true".equals(System.getProperty("project_myrmidon.bot"));
    }

    private Path findProjectRoot() {
        String classpath = System.getProperty("java.class.path");
        if (classpath != null && !classpath.isEmpty()) {
            String firstEntry = classpath.split(java.io.File.pathSeparator)[0];
            Path path = Path.of(firstEntry);
            // Walk up from the classpath entry to find the project root
            // In dev, classpath entries are under build/classes/java/client or similar
            Path current = path;
            while (current != null) {
                if (current.resolve("build.gradle").toFile().exists()
                        || current.resolve("settings.gradle").toFile().exists()) {
                    return current;
                }
                current = current.getParent();
            }
        }
        // Fallback: assume we're running from the project root
        return Path.of("").toAbsolutePath();
    }
}
