package com.grupozap.search.api.controller.error;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes;

import com.grupozap.search.api.exception.QueryPhaseExecutionException;
import com.grupozap.search.api.exception.QueryTimeoutException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import javax.servlet.http.HttpServletRequest;
import org.elasticsearch.ElasticsearchStatusException;
import org.jparsec.error.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;

@RequestScope
@Component
public class ExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandler.class);

  @Autowired private ErrorAttributes errorAttributes;

  public ResponseEntity<Map<String, Object>> error(Throwable e) {
    return error(e, ((ServletRequestAttributes) currentRequestAttributes()).getRequest());
  }

  public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
    return error(errorAttributes.getError(new DispatcherServletWebRequest(request)), request);
  }

  public ResponseEntity<Map<String, Object>> error(Throwable e, HttpServletRequest request) {
    var errorBody =
        errorAttributes.getErrorAttributes(new DispatcherServletWebRequest(request), false);

    var httpStatus = getStatusCode(e, request);
    Optional<String> rootCauseMessage = Optional.empty();

    errorBody.put("request", request.getParameterMap());
    errorBody.put("status", httpStatus.value());
    errorBody.put("error", httpStatus.getReasonPhrase());
    if (e != null) {
      rootCauseMessage = Optional.of(getRootCauseMessage(e));
      rootCauseMessage.ifPresent(msg -> errorBody.put("message", msg));
    }

    if (httpStatus.is5xxServerError())
      logErrorMsg2Appenders(e, request, errorBody, rootCauseMessage);

    return new ResponseEntity<>(errorBody, httpStatus);
  }

  private void logErrorMsg2Appenders(
      Throwable e,
      HttpServletRequest request,
      Map<String, Object> errorBody,
      Optional<String> rootCauseMessage) {
    var builder =
        new StringBuilder("Path: [" + ofNullable(request.getServletPath()).orElse("None") + "]");
    builder
        .append(" - Request Parameters: [")
        .append(getParametersFromRequest(request))
        .append("]");
    rootCauseMessage.ifPresent(
        rootCause -> builder.append(" - RootCauseMessage: [").append(rootCause).append("]"));
    var additionalMessage = additionalMessage(e);

    if (nonNull(additionalMessage)) {
      builder.append(additionalMessage);
    }

    LOG.error(builder.toString(), e);
  }

  private String additionalMessage(Throwable e) {
    if (e instanceof QueryPhaseExecutionException)
      return " - Query: [" + ((QueryPhaseExecutionException) e).getQuery() + "]";

    return null;
  }

  public static boolean isBadRequestException(Throwable e) {
    return (e instanceof IllegalArgumentException
        || getRootCause(e) instanceof IllegalArgumentException
        || e instanceof InvalidPropertyException
        || e instanceof ParserException);
  }

  private HttpStatus getStatusCode(Throwable e, HttpServletRequest request) {
    if (e != null) {
      if (e.getCause() instanceof ElasticsearchStatusException) {
        return HttpStatus.valueOf(
            ((ElasticsearchStatusException) e.getCause()).status().getStatus());
      }

      if (isBadRequestException(e)) return BAD_REQUEST;

      if (e instanceof QueryTimeoutException
          || getRootCause(e) instanceof TimeoutException
          || getRootCause(e) instanceof QueryTimeoutException) return GATEWAY_TIMEOUT;
    }

    var statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
    if (statusCode != null) {
      try {
        return HttpStatus.valueOf(statusCode);
      } catch (Exception ex) {
        LOG.error("Invalid http status code", ex);
      }
    }

    return INTERNAL_SERVER_ERROR;
  }

  private String getParametersFromRequest(final HttpServletRequest request) {
    return request.getParameterMap().entrySet().stream()
        .map(e -> format("%s=%s", e.getKey(), join(e.getValue())))
        .collect(joining("&"));
  }
}
