package io.quarkiverse.httpproblem;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.logging.Logger;

public final class InstanceUtils {

    private static final Logger LOG = Logger.getLogger(InstanceUtils.class);

    public static URI pathToInstance(String path) {
        if (path == null) {
            return null;
        }
        try {
            return new URI(null, null, path, null);
        } catch (URISyntaxException e) {
            LOG.warnf("Could not convert path to URI instance: %s", path);
            return null;
        }
    }

    public static String instanceToPath(URI instance) {
        return instance.toASCIIString();
    }

}
