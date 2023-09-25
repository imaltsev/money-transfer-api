package dev.maltsev.money.transfer.api;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Parameters {

    @Parameter(names = {"--port", "-p"}, description = "The port number on which the server should run. Default is 8080.", arity = 1)
    private int port = 8080;

    @Parameter(names = {"--withdrawalWorkerPoolSize", "-w"},
            description = "Amount of worker threads used to handle withdrawal transactions simultaneously. Default is 20.",
            arity = 1)
    private int withdrawalWorkerPoolSize = 20;

    @Parameter(names = {"--transferWorkerPoolSize", "-t"},
            description = "Amount of worker threads used to handle transfer transactions simultaneously. Default is 10.",
            arity = 1)
    private int transferWorkerPoolSize = 10;

    @Parameter(names = "--help", description = "To read this help ;)", help = true)
    private boolean help;

    @Parameter(names = {"--recoveryInterval", "-r"}, description = "An interval in milliseconds between transaction recovery attempts. Default is 60000.",
            arity = 1)
    private long recoveryInterval = 60_000;

    @Parameter(names = {"--shutdownHookTimeout", "-s"},
            description = "A timeout in milliseconds for graceful shutdown hook to wait for the server to stop. Default is 10000.",
            arity = 1)
    private long shutdownHookTimeout = 10_000;
}
