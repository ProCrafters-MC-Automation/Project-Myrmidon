package org.pmca.project_myrmidon.client.bot;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BotConfig {

    private final String name;
    private final String serverAddress;
    private final int serverPort;
    private final List<Path> modPaths;
    private final List<String> jvmArgs;

    private BotConfig(Builder builder) {
        this.name = builder.name;
        this.serverAddress = builder.serverAddress;
        this.serverPort = builder.serverPort;
        this.modPaths = Collections.unmodifiableList(new ArrayList<>(builder.modPaths));
        this.jvmArgs = Collections.unmodifiableList(new ArrayList<>(builder.jvmArgs));
    }

    public String getName() {
        return name;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }

    public List<Path> getModPaths() {
        return modPaths;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String serverAddress = "localhost";
        private int serverPort = 25565;
        private final List<Path> modPaths = new ArrayList<>();
        private final List<String> jvmArgs = new ArrayList<>();

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder serverAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder serverPort(int serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        public Builder addMod(Path modPath) {
            this.modPaths.add(modPath);
            return this;
        }

        public Builder addMods(List<Path> modPaths) {
            this.modPaths.addAll(modPaths);
            return this;
        }

        public Builder jvmArg(String arg) {
            this.jvmArgs.add(arg);
            return this;
        }

        public BotConfig build() {
            if (name == null || name.isBlank()) {
                throw new IllegalStateException("Bot name must not be null or blank");
            }
            return new BotConfig(this);
        }
    }
}
