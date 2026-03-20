package com.lovettj.surfspotsapi.http;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Builds {@code Location} header values for {@code 201 Created} responses (RFC 9110).
 * All resources use the same pattern: path template + optional {@code userId} query when the GET route uses it.
 */
public final class CreatedResourceLocations {

  private CreatedResourceLocations() {}

  /**
   * @param pathTemplate e.g. {@code "/api/trips/{tripId}"} or {@code "/api/surf-spots/id/{id}"}
   * @param userId       if non-null, adds {@code ?userId=...}; use {@code null} when the GET route has no such param
   * @param uriVariables values for {@code {...}} placeholders, in order
   */
  public static URI fromApiPath(String pathTemplate, String userId, Object... uriVariables) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromPath(pathTemplate);
    if (userId != null) {
      builder.queryParam("userId", userId);
    }
    return builder.buildAndExpand(uriVariables).toUri();
  }
}
