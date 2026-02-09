package com.nanobot;

import com.nanobot.cli.NanobotCli;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Nanobot - High-Performance AI Agent
 * Java 21 Implementation with Virtual Threads
 */
@SpringBootApplication
@ComponentScan(
    basePackages = "com.nanobot",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {NanobotCli.class}
        )
    }
)
public class NanobotApplication {

    public static void main(String[] args) {
        // Check if running in CLI mode (no Spring Boot web)
        boolean cliMode = args.length > 0 && ("run".equals(args[0]) || "agent".equals(args[0]) || "shell".equals(args[0]));

        if (cliMode) {
            // Run in CLI mode without Spring Boot
            NanobotCli.main(args);
        } else {
            // Run Spring Boot for web/API mode
            SpringApplication.run(NanobotApplication.class, args);
        }
    }
}
