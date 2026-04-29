package com.lovettj.surfspotsapi.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpExtractorTests {

    @Test
    void extractShouldReturnRemoteAddrWhenNoForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.5");

        assertEquals("203.0.113.5", ClientIpExtractor.extract(request));
    }

    @Test
    void extractShouldReturnFirstEntryFromForwardedHeader() {
        // X-Forwarded-For format: "client, proxy1, proxy2" — we only ever trust
        // the left-most entry for rate-limiting purposes.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1, 10.0.0.2");
        request.setRemoteAddr("10.0.0.2");

        assertEquals("203.0.113.5", ClientIpExtractor.extract(request));
    }

    @Test
    void extractShouldTrimForwardedEntry() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "   198.51.100.1   , 10.0.0.1");
        request.setRemoteAddr("10.0.0.1");

        assertEquals("198.51.100.1", ClientIpExtractor.extract(request));
    }

    @Test
    void extractShouldFallBackToRemoteAddrWhenForwardedIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "   ");
        request.setRemoteAddr("198.51.100.10");

        assertEquals("198.51.100.10", ClientIpExtractor.extract(request));
    }

    @Test
    void extractShouldReturnUnknownWhenNoAddressAvailable() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(null);

        assertEquals("unknown", ClientIpExtractor.extract(request));
    }

    @Test
    void extractShouldReturnUnknownForNullRequest() {
        // Defensive: the resolver delegates to this for both web and ad-hoc
        // callers; a null request must not NPE.
        assertEquals("unknown", ClientIpExtractor.extract(null));
    }
}
