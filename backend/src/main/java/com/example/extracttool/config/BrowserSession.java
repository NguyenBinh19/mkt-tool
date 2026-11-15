package com.example.extracttool.config;

import com.example.extracttool.model.dto.FacebookBrowserStatusDTO;
import com.example.extracttool.exception.AppException;
import com.example.extracttool.exception.ErrorCode;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/** Owns one authenticated Chromium profile and limits concurrent request tabs. */
@Component
@Slf4j
public class BrowserSession {
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(20);
    private final FacebookProperties properties;
    private final Semaphore requestTabs;
    private final ReentrantLock loginLock = new ReentrantLock();
    private final Deque<String> recentBrowserOutput = new ArrayDeque<>();
    private Playwright loginConnection;
    private volatile Process browserProcess;
    private volatile Page loginPage;
    private volatile boolean stopping;
    private volatile boolean starting;
    private volatile boolean restarting;
    private volatile String startupError;
    private volatile String authentication = "UNKNOWN";

    public BrowserSession(FacebookProperties properties) {
        this.properties = properties;
        if (properties.getMaxConcurrentTabs() < 1) throw new IllegalArgumentException("facebook.max-concurrent-tabs must be at least 1");
        this.requestTabs = new Semaphore(properties.getMaxConcurrentTabs(), true);
    }

    /** Prepare the server-owned browser without delaying the API startup on Facebook navigation. */
    @EventListener(ApplicationReadyEvent.class)
    public void prepareLoginPage() {
        starting = true;
        Thread startup = new Thread(() -> {
            try {
                openLoginPage();
            } catch (RuntimeException exception) {
                startupError = exception.getMessage();
                if (!stopping) log.warn("Facebook browser could not be prepared; an administrator can retry from the dashboard: {}", exception.getMessage());
            } finally {
                starting = false;
            }
        }, "facebook-browser-startup");
        startup.setDaemon(true);
        startup.start();
    }

    /** Recover an owned browser process that exited unexpectedly; a live session is never restarted automatically. */
    @Scheduled(initialDelay = 30000, fixedDelay = 30000)
    public void retryAfterCrash() {
        Process current = browserProcess;
        if (stopping || starting || restarting || current == null || current.isAlive()) return;
        if (requestTabs.availablePermits() != properties.getMaxConcurrentTabs()) return;
        try {
            retry();
            log.info("Facebook browser was restarted after an unexpected exit");
        } catch (RuntimeException exception) {
            if (!stopping) log.warn("Facebook browser restart failed: {}", exception.getMessage());
        }
    }

    private String cdpEndpoint() { return "http://127.0.0.1:" + properties.getDebugPort(); }

    /** Playwright's persistent launch uses a pipe and cannot reliably expose a second CDP port on Windows. */
    private synchronized void ensureBrowserStarted() {
        if (browserProcess != null && browserProcess.isAlive() && isCdpReady()) return;
        if (browserProcess != null) {
            if (browserProcess.isAlive()) {
                throw new IllegalStateException("Chromium is running but its private control endpoint is unavailable. "
                        + "Close the old project Chromium window and restart the backend.");
            }
            closeOperatorConnections();
            browserProcess = null;
            authentication = "UNKNOWN";
        }
        if (isCdpReady()) {
            throw new IllegalStateException("Facebook browser debug port " + properties.getDebugPort()
                    + " is already in use. Close the other browser or configure FACEBOOK_DEBUG_PORT.");
        }

        Path profile = Path.of(properties.getProfileDirectory()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(profile);
            String browserBinary;
            try (Playwright launcher = Playwright.create()) {
                browserBinary = launcher.chromium().executablePath();
            }
            List<String> command = new ArrayList<>(List.of(browserBinary,
                    "--user-data-dir=" + profile,
                    "--remote-debugging-port=" + properties.getDebugPort(),
                    "--remote-debugging-address=127.0.0.1",
                    "--no-first-run", "--no-default-browser-check", "--disable-dev-shm-usage",
                    "--password-store=basic", "--use-mock-keychain",
                    "--window-size=1366,900"));
            if (properties.isHeadless()) command.add("--headless=new");
            if ("root".equals(System.getProperty("user.name"))) command.add("--no-sandbox");
            command.add("about:blank");
            browserProcess = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            watchBrowserOutput(browserProcess);
            waitForCdp();
        } catch (IOException exception) {
            browserProcess = null;
            throw new IllegalStateException("Cannot start Chromium for the Facebook profile " + profile, exception);
        } catch (RuntimeException exception) {
            if (browserProcess != null) browserProcess.destroy();
            browserProcess = null;
            throw exception;
        }
    }

