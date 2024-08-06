/* 
 * Copyright (C) 2020 Alexander Stojanovich <coas91@rocketmail.DSLogger>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rs.alexanderstojanovich.evg.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.json.JsonConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.DSLogger>
 */
public class DSLogger {

    public static enum DSLogLevel {
        ERR, DEBUG, ALL
    }

    public static org.apache.logging.log4j.core.Logger INTERNAL_LOGGER;

    protected static org.apache.logging.log4j.core.layout.PatternLayout DSLoggerLayout;

    /**
     * Generate log file name (internal)
     *
     * @return filename as string
     */
    private static String generateLogFileName() { // such as "dsynergy_2020-20-10_15-20-42.log"
        final LocalDateTime dateTime = LocalDateTime.now();
        StringBuilder sb = new StringBuilder();
        sb.append("dsynergy_");
        sb.append(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
        sb.append(".log");
        return sb.toString();
    }

    /**
     * Initalize logging with optional logging to file.
     *
     * @param logLevel {ERROR, DEBUG, INFO}
     * @param logToFile logs to file (besides console if set to true)
     */
    public static void init(DSLogLevel logLevel, boolean logToFile) {
        try {
            // YamlConfigurationFactory & JsonConfigurationFactory
            // Get context instance
            LoggerContext context = new LoggerContext("DSynergyContext");
            // Get a reference from configuration                                     // log4j configuration
            File cfgFile = new File("./log4j2.json");
            if (!cfgFile.exists()) {
                throw new RuntimeException("Logging cannot be initialized. Json file is missing!");
            }
            ConfigurationSource source = new ConfigurationSource(new FileInputStream(cfgFile));
            Configuration configuration = new JsonConfiguration(context, source);
            DSLoggerLayout = PatternLayout.createDefaultLayout(configuration);
            // Create default console appender
            ConsoleAppender appender = ConsoleAppender.createDefaultAppenderForLayout(DSLoggerLayout);
            // Add console appender into configuration            
            configuration.addAppender(appender);
            // Create loggerConfig
            org.apache.logging.log4j.Level level;
            switch (logLevel) {
                default:
                case ERR:
                    level = org.apache.logging.log4j.Level.ERROR;
                    break;
                case ALL:
                    level = org.apache.logging.log4j.Level.INFO;
                    break;
                case DEBUG:
                    level = org.apache.logging.log4j.Level.DEBUG;
                    break;
            }

            LoggerConfig loggerConfig = new LoggerConfig("DSLogger", level, false);
            // Add logger and associate it with loggerConfig instance
            configuration.addLogger("DSLogger", loggerConfig);
            // Start logging system
            context.start(configuration);
            // Get a reference for logger
            INTERNAL_LOGGER = context.getLogger("DSLogger");
            //----------------------------------------------------------------------
            if (logToFile) { // if debug it's gonna store messages in both console and the file otherwise just in the file
                final String logFileName = generateLogFileName();
                FileAppender fileAppender = FileAppender
                        .newBuilder()
                        .withFileName(logFileName)
                        .setBufferSize(8192)
                        .setName("DSLoggerFile")
                        .setConfiguration(configuration)
                        .setLayout(DSLoggerLayout).build();
                fileAppender.start();
                INTERNAL_LOGGER.addAppender(fileAppender);
            }
            // Add logger and associate it with loggerConfig instance
            configuration.addLogger("DSLogger", loggerConfig);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DSLogger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DSLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // used when catchin exceptions
    // as they would cause application to crash
    // replacing NetBeans default logger ones
    /**
     * Report errors which prevents program from continuing normally (program
     * crash).
     *
     * @param msg message
     * @param t exception (thorwable)
     */
    public static void reportFatalError(String msg, Throwable t) {
        INTERNAL_LOGGER.fatal(msg, t);
    }

    /**
     *
     * Report less severe error.
     *
     * @param msg message
     * @param t exception (throwable)
     */
    public static void reportError(String msg, Throwable t) {
        INTERNAL_LOGGER.error(msg, t);
    }

    /**
     * Report warning. Warnings are treated are much less severe than errors.
     * When something unordinary occurs which may lead to errors.
     *
     * @param msg warning message
     * @param t exception (throwable)
     */
    public static void reportWarning(String msg, Throwable t) {
        INTERNAL_LOGGER.warn(msg, t);
    }

    /**
     * Report program debug information. Classified as something which can be
     * useful or ignored. Or a hint.
     *
     * @param msg message
     * @param t exception (throwable)
     */
    public static void reportDebug(String msg, Throwable t) {
        INTERNAL_LOGGER.debug(msg, t);
    }

    /**
     * Report program debug information. Classified as program information. Can
     * be ignored. (Not used).
     *
     * @param msg message
     * @param t exception (throwable)
     */
    public static void reportInfo(String msg, Throwable t) {
        INTERNAL_LOGGER.info(msg, t);
    }

}
