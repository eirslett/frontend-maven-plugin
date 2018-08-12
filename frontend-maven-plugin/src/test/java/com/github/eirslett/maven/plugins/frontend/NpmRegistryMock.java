package com.github.eirslett.maven.plugins.frontend;

import org.mockserver.client.netty.NettyHttpClient;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.initialize.ExpectationInitializer;
import org.mockserver.mock.action.ExpectationCallback;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.FORBIDDEN_403;
import static org.mockserver.model.HttpStatusCode.OK_200;

public class NpmRegistryMock implements ExpectationInitializer {
    private static final String NPM_REGISTRY_HOST = "registry.npmjs.org";
    private static final int NPM_REGISTRY_PORT = 443;
    private static final String NPM_USERNAME = "username";
    private static final String NPM_PASSWORD = "password";
    private static final String NPM_TOKEN = "NpmToken.22e3a730-9e62-11e8-98d0-529269fb1459";

    private static final String HEADER_HOST = "Host";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String MIME_TYPE_APPLICATION_JSON = "application/json";

    private final NettyHttpClient httpClient = new NettyHttpClient();

    @Override
    public void initializeExpectations(MockServerClient mockServerClient) {
        mockServerClient.when(
                request()
                        .withMethod("PUT")
                        .withPath("/-/user/org.couchdb.user:" + NPM_USERNAME)
                        .withHeader(HEADER_CONTENT_TYPE, MIME_TYPE_APPLICATION_JSON)
                        .withBody("{\"name\":\"" + NPM_USERNAME + "\",\"password\":\"" + NPM_PASSWORD + "\"}")
        )
                .respond(
                        response()
                                .withStatusCode(OK_200.code())
                                .withHeaders(
                                        header(HEADER_CONTENT_TYPE, MIME_TYPE_APPLICATION_JSON)
                                )
                                .withBody("{\"rev\":\"_we_dont_use_revs_any_more\",\"id\":\"org.couchdb.user:undefined\"," +
                                        "\"ok\":\"true\",\"token\":\"" + NPM_TOKEN + "\"}"));


        mockServerClient.when(request()).callback(new ExpectationCallback() {
            @Override
            public HttpResponse handle(HttpRequest httpRequest) {
                httpRequest.withSecure(true);
                List<Header> headers = new ArrayList<>();
                for (Header header : httpRequest.getHeaderList()) {
                    if (!HEADER_HOST.equalsIgnoreCase(header.getName().getValue())) {
                        headers.add(header);
                    }
                }
                httpRequest = httpRequest.clone()
                        .withHeaders(headers)
                        .withHeader(HEADER_HOST, NPM_REGISTRY_HOST);

                List<String> authorization = httpRequest.getHeader(HEADER_AUTHORIZATION);
                if (null != authorization && !authorization.isEmpty()) {
                    if (!authorization.get(0).replace("Bearer ", "").equals(NPM_TOKEN)) {
                        return HttpResponse
                                .response()
                                .withStatusCode(FORBIDDEN_403.code());
                    }
                }
                return httpClient.sendRequest(
                        httpRequest,
                        new InetSocketAddress(NPM_REGISTRY_HOST, NPM_REGISTRY_PORT)
                );
            }
        });
    }
}
