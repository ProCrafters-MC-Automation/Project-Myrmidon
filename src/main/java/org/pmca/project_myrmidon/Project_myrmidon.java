package org.pmca.project_myrmidon;

import net.fabricmc.api.ModInitializer;

public class Project_myrmidon implements ModInitializer {

    @Override
    public void onInitialize() {
        if ("true".equals(System.getProperty("project_myrmidon.bot"))) {
            String botName = System.getProperty("project_myrmidon.bot_name", "unknown");
            System.out.println("[Project Myrmidon] Running as bot: " + botName);
        }
    }
}
