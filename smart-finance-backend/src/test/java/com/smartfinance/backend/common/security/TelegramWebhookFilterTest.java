package com.smartfinance.backend.common.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit test for {@link TelegramWebhookFilter} driven directly with {@link MockHttpServletRequest}
 * / {@link MockHttpServletResponse}, mirroring how {@link RateLimitFilter} is exercised through
 * {@code RateLimitFilterTest} but without the {@code @WebMvcTest} overhead, since this filter's
 * behavior does not depend on any downstream controller.
 */
@ExtendWith(MockitoExtension.class)
class TelegramWebhookFilterTest {

    private static final String CONFIRM_LINK_PATH = "/api/integrations/telegram/confirm-link";
    private static final String EXPENSES_PATH = "/api/integrations/telegram/expenses";
    private static final String RECEIPTS_PATH = "/api/integrations/telegram/receipts";
    private static final String SECRET_HEADER = "X-Telegram-Webhook-Secret";

    @Mock
    private FilterChain filterChain;

    @Test
    void unrelatedPathIsNeverInspectedAndTheChainContinues() throws Exception {
        TelegramWebhookFilter filter = new TelegramWebhookFilter("configured-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/expenses");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void protectedPathWithoutHeaderIsRejectedWith401() throws Exception {
        TelegramWebhookFilter filter = new TelegramWebhookFilter("configured-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", CONFIRM_LINK_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void protectedPathWithWrongSecretIsRejectedWith401() throws Exception {
        TelegramWebhookFilter filter = new TelegramWebhookFilter("configured-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", EXPENSES_PATH);
        request.addHeader(SECRET_HEADER, "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    /**
     * Regresión directa de agregar {@code /receipts} (foto de recibo) al conjunto de rutas
     * protegidas: sin este test, un descuido al sumar la nueva ruta a {@code PROTECTED_PATHS} (sin
     * tocar {@code SecurityConfig}, que sí la declara {@code permitAll}) dejaría el endpoint
     * completamente público en runtime sin que ningún test lo detectara — ver el mismo escenario
     * documentado para {@code /expenses} en el Javadoc de
     * {@code TelegramIntegrationControllerTest#expensesWithTrailingSlashIsNeverAuthorizedByEitherMechanism}.
     */
    @Test
    void receiptsPathWithoutHeaderIsRejectedWith401() throws Exception {
        TelegramWebhookFilter filter = new TelegramWebhookFilter("configured-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", RECEIPTS_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void receiptsPathWithCorrectSecretContinuesTheChain() throws Exception {
        TelegramWebhookFilter filter = new TelegramWebhookFilter("configured-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", RECEIPTS_PATH);
        request.addHeader(SECRET_HEADER, "configured-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void protectedPathWithCorrectSecretContinuesTheChain() throws Exception {
        TelegramWebhookFilter filter = new TelegramWebhookFilter("configured-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", CONFIRM_LINK_PATH);
        request.addHeader(SECRET_HEADER, "configured-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void blankConfiguredSecretAlwaysRejectsEvenWithAMatchingBlankHeader() throws Exception {
        TelegramWebhookFilter filter = new TelegramWebhookFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", EXPENSES_PATH);
        request.addHeader(SECRET_HEADER, "");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    /**
     * Regresión de una revisión de seguridad: {@code shouldNotFilter()} compara con
     * {@code Set.contains(request.getRequestURI())} (match exacto), mientras que
     * {@code SecurityConfig} declara las mismas dos rutas con {@code PathPattern} de Spring
     * Security. Son dos mecanismos de matching independientes que podrían divergir en un cambio
     * futuro (por ejemplo, si alguien agrega un wildcard {@code /**} a uno de los dos y no al
     * otro). No se encontró una vulnerabilidad explotable hoy, pero se documenta con tests el
     * comportamiento real de cada variante de path para que cualquier divergencia futura rompa un
     * test en lugar de pasar desapercibida.
     *
     * <p>Con barra final, este filtro NO reconoce la ruta (no está en el conjunto de rutas
     * protegidas con ese valor exacto), así que {@code shouldNotFilter} devuelve {@code true} y la cadena de
     * filtros continúa sin pasar por la validación del secreto — el filtro se comporta como si la
     * ruta no fuera suya. Esto NO es un agujero de autenticación: {@code SecurityConfig} declara
     * sus dos rutas {@code permitAll} sin wildcard, y {@code PathPattern} (Spring Security 6+) no
     * matchea una barra final adicional por defecto, así que esta variante de path cae en
     * {@code anyRequest().authenticated()} y de todas formas exige JWT más adelante en la cadena.
     * Esa segunda capa se verifica end-to-end en
     * {@code TelegramIntegrationControllerTest#expensesWithTrailingSlashIsNeverAuthorizedByEitherMechanism}.
     */
    @Test
    void trailingSlashVariantIsNotRecognizedByThisFilterAndFallsThroughToTheChain() throws Exception {
        TelegramWebhookFilter filter = new TelegramWebhookFilter("configured-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", EXPENSES_PATH + "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // El filtro no actúa: no rechaza (no hay 401 propio) pero tampoco es una autorización -
        // simplemente delega en el resto de la cadena, que en el filtro chain real es
        // SecurityConfig exigiendo autenticación (ver Javadoc de este test).
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    /**
     * Misma lógica que la barra final: el match exacto de {@code Set.contains} es sensible a
     * mayúsculas/minúsculas, así que una ruta con distinto casing tampoco es reconocida por este
     * filtro. {@code PathPattern} de Spring Security también es case-sensitive por defecto, así
     * que esta variante cae igual en {@code anyRequest().authenticated()} en {@code SecurityConfig}.
     */
    @Test
    void differentCaseVariantIsNotRecognizedByThisFilterAndFallsThroughToTheChain() throws Exception {
        TelegramWebhookFilter filter = new TelegramWebhookFilter("configured-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/API/integrations/telegram/expenses");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    /**
     * {@code HttpServletRequest#getRequestURI()} nunca incluye el query string (eso vive en
     * {@code getQueryString()} por separado), así que una query string no puede usarse para
     * esquivar el match exacto de {@code Set.contains}: la ruta protegida se sigue reconociendo
     * y, sin el header del secreto, se sigue rechazando con 401.
     */
    @Test
    void queryStringDoesNotAffectPathMatchingAndTheRouteIsStillProtected() throws Exception {
        TelegramWebhookFilter filter = new TelegramWebhookFilter("configured-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", EXPENSES_PATH);
        request.setQueryString("x=1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
