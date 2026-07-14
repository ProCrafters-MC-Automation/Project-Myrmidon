package org.pmca.project_myrmidon.client.bot;

public class BotProcess {

    private final String name;
    private final Process process;

    public BotProcess(String name, Process process) {
        this.name = name;
        this.process = process;
    }

    public String getName() {
        return name;
    }

    public Process getProcess() {
        return process;
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public void kill() {
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }
}
