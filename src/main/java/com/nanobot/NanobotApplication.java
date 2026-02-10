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
        // Check if running in interactive CLI mode
        boolean cliMode = args.length > 0 && ("run".equals(args[0]) || "agent".equals(args[0]) || "shell".equals(args[0]));

        if (cliMode) {
            // Run in interactive CLI mode without Spring Boot container
            NanobotCli.main(args);
        } else {
            // Run Spring Boot for dependency injection and component management
            // Note: This is NOT a web server - Nanobot is a pure CLI application
            SpringApplication.run(NanobotApplication.class, args);
        }
    }
}
