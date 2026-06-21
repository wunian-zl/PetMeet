package org.petmeet.support;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DevProcessLauncher {

    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(5);

    private final Environment environment;
    private final List<ManagedProcess> managedProcesses = new CopyOnWriteArrayList<>();
    private volatile boolean shuttingDown;
    private volatile boolean launched;

    @EventListener(ApplicationReadyEvent.class)
    public void launch() {
        if (launched || isSpringBootTest() || !isEnabled("petmeet.dev.launch-all", false)) {
            return;
        }
        launched = true;

        Path projectRoot = resolveProjectRoot();
        log.info("Dev launch-all enabled. Project root: {}", projectRoot);

        if (isEnabled("petmeet.dev.launch-redis", true)) {
            startRedisIfNeeded(projectRoot);
        }
        if (isEnabled("petmeet.dev.launch-frontends", true)) {
            startFrontends(projectRoot);
        }
    }

    private void startRedisIfNeeded(Path projectRoot) {
        Path script = projectRoot.resolve("scripts").resolve("start-redis-if-needed.ps1");
        if (!Files.isRegularFile(script)) {
            log.warn("Redis startup script not found: {}", script);
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(isWindows() ? "powershell.exe" : "pwsh");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-File");
        command.add(script.toString());

        try {
            Process process = new ProcessBuilder(command)
                    .directory(projectRoot.toFile())
                    .inheritIO()
                    .start();
            int code = process.waitFor();
            if (code == 0) {
                log.info("Redis startup check completed.");
            } else {
                log.warn("Redis startup script exited with code {}.", code);
            }
        } catch (IOException e) {
            log.warn("Redis startup script failed to execute: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Redis startup check was interrupted.");
        }
    }

    private void startFrontends(Path projectRoot) {
        Path script = projectRoot.resolve("scripts").resolve("dev-frontends.mjs");
        if (!Files.isRegularFile(script)) {
            log.warn("Frontend startup script not found: {}", script);
            return;
        }

        List<String> command = List.of("node", script.toString());
        try {
            Process process = new ProcessBuilder(command)
                    .directory(projectRoot.toFile())
                    .inheritIO()
                    .start();
            ManagedProcess managedProcess = new ManagedProcess("frontends", process);
            managedProcesses.add(managedProcess);
            watch(managedProcess);
            log.info("Frontend dev servers are starting with PID {}.", process.pid());
        } catch (IOException e) {
            log.warn("Frontend dev servers failed to start: {}", e.getMessage());
        }
    }

    private void watch(ManagedProcess managedProcess) {
        Thread watcher = new Thread(() -> {
            try {
                int code = managedProcess.process().waitFor();
                managedProcesses.remove(managedProcess);
                if (!shuttingDown) {
                    log.warn("{} process exited with code {}.", managedProcess.name(), code);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "petmeet-dev-process-" + managedProcess.name());
        watcher.setDaemon(true);
        watcher.start();
    }

    private Path resolveProjectRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        Path cursor = current;
        for (int i = 0; i < 4 && cursor != null; i++) {
            if (isProjectRoot(cursor)) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Cannot find PetMeet project root from " + current);
    }

    private boolean isProjectRoot(Path path) {
        return Files.isDirectory(path.resolve("PetMeet-frontend"))
                && Files.isRegularFile(path.resolve("scripts").resolve("dev-frontends.mjs"));
    }

    private boolean isEnabled(String key, boolean defaultValue) {
        return environment.getProperty(key, Boolean.class, defaultValue);
    }

    private boolean isSpringBootTest() {
        return environment.getProperty(
                "org.springframework.boot.test.context.SpringBootTestContextBootstrapper",
                Boolean.class,
                false);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    @PreDestroy
    public void stopManagedProcesses() {
        shuttingDown = true;
        for (ManagedProcess managedProcess : managedProcesses) {
            stop(managedProcess);
        }
        managedProcesses.clear();
    }

    private void stop(ManagedProcess managedProcess) {
        Process process = managedProcess.process();
        if (!process.isAlive()) {
            return;
        }

        log.info("Stopping {} process tree, PID {}.", managedProcess.name(), process.pid());
        if (isWindows()) {
            stopWindowsProcessTree(process);
        } else {
            process.destroy();
        }
        waitOrDestroy(process);
    }

    private void stopWindowsProcessTree(Process process) {
        try {
            new ProcessBuilder("taskkill", "/pid", Long.toString(process.pid()), "/T", "/F")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                    .waitFor(STOP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            log.warn("taskkill failed to start: {}", e.getMessage());
            process.destroy();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroy();
        }
    }

    private void waitOrDestroy(Process process) {
        try {
            if (!process.waitFor(STOP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private record ManagedProcess(String name, Process process) {
    }
}
