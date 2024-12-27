package io.github.abinjoseplappallil.api.limit;

import io.github.abinjoseplappallil.api.limit.internal.Limiter;
import net.jcip.annotations.ThreadSafe;

import java.util.*;

/**
 * Class to consume API calls on behalf of a client and
 * to limit the API calls that a client can consume within a certain timeframe.
 */
@ThreadSafe
public final class ApiLimiter {
    private final static ApiLimiter INSTANCE = new ApiLimiter();
    private final Map<String, Map<String, Limiter>> apiLimiterMap = new HashMap<>();
    private final List<String> rootApis = new ArrayList<>();

    private ApiLimiter() {}

    /**
     * It registers the APIs to limit.
     * @param apis the apis
     */
    public static void registerApis(ApiConfig... apis) {
        synchronized (INSTANCE) {
            Arrays.stream(apis).forEach(api -> {
                INSTANCE.apiLimiterMap.computeIfAbsent(api.getApiName(), k -> new HashMap<>()).put(api.getClient(), new Limiter(api));
                if (api.getApiName().endsWith("*")) {
                    INSTANCE.rootApis.add(api.getApiName().substring(0, api.getApiName().length() - 1));
                }
            });
        }
    }

    /**
     * It returns the name of configured APIs.
     * @return the list with the names of the APIs
     */
    public static List<String> getConfiguredApisName() {
        return new ArrayList<>(INSTANCE.apiLimiterMap.keySet());
    }

    /**
     * It checks whether an api is configured.
     * @param apiName the api name
     * @return true if the api is configured, false otherwise
     */
    public static boolean isApiConfigured(String apiName) {
        if (apiName == null) {
            return false;
        }

        return INSTANCE.apiLimiterMap.containsKey(apiName);
    }

    /**
     * It consumes an API.
     * @param apiName the api name
     * @return true if consumed successfully, false if the current API call exceeds
     * the configured API maximum calls in the configured API timeframe
     * @throws ApiLimiterException if api name is null or not registered, or client is null or not found
     */
    public static boolean consume(String apiName) {
        return consume(apiName, ApiConfig.ALL_CLIENTS);
    }

    /**
     * It consumes an API on behalf of a specific client.
     * @param apiName the api name
     * @param client the client name (ignored if the API was configured for all clients)
     * @return true if consumed successfully, false if the current API call exceeds
     * the configured API maximum calls within the configured API timeframe
     * @throws ApiLimiterException if api name is null or not registered, or client is null or not found
     */
    public static boolean consume(String apiName, String client) {

        if (apiName == null) {
            throw new ApiLimiterException("API name cannot be null");
        }

        for (String rootApi: INSTANCE.rootApis) {
            if (apiName.startsWith(rootApi)) {
                apiName = rootApi + "*";
                break;
            }
        }

        Limiter limiter;
        if (INSTANCE.apiLimiterMap.containsKey(apiName)) {
            Map<String, Limiter> clientLimiterMap = INSTANCE.apiLimiterMap.get(apiName);

            synchronized (clientLimiterMap) {
                if (clientLimiterMap.containsKey(ApiConfig.ALL_CLIENTS)) {
                    limiter = clientLimiterMap.get(ApiConfig.ALL_CLIENTS);
                    client = ApiConfig.ALL_CLIENTS;
                } else if (client == null) {
                    throw new ApiLimiterException("Client cannot be null");
                } else if (clientLimiterMap.containsKey(client)) {
                    limiter = clientLimiterMap.get(client);
                } else {
                    throw new ApiLimiterException(String.format("Client %s non found for API %s", client, apiName));
                }
            }
        } else {
            throw new ApiLimiterException(String.format("API %s not registered", apiName));
        }

        return limiter.consume(client);
    }
}
