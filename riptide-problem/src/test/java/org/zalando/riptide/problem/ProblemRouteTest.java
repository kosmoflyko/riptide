package org.zalando.riptide.problem;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.problem.ThrowableProblem;
import org.zalando.riptide.Rest;

import java.net.URI;
import java.util.concurrent.CompletionException;

import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Route.pass;
import static org.zalando.riptide.problem.ProblemRoute.problemHandling;

public final class ProblemRouteTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Rest unit;
    private final MockRestServiceServer server;

    public ProblemRouteTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getRest();
        this.server = setup.getServer();
    }

    @Test
    public void shouldPropagateProblem() {
        perform("application/problem+json");
    }

    @Test
    public void shouldPropagateLegacyProblem() {
        perform("application/x.problem+json");
    }

    @Test
    public void shouldPropagateLegacyProblemWithAlternativeSpelling() {
        perform("application/x-problem+json");
    }

    private void perform(final String mediaType) {
        server.expect(requestTo(url))
                .andRespond(withStatus(BAD_REQUEST)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(MediaType.parseMediaType(mediaType)));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(ThrowableProblem.class));

        unit.get(url)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().call(problemHandling()))
                .join();
    }

}