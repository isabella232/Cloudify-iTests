package org.cloudifysource.quality.iTests.test.cli.cloudify.util.exceptions;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.Map;

/**
 * Exception class indicating some sort of error happened while trying to uninstall the services and applications
 * after a test has finished.
 *
 * @author Eli Polonsky
 */
public class FailedToCleanupEnvironmentAfterTestException extends Exception {

    private Map<String, Throwable> serviceErrors;
    private Map<String, Throwable> applicationErrors;

    public FailedToCleanupEnvironmentAfterTestException(final Map<String, Throwable> serviceErrors,
                                                        final Map<String, Throwable> applicationErrors) {
        super("Encountered erros while cleaning service:" + message(serviceErrors)
                + "\\n" + "Encountered erros while cleaning service:" + message(applicationErrors));
        this.applicationErrors = applicationErrors;
        this.serviceErrors = serviceErrors;
    }

    public Map<String, Throwable> getServiceErrors() {
        return serviceErrors;
    }

    public Map<String, Throwable> getApplicationErrors() {
        return applicationErrors;
    }

    private static String message(final Map<String, Throwable> errors) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Throwable> entry : errors.entrySet()) {
            builder.append(entry.getKey()).append(" : ").append(ExceptionUtils.getFullStackTrace(entry.getValue()));
        }
        return builder.toString();
    }
}
