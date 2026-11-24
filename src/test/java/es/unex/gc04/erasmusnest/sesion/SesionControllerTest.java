package es.unex.gc04.erasmusnest.sesion;

import static org.assertj.core.api.Assertions.assertThat;

import es.unex.gc04.erasmusnest.usuario.Usuario;
import es.unex.gc04.erasmusnest.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;

/** Pantallas de inicio y cierre de sesión (TK-012). Cuentas sintéticas (NFR-009). */
@SpringBootTest
class SesionControllerTest {

    private static final String CORREO = "ana.garcia@alumnos.unex.es";

    @Autowired private SesionController controller;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private IntentoAccesoRepository intentos;
    @Autowired private PasswordEncoder encoder;

    @BeforeEach
    void preparar() {
        intentos.deleteAll();
        usuarios.deleteAll();
        usuarios.save(new Usuario(CORREO, encoder.encode("Erasmus2026")));
    }

    @Test
    void conCredencialesCorrectasVaAlPanel() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String vista = controller.iniciarSesion(CORREO, "Erasmus2026", request, new ExtendedModelMap());

        assertThat(vista).isEqualTo("redirect:/panel");
        assertThat(SesionUsuario.usuarioActual(request)).isPresent();
    }

    @Test
    void conCredencialesIncorrectasMuestraElMensajeGenericoYConservaElCorreo() {
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.iniciarSesion(CORREO, "ClaveMala1", new MockHttpServletRequest(), model);

        assertThat(vista).isEqualTo("login");
        assertThat(model)
            .containsEntry("error", AccesoService.ERROR_GENERICO)
            .containsEntry("email", CORREO);
    }

    @Test
    void elPanelSinSesionLlevaAlLoginYNoSeGuardaEnCache() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String vista = controller.panel(new MockHttpServletRequest(), response, new ExtendedModelMap());

        assertThat(vista).isEqualTo("redirect:/login");
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
    }

    @Test
    void cerrarSesionInvalidaLaSesion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        controller.iniciarSesion(CORREO, "Erasmus2026", request, new ExtendedModelMap());

        assertThat(controller.cerrarSesion(request)).isEqualTo("redirect:/login?salida");
        assertThat(SesionUsuario.usuarioActual(request)).isEmpty();
    }
}
