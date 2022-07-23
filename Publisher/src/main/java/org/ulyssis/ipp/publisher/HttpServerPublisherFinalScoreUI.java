package org.ulyssis.ipp.publisher;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Collectors;

final class HttpServerPublisherFinalScoreUI implements HttpHandler {
    final HttpServerPublisher publisher;
    final PublisherOptions options;

    public HttpServerPublisherFinalScoreUI(HttpServerPublisher publisher, PublisherOptions options) {
        this.publisher = publisher;
        this.options = options;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        if (isAuthorized(exchange)) {
            if (exchange.getRequestMethod().equalToString("GET")) {
                handleGet(exchange);
            } else if (exchange.getRequestMethod().equalToString("POST")) {
                handlePost(exchange);
            }
        } else {
            exchange.startBlocking();
            exchange.setStatusCode(401);
            exchange.getResponseHeaders().put(
                    HttpString.tryFromString("WWW-Authenticate"),
                    "Basic realm=\"Final score publishing\""
            );
            exchange.getResponseSender().send(UNAUTHORIZED_RESPONSE);
        }
    }

    void handleGet(HttpServerExchange exchange) {
        exchange.startBlocking();

        exchange.setStatusCode(200);
        if (publisher.getWithheldFinalScore() == null) {
            exchange.getResponseSender().send(AUTHORIZED_GET_RESPONSE_NO_PENDING);
        } else {
            Score score = publisher.getWithheldFinalScore();
            String tableRows = score
                    .getTeams()
                    .stream()
                    .sorted(Comparator.comparingInt(Score.Team::getLaps).reversed())
                    .map(team -> String.format("<tr><td>%s</td><td>%d</td></tr>", team.getName(), team.getLaps()))
                    .collect(Collectors.joining());
            exchange.getResponseSender().send(AUTHORIZED_GET_RESPONSE_PENDING_SCORES.replace("$rows", tableRows));
        }
    }

    void handlePost(HttpServerExchange exchange) {
        publisher.outputWithheldScore();
        exchange.startBlocking();
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(SCORES_PUBLISHED);
    }

    boolean isAuthorized(HttpServerExchange exchange) {
        String authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authorizationHeader == null) {
            return false;
        }

        String[] parts = authorizationHeader.split(" ");

        if (parts.length != 2 || !parts[0].equals("Basic")) {
            return false;
        }

        String credentials = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.US_ASCII);
        String expected = options.getFinalScoreUICredentials();

        if (expected == null || expected.length() != credentials.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < credentials.length(); i++) {
            result |= credentials.charAt(i) ^ expected.charAt(i);
        }
        return result == 0;
    }

    static final String UNAUTHORIZED_RESPONSE = "<!DOCTYPE html><html>" +
            "<head><title>401 Unauthorized</title></head>" +
            "<body>401 Unauthorized</body>" +
            "</html>";

    static final String AUTHORIZED_GET_RESPONSE_NO_PENDING = "<!DOCTYPE html><html>" +
            "<head><title>i++ Publisher</title></head>" +
            "<body>There are no final scores that await publishing.</body>" +
            "</html>";

    static final String AUTHORIZED_GET_RESPONSE_PENDING_SCORES = "<!DOCTYPE html><html>" +
            "<head><title>i++ Publisher</title></head>" +
            "<body><form method='post' onsubmit='return confirm(\"Publish scores?\");'>" +
            "<button type='submit' style='padding: 1em'><strong>Publish final scores</strong></button>" +
            "<table>$rows</table>" +
            "</form></body>" +
            "</html>";

    static final String SCORES_PUBLISHED = "<!DOCTYPE html><html>" +
            "<head><title>i++ Publisher</title></head>" +
            "<body>Scores have been published.</body>" +
            "</html>";
}
