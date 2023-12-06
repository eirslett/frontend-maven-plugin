package com.github.eirslett.maven.plugins;

import org.mockserver.client.MockServerClient;
import org.mockserver.client.initialize.PluginExpectationInitializer;

import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.OK_200;

public class NpmRegistryMock implements PluginExpectationInitializer {
    private static final String NPM_USERNAME = "username";
    private static final String NPM_PASSWORD = "password";
    private static final String NPM_TOKEN = "NpmToken.22e3a730-9e62-11e8-98d0-529269fb1459";

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String MIME_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";

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
    }
}