    private void waitForCdp() {
        long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (isCdpReady()) return;
            if (!browserProcess.isAlive()) break;
            try {
                Thread.sleep(250);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while starting Chromium", exception);
            }
        }
        String output = recentBrowserOutput();
        if (!output.isBlank()) log.warn("Chromium startup output: {}", output);
        throw new IllegalStateException("Chromium did not open its local debugging endpoint. "
                + "Close any old project Chromium window using this profile and restart the backend.");
    }

    private void watchBrowserOutput(Process process) {
        synchronized (recentBrowserOutput) { recentBrowserOutput.clear(); }
        Thread reader = new Thread(() -> {
            try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = output.readLine()) != null) {
                    synchronized (recentBrowserOutput) {
                        if (recentBrowserOutput.size() == 20) recentBrowserOutput.removeFirst();
                        recentBrowserOutput.addLast(line);
                    }
                }
            } catch (IOException exception) {
                log.debug("Stopped reading Chromium output: {}", exception.getMessage());
            }
        }, "facebook-browser-output");
        reader.setDaemon(true);
        reader.start();
    }

    private String recentBrowserOutput() {
        synchronized (recentBrowserOutput) { return String.join(" | ", recentBrowserOutput); }
    }

    private boolean isCdpReady() {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(cdpEndpoint() + "/json/version"))
                    .timeout(Duration.ofSeconds(1)).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while checking Chromium", exception);
        }
    }

    /** Each worker owns its Playwright connection. Only Chromium's profile is shared. */
    public TaskPage openTaskPage() {
        try {
            requestTabs.acquire();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a Facebook browser tab", exception);
        }
        Playwright worker = null;
        try {
            if (stopping) throw new IllegalStateException("Facebook browser is shutting down");
            ensureBrowserStarted();
            worker = Playwright.create();
            Browser browser = worker.chromium().connectOverCDP(cdpEndpoint());
            BrowserContext shared = browser.contexts().get(0);
            shared.setDefaultTimeout(properties.getNavigationTimeoutMs());
            shared.setDefaultNavigationTimeout(properties.getNavigationTimeoutMs());
            return new TaskPage(worker, shared.newPage(), requestTabs);
        } catch (RuntimeException exception) {
            if (worker != null) worker.close();
            requestTabs.release();
            throw exception;
        }
    }

    /** Leaves one login tab visible for the operator. Repeated clicks are idempotent. */
    public void openLoginPage() {
        synchronized (this) {
            if (stopping) throw new IllegalStateException("Facebook browser is shutting down");
            try {
                ensureBrowserStarted();
            } catch (RuntimeException exception) {
                startupError = exception.getMessage();
                throw exception;
            }
        }
        loginLock.lock();
        try {
            if (loginPage != null && !loginPage.isClosed()) return;
            closeOperatorConnections();
            Playwright operator = Playwright.create();
            try {
                Browser browser = operator.chromium().connectOverCDP(cdpEndpoint());
                Page page = browser.contexts().get(0).newPage();
                loginConnection = operator;
                loginPage = page;
                page.navigate("https://www.facebook.com/", new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.COMMIT).setTimeout(properties.getNavigationTimeoutMs()));
                startupError = null;
            } catch (RuntimeException exception) {
                startupError = exception.getMessage();
                if (loginConnection == operator) closeOperatorConnections();
                else operator.close();
                throw exception;
            }
        } finally {
            loginLock.unlock();
        }
    }

    public FacebookBrowserStatusDTO status() {
        boolean ready = browserProcess != null && browserProcess.isAlive() && isCdpReady();
        boolean pageOpen = ready && loginPage != null;
        if (pageOpen && loginLock.tryLock()) {
            try { Page current = loginPage; pageOpen = current != null && !current.isClosed(); }
            catch (RuntimeException ignored) { pageOpen = false; }
            finally { loginLock.unlock(); }
        }
        String browser = restarting || starting ? "STARTING" : ready ? "READY" : "UNAVAILABLE";
        return new FacebookBrowserStatusDTO(browser, ready ? authentication : "UNKNOWN", pageOpen,
                properties.getMaxConcurrentTabs(), restarting ? 0 : properties.getMaxConcurrentTabs() - requestTabs.availablePermits(), startupError);
    }

    public void markAuthenticationRequired() { authentication = "REQUIRED"; }
    public void markAuthenticated() { authentication = "READY"; }

    /** Restarts only this application's Chromium process; never takes over another process on the debug port. */
    public void retry() {
        int capacity = properties.getMaxConcurrentTabs();
        if (!requestTabs.tryAcquire(capacity)) {
            throw new AppException(ErrorCode.BROWSER_BUSY);
        }
        restarting = true;
        try {
            synchronized (this) {
                stopOwnedBrowser();
                closeOperatorConnections();
                authentication = "UNKNOWN";
                startupError = null;
            }
            openLoginPage();
        } finally {
            restarting = false;
            requestTabs.release(capacity);
        }
    }

    public static final class TaskPage implements AutoCloseable {
        private final Playwright worker;
        private final Page page;
        private final Semaphore slots;

        private TaskPage(Playwright worker, Page page, Semaphore slots) {
            this.worker = worker;
            this.page = page;
            this.slots = slots;
        }

        public Page page() { return page; }

        @Override public void close() {
            try { page.close(); } finally {
                try { worker.close(); } finally { slots.release(); }
            }
        }
    }

    @PreDestroy
    public synchronized void close() {
        stopping = true;
        stopOwnedBrowser();
        closeOperatorConnections();
    }

    private void closeOperatorConnections() {
        loginLock.lock();
        try {
            if (loginConnection != null) loginConnection.close();
            loginConnection = null;
            loginPage = null;
        } finally {
            loginLock.unlock();
        }
    }

    private void stopOwnedBrowser() {
        if (browserProcess != null) {
            Process ownedBrowser = browserProcess;
            browserProcess = null;
            List<ProcessHandle> children = ownedBrowser.descendants().toList();
            children.forEach(ProcessHandle::destroy);
            ownedBrowser.destroy();
            try {
                if (!ownedBrowser.waitFor(5, TimeUnit.SECONDS)) ownedBrowser.destroyForcibly();
                for (ProcessHandle child : children) {
                    if (child.isAlive()) child.destroyForcibly();
                }
                ownedBrowser.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                ownedBrowser.destroyForcibly();
            }
        }
    }
}
