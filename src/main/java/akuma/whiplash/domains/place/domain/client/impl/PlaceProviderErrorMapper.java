package akuma.whiplash.domains.place.domain.client.impl;

import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import io.netty.handler.timeout.ReadTimeoutException;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

final class PlaceProviderErrorMapper {

    private PlaceProviderErrorMapper() {
        throw new IllegalArgumentException();
    }

    static ApplicationException map(WebClientResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        if (status.value() == 401) {
            return ApplicationException.from(PlaceErrorCode.PROVIDER_AUTHENTICATION_FAILED);
        }
        if (status.value() == 403) {
            return ApplicationException.from(PlaceErrorCode.PROVIDER_PERMISSION_DENIED);
        }
        if (status.value() == 429) {
            return ApplicationException.from(PlaceErrorCode.PROVIDER_QUOTA_EXCEEDED);
        }
        return ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
    }

    static ApplicationException map(WebClientRequestException exception) {
        if (hasTimeoutCause(exception)) {
            return ApplicationException.from(PlaceErrorCode.PROVIDER_TIMEOUT);
        }
        return ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
    }

    private static boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TimeoutException || current instanceof ReadTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
