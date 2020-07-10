package org.apereo.cas.authentication.principal;

import org.apereo.cas.CasProtocolConstants;
import org.apereo.cas.services.DefaultServicesManager;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.services.ServiceRegistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Scott Battaglia
 * @author Arnaud Lesueur
 * @since 3.1
 */
@Tag("Simple")
public class SimpleWebApplicationServiceImplTests {
    private static final File JSON_FILE = new File(FileUtils.getTempDirectoryPath(), "simpleWebApplicationServiceImpl.json");

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private static final String SERVICE = "service";

    @Test
    public void verifySerializeACompletePrincipalToJson() throws IOException {
        val request = new MockHttpServletRequest();
        request.setParameter(CasProtocolConstants.PARAMETER_SERVICE, SERVICE);
        val serviceWritten = new WebApplicationServiceFactory().createService(request);
        MAPPER.writeValue(JSON_FILE, serviceWritten);
        val serviceRead = MAPPER.readValue(JSON_FILE, SimpleWebApplicationServiceImpl.class);
        assertEquals(serviceWritten, serviceRead);
    }

    @Test
    public void verifyResponse() {
        val applicationContext = new StaticApplicationContext();
        applicationContext.refresh();

        val request = new MockHttpServletRequest();
        request.setParameter(CasProtocolConstants.PARAMETER_SERVICE, SERVICE);
        val impl = new WebApplicationServiceFactory().createService(request);
        val response = new WebApplicationServiceResponseBuilder(
            new DefaultServicesManager(mock(ServiceRegistry.class), applicationContext, new HashSet<>(), Caffeine.newBuilder().build()))
            .build(impl, "ticketId", RegisteredServiceTestUtils.getAuthentication());
        assertNotNull(response);
        assertEquals(Response.ResponseType.REDIRECT, response.getResponseType());
    }

    @Test
    public void verifyCreateSimpleWebApplicationServiceImplFromServiceAttribute() {
        val request = new MockHttpServletRequest();
        request.setAttribute(CasProtocolConstants.PARAMETER_SERVICE, SERVICE);
        val impl = new WebApplicationServiceFactory().createService(request);
        assertNotNull(impl);
    }

    @Test
    public void verifyResponseForJsession() {
        val request = new MockHttpServletRequest();
        request.setParameter(CasProtocolConstants.PARAMETER_SERVICE, "http://www.cnn.com/;jsession=test");
        val impl = new WebApplicationServiceFactory().createService(request);

        assertEquals("http://www.cnn.com/", impl.getId());
    }

    @Test
    public void verifyResponseWithNoTicket() {
        val request = new MockHttpServletRequest();
        request.setParameter(CasProtocolConstants.PARAMETER_SERVICE, SERVICE);
        val impl = new WebApplicationServiceFactory().createService(request);

        val applicationContext = new StaticApplicationContext();
        applicationContext.refresh();

        val response = new WebApplicationServiceResponseBuilder(
            new DefaultServicesManager(mock(ServiceRegistry.class), applicationContext, new HashSet<>(), Caffeine.newBuilder().build()))
            .build(impl, null,
                RegisteredServiceTestUtils.getAuthentication());
        assertNotNull(response);
        assertEquals(Response.ResponseType.REDIRECT, response.getResponseType());
        assertFalse(response.getUrl().contains("ticket="));
    }

    @Test
    public void verifyResponseWithNoTicketAndNoParameterInServiceURL() {
        val applicationContext = new StaticApplicationContext();
        applicationContext.refresh();

        val request = new MockHttpServletRequest();
        request.setParameter(SERVICE, "http://foo.com/");
        val impl = new WebApplicationServiceFactory().createService(request);
        val response = new WebApplicationServiceResponseBuilder(
            new DefaultServicesManager(mock(ServiceRegistry.class), applicationContext, new HashSet<>(), Caffeine.newBuilder().build()))
            .build(impl, null,
                RegisteredServiceTestUtils.getAuthentication());
        assertNotNull(response);
        assertEquals(Response.ResponseType.REDIRECT, response.getResponseType());
        assertFalse(response.getUrl().contains("ticket="));
        assertEquals("http://foo.com/", response.getUrl());
    }

    @Test
    public void verifyResponseWithNoTicketAndOneParameterInServiceURL() {
        val applicationContext = new StaticApplicationContext();
        applicationContext.refresh();

        val request = new MockHttpServletRequest();
        request.setParameter(CasProtocolConstants.PARAMETER_SERVICE, "http://foo.com/?param=test");
        val impl = new WebApplicationServiceFactory().createService(request);
        val response = new WebApplicationServiceResponseBuilder(
            new DefaultServicesManager(mock(ServiceRegistry.class), applicationContext, new HashSet<>(), Caffeine.newBuilder().build()))
            .build(impl, null, RegisteredServiceTestUtils.getAuthentication());
        assertNotNull(response);
        assertEquals(Response.ResponseType.REDIRECT, response.getResponseType());
        assertEquals("http://foo.com/?param=test", response.getUrl());
    }
}
