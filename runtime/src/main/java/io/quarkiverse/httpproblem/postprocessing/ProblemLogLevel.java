package io.quarkiverse.httpproblem.postprocessing;

import org.jboss.logging.Logger;

public enum ProblemLogLevel {

    OFF {
        @Override
        public void log(Logger logger, String message) {
        }

        @Override
        public void log(Logger logger, String message, Throwable cause) {
        }
    },
    TRACE {
        @Override
        public void log(Logger logger, String message) {
            if (logger.isTraceEnabled()) {
                logger.trace(message);
            }
        }

        @Override
        public void log(Logger logger, String message, Throwable cause) {
            if (logger.isTraceEnabled()) {
                logger.trace(message, cause);
            }
        }
    },
    DEBUG {
        @Override
        public void log(Logger logger, String message) {
            if (logger.isDebugEnabled()) {
                logger.debug(message);
            }
        }

        @Override
        public void log(Logger logger, String message, Throwable cause) {
            if (logger.isDebugEnabled()) {
                logger.debug(message, cause);
            }
        }
    },
    INFO {
        @Override
        public void log(Logger logger, String message) {
            if (logger.isInfoEnabled()) {
                logger.info(message);
            }
        }

        @Override
        public void log(Logger logger, String message, Throwable cause) {
            if (logger.isInfoEnabled()) {
                logger.info(message, cause);
            }
        }
    },
    WARN {
        @Override
        public void log(Logger logger, String message) {
            if (logger.isEnabled(Logger.Level.WARN)) {
                logger.warn(message);
            }
        }

        @Override
        public void log(Logger logger, String message, Throwable cause) {
            if (logger.isEnabled(Logger.Level.WARN)) {
                logger.warn(message, cause);
            }
        }
    },
    ERROR {
        @Override
        public void log(Logger logger, String message) {
            if (logger.isEnabled(Logger.Level.ERROR)) {
                logger.error(message);
            }
        }

        @Override
        public void log(Logger logger, String message, Throwable cause) {
            if (logger.isEnabled(Logger.Level.ERROR)) {
                logger.error(message, cause);
            }
        }
    };

    public abstract void log(Logger logger, String message);

    public abstract void log(Logger logger, String message, Throwable cause);
}
