package org.pmca.project_myrmidon.client.bot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BotManager {

    private static BotManager instance;

    private final Map<String, BotProcess> bots = new LinkedHashMap<>();
    private final BotLauncher launcher;
    private final List<BotConfig> registeredConfigs = new ArrayList<>();

    private BotManager(Path projectRoot) {
        this.launcher = new BotLauncher(projectRoot);
    }

    public static void init(Path projectRoot) {
        if (instance == null) {
            instance = new BotManager(projectRoot);
        }
    }

    public static BotManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BotManager has not been initialized");
        }
        return instance;
    }

    public void registerConfig(BotConfig config) {
        registeredConfigs.add(config);
    }

    public List<BotConfig> getRegisteredConfigs() {
        return Collections.unmodifiableList(registeredConfigs);
    }

    public BotProcess createBot(BotConfig config) throws IOException {
        if (bots.containsKey(config.getName())) {
            throw new IllegalStateException("Bot '" + config.getName() + "' already exists");
        }

        BotProcess process = launcher.launch(config);
        bots.put(config.getName(), process);
        System.out.println("[BotManager] Created bot: " + config.getName());
        return process;
    }

    public boolean removeBot(String name) {
        BotProcess process = bots.remove(name);
        if (process == null) {
            return false;
        }
        process.kill();
        System.out.println("[BotManager] Removed bot: " + name);
        return true;
    }

    public BotProcess getBot(String name) {
        return bots.get(name);
    }

    public Collection<BotProcess> listBots() {
        return Collections.unmodifiableCollection(bots.values());
    }

    public List<String> getActiveBotNames() {
        return bots.keySet().stream().collect(Collectors.toList());
    }
}
