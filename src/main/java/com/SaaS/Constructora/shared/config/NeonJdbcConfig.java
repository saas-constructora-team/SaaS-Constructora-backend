package com.SaaS.Constructora.shared.config;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayConnectionDetails;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Conecta la aplicación a Neon Postgres a partir de las variables que el CLI de
 * Neon escribe en {@code .env.local} ({@code neon init} / {@code neon env pull}):
 *
 * <ul>
 * <li>{@code DATABASE_URL}: conexión agrupada (host con sufijo {@code -pooler}).
 * La usa el datasource principal, que es el tráfico normal de la aplicación;
 * PgBouncer multiplexa esas conexiones en modo transacción.</li>
 * <li>{@code DATABASE_URL_UNPOOLED}: conexión directa, sin pooler. La usa Flyway,
 * porque las migraciones ejecutan operaciones a nivel de sesión que PgBouncer en
 * modo transacción no soporta.</li>
 * </ul>
 *
 * <p>Las cadenas de Neon llegan en formato {@code postgresql://usuario:password@host/bd?sslmode=require&channel_binding=require};
 * pgjdbc necesita {@code jdbc:postgresql://...} con usuario y password aparte y el
 * parámetro escrito {@code channelBinding}, así que esta clase hace la conversión.</p>
 *
 * <p>Si esas variables no existen (desarrollo local sin Neon), no se registra nada y
 * mandan las propiedades {@code spring.datasource.*} de {@code application.properties}.
 * Si el datasource principal lo aporta una fuente externa (Testcontainers con
 * {@code @ServiceConnection} en los tests de integración), Flyway reutiliza esa misma
 * conexión para no desviar las migraciones fuera del contenedor de pruebas.</p>
 */
@Configuration(proxyBeanMethods = false)
public class NeonJdbcConfig {

    private static final String POOLED_URL_PROPERTY = "DATABASE_URL";
    private static final String DIRECT_URL_PROPERTY = "DATABASE_URL_UNPOOLED";

    /**
     * Detalles JDBC del datasource principal: la conexión agrupada de Neon.
     * Sustituye a las propiedades {@code spring.datasource.*} y cede ante
     * cualquier otro {@link JdbcConnectionDetails} ya definido.
     */
    @Bean
    @ConditionalOnProperty(POOLED_URL_PROPERTY)
    @ConditionalOnMissingBean(JdbcConnectionDetails.class)
    NeonConnectionDetails neonJdbcConnectionDetails(Environment environment) {
        return NeonConnectionDetails.from(environment.getRequiredProperty(POOLED_URL_PROPERTY));
    }

    /**
     * Detalles JDBC de Flyway: la conexión directa de Neon (sin pooler).
     */
    @Bean
    @ConditionalOnProperty(DIRECT_URL_PROPERTY)
    @ConditionalOnMissingBean(FlywayConnectionDetails.class)
    NeonFlywayConnectionDetails neonFlywayConnectionDetails(Environment environment,
            ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetails) {
        return new NeonFlywayConnectionDetails(environment.getRequiredProperty(DIRECT_URL_PROPERTY),
                jdbcConnectionDetails);
    }

    /**
     * Detalles JDBC de la conexión agrupada ({@code DATABASE_URL}) ya convertidos
     * al formato que espera pgjdbc.
     */
    record NeonConnectionDetails(String jdbcUrl, String username, String password) implements JdbcConnectionDetails {

        static NeonConnectionDetails from(String postgresUrl) {
            PostgresUrl url = PostgresUrl.parse(postgresUrl);
            return new NeonConnectionDetails(url.jdbcUrl(), url.username(), url.password());
        }

        @Override
        public String getJdbcUrl() {
            return jdbcUrl;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getPassword() {
            return password;
        }
    }

    /**
     * Detalles JDBC de Flyway sobre la conexión directa ({@code DATABASE_URL_UNPOOLED}).
     *
     * <p>Si el datasource principal quedó en manos de una fuente externa (Testcontainers
     * con {@code @ServiceConnection} en los tests de integración), Flyway reutiliza esa
     * misma conexión; en cualquier otro caso usa la conexión directa de Neon.</p>
     */
    static final class NeonFlywayConnectionDetails implements FlywayConnectionDetails {

        private final PostgresUrl url;
        private final ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetails;

        NeonFlywayConnectionDetails(String postgresUrl, ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetails) {
            this.url = PostgresUrl.parse(postgresUrl);
            this.jdbcConnectionDetails = jdbcConnectionDetails;
        }

        /** Detalles del datasource principal definidos fuera de Neon, si los hay. */
        private JdbcConnectionDetails externalConnectionDetails() {
            return this.jdbcConnectionDetails.stream()
                .filter(details -> !(details instanceof NeonConnectionDetails))
                .findFirst()
                .orElse(null);
        }

        @Override
        public String getJdbcUrl() {
            JdbcConnectionDetails external = externalConnectionDetails();
            return (external != null) ? external.getJdbcUrl() : this.url.jdbcUrl();
        }

        @Override
        public String getUsername() {
            JdbcConnectionDetails external = externalConnectionDetails();
            return (external != null) ? external.getUsername() : this.url.username();
        }

        @Override
        public String getPassword() {
            JdbcConnectionDetails external = externalConnectionDetails();
            return (external != null) ? external.getPassword() : this.url.password();
        }
    }

    /**
     * Cadena de conexión de Neon ({@code postgresql://...}) convertida al formato de
     * pgjdbc: {@code jdbc:postgresql://host/bd?...} con usuario y password separados.
     * Renombra el parámetro {@code channel_binding} a {@code channelBinding}, que es
     * el nombre que entiende pgjdbc.
     */
    private record PostgresUrl(String jdbcUrl, String username, String password) {

        private static PostgresUrl parse(String postgresUrl) {
            URI uri = URI.create(unquote(postgresUrl.trim()));
            if (!"postgresql".equals(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalStateException(
                        "La variable de conexión de Neon no tiene el formato esperado (postgresql://usuario:password@host/bd)");
            }
            StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
            if (uri.getPort() != -1) {
                jdbcUrl.append(':').append(uri.getPort());
            }
            String path = uri.getPath();
            jdbcUrl.append((path == null || path.isBlank()) ? "/" : path);
            if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
                jdbcUrl.append('?').append(Arrays.stream(uri.getRawQuery().split("&"))
                    .map(NeonJdbcConfig::toJdbcQueryParameter)
                    .collect(Collectors.joining("&")));
            }
            String username = null;
            String password = null;
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                int separator = userInfo.indexOf(':');
                username = (separator >= 0) ? userInfo.substring(0, separator) : userInfo;
                password = (separator >= 0) ? userInfo.substring(separator + 1) : null;
            }
            return new PostgresUrl(jdbcUrl.toString(), username, password);
        }
    }

    private static String toJdbcQueryParameter(String parameter) {
        return parameter.startsWith("channel_binding=")
                ? "channelBinding=" + parameter.substring("channel_binding=".length())
                : parameter;
    }

    /**
     * Quita las comillas que el CLI de Neon pone alrededor de los valores al
     * escribirlos en el archivo .env: Spring los carga como .properties, donde
     * las comillas forman parte del valor.
     */
    private static String unquote(String value) {
        if (value.length() >= 2 && value.charAt(0) == value.charAt(value.length() - 1)
                && (value.charAt(0) == '"' || value.charAt(0) == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
