/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.remoting1.retrofit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.google.common.base.Optional;
import com.palantir.remoting1.tracing.OpenSpan;
import com.palantir.remoting1.tracing.TraceHttpHeaders;
import com.palantir.remoting1.tracing.Tracer;
import javax.net.ssl.SSLSocketFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit.http.GET;

public final class TracerTest {

    @Rule
    public final MockWebServer server = new MockWebServer();

    private TestService service;

    @Before
    public void before() {
        String uri = "http://localhost:" + server.getPort();
        service = RetrofitClientFactory.createProxy(
                Optional.<SSLSocketFactory>absent(),
                uri,
                TestService.class,
                OkHttpClientOptions.builder().build());

        server.enqueue(new MockResponse().setBody("{}"));
    }

    @Test
    public void testClientIsInstrumentedWithTracer() throws InterruptedException {
        OpenSpan parentTrace = Tracer.startSpan("");
        String traceId = Tracer.getTraceId();
        service.get();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader(TraceHttpHeaders.TRACE_ID), is(traceId));
        assertThat(request.getHeader(TraceHttpHeaders.SPAN_ID), is(not(parentTrace.getSpanId())));
    }

    public interface TestService {
        @GET("/")
        Object get();
    }
}
