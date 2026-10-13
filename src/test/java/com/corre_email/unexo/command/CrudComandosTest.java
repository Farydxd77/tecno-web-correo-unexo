package com.corre_email.unexo.command;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;
import com.corre_email.unexo.model.Grupo;
import com.corre_email.unexo.model.Persona;
import com.corre_email.unexo.model.Programa;
import com.corre_email.unexo.repository.CohorteRepository;
import com.corre_email.unexo.repository.GrupoRepository;
import com.corre_email.unexo.repository.HorarioRepository;
import com.corre_email.unexo.repository.MatriculaRepository;
import com.corre_email.unexo.repository.ModuloRepository;
import com.corre_email.unexo.repository.PersonaRepository;
import com.corre_email.unexo.repository.ProgramaModuloRepository;
import com.corre_email.unexo.repository.ProgramaRepository;
import com.corre_email.unexo.repository.RoleRepository;
import com.corre_email.unexo.service.CommandDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El CRUD de las tablas nuevas, pasando por el dispatcher como llega un correo real. */
class CrudComandosTest {

    private PersonaRepository personas;
    private ProgramaRepository programas;
    private ModuloRepository modulos;
    private ProgramaModuloRepository programaModulos;
    private CohorteRepository cohortes;
    private GrupoRepository grupos;
    private HorarioRepository horarios;
    private MatriculaRepository matriculas;
    private CommandDispatcher dispatcher;

    @BeforeEach
    void armarSistema() {
        personas = mock(PersonaRepository.class);
        programas = mock(ProgramaRepository.class);
        modulos = mock(ModuloRepository.class);
        programaModulos = mock(ProgramaModuloRepository.class);
        cohortes = mock(CohorteRepository.class);
        grupos = mock(GrupoRepository.class);
        horarios = mock(HorarioRepository.class);
        matriculas = mock(MatriculaRepository.class);

        PersonaComandos per = new PersonaComandos(personas, mock(RoleRepository.class));
        ProgramaComandos prg = new ProgramaComandos(programas);
        ModuloComandos mdl = new ModuloComandos(modulos);
        ProgramaModuloComandos pmd = new ProgramaModuloComandos(programaModulos, programas, modulos);
        CohorteComandos coh = new CohorteComandos(cohortes, programas);
        GrupoComandos gru = new GrupoComandos(grupos, cohortes, modulos, personas);
        HorarioComandos hor = new HorarioComandos(horarios, grupos);
        MatriculaComandos mat = new MatriculaComandos(matriculas, personas, grupos);

        List<CommandStrategy> comandos = new ArrayList<>();
        comandos.add(new HelpCommand(() -> comandos));
        comandos.addAll(List.of(
                per.lisper(), per.verper(), per.insper(), per.modper(), per.eliper(),
                prg.lisprg(), prg.verprg(), prg.insprg(), prg.modprg(), prg.eliprg(),
                mdl.lismdl(), mdl.vermdl(), mdl.insmdl(), mdl.modmdl(), mdl.elimdl(),
                pmd.lispmd(), pmd.verpmd(), pmd.inspmd(), pmd.modpmd(), pmd.elipmd(),
                coh.liscoh(), coh.vercoh(), coh.inscoh(), coh.modcoh(), coh.elicoh(),
                gru.lisgru(), gru.vergru(), gru.insgru(), gru.modgru(), gru.eligru(),
                hor.lishor(), hor.verhor(), hor.inshor(), hor.modhor(), hor.elihor(),
                mat.lismat(), mat.vermat(), mat.insmat(), mat.modmat(), mat.elimat()));
        dispatcher = new CommandDispatcher(comandos);
    }

    private UnexoException falla(String asunto) {
        return assertThrows(UnexoException.class, () -> dispatcher.ejecutar(asunto));
    }

    // ---------- HELP ----------

    @Test
    void helpMuestraLos40ComandosAgrupadosPorTabla() {
        String ayuda = dispatcher.ejecutar("HELP");

        for (String tabla : List.of("PER", "PRG", "MDL", "PMD", "COH", "GRU", "HOR", "MAT")) {
            for (String operacion : List.of("LIS", "VER", "INS", "MOD", "ELI")) {
                assertTrue(ayuda.contains(operacion + tabla), operacion + tabla);
            }
        }
        assertTrue(ayuda.indexOf("ELIPER") < ayuda.indexOf("ELIPRG"), "primero todo PER, despues PRG");
        assertTrue(ayuda.indexOf("VERPER") < ayuda.indexOf("ELIPRG"), "primero todo PER, despues PRG");
    }

    // ---------- Programa: el CRUD completo ----------

    @Test
    void insprgGuardaYDevuelveElId() {
        when(programas.save(any())).thenAnswer(inv -> {
            Programa p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        String respuesta = dispatcher.ejecutar(
                "INSPRG[\"DIP-JAVA\",\"Diplomado en Java\",\"\",\"4\",\"1500,50\",\"activo\"]");

        assertTrue(respuesta.startsWith("OK - Programa registrado con id 1."), respuesta);
        assertTrue(respuesta.contains("Precio al contado: 1500.50"), respuesta);
        assertTrue(respuesta.contains("Estado comercial: ACTIVO"), respuesta);
    }

    @Test
    void insprgCodigoRepetido403() {
        when(programas.existsByCodigoIgnoreCase("DIP-JAVA")).thenReturn(true);

        assertEquals(ErrorCode.DATO_UNICO_REPETIDO,
                falla("INSPRG[\"DIP-JAVA\",\"Diplomado en Java\",\"\",\"4\",\"1500\",\"ACTIVO\"]").getError());
        verify(programas, never()).save(any());
    }

    @Test
    void verprgInexistente402() {
        when(programas.findById(99L)).thenReturn(Optional.empty());

        UnexoException e = falla("VERPRG[\"99\"]");
        assertEquals(ErrorCode.REGISTRO_NO_ENCONTRADO, e.getError());
        assertTrue(e.getMessage().contains("No existe el programa 99."), e.getMessage());
    }

    @Test
    void modprgCambiaLosDatos() {
        Programa programa = new Programa("DIP-JAVA", "Viejo", null, 4, null, true);
        programa.setId(1L);
        when(programas.findById(1L)).thenReturn(Optional.of(programa));

        String respuesta = dispatcher.ejecutar("MODPRG[\"1\",\"DIP-JAVA\",\"Nuevo\",\"\",\"5\",\"1800\",\"INACTIVO\"]");

        assertTrue(respuesta.startsWith("OK - Programa modificado"), respuesta);
        assertEquals("Nuevo", programa.getNombre());
        assertEquals(5, programa.getCantidadModulos());
        assertEquals(false, programa.getEstadoComercial());
    }

    @Test
    void eliprgConCohortesQueLoUsan405() {
        when(programas.findById(1L)).thenReturn(Optional.of(new Programa()));
        doThrow(new DataIntegrityViolationException("fk_cohorte_programa")).when(programas).delete(any());

        UnexoException e = falla("ELIPRG[\"1\"]");
        assertEquals(ErrorCode.OPERACION_NO_PERMITIDA, e.getError());
        assertTrue(e.getMessage().contains("dependen de este dato"), e.getMessage());
    }

    @Test
    void lisprgVacio() {
        assertEquals("No hay registros en PROGRAMAS.", dispatcher.ejecutar("LISPRG[\"*\"]"));
    }

    // ---------- Reglas entre tablas y entre parametros ----------

    @Test
    void inscohConProgramaInexistente404() {
        when(programas.findById(7L)).thenReturn(Optional.empty());

        UnexoException e = falla("INSCOH[\"7\",\"Cohorte 2026-I\",\"2026-03-01\",\"2026-07-31\",\"PLANIFICADA\"]");
        assertEquals(ErrorCode.REFERENCIA_INVALIDA, e.getError());
        assertTrue(e.getMessage().contains("programa_id"), e.getMessage());
    }

    @Test
    void inscohFechasAlReves307() {
        UnexoException e = falla("INSCOH[\"1\",\"Cohorte 2026-I\",\"31/07/2026\",\"01/03/2026\",\"PLANIFICADA\"]");
        assertEquals(ErrorCode.VALOR_NO_PERMITIDO, e.getError());
        assertTrue(e.getMessage().contains("fecha_fin"), e.getMessage());
    }

    @Test
    void insgruCupoMinimoMayorQueMaximo307() {
        UnexoException e = falla(
                "INSGRU[\"1\",\"2\",\"12345678\",\"V1\",\"2026-03-02\",\"2026-04-10\",\"40\",\"30\",\"\",\"ABIERTO\"]");
        assertEquals(ErrorCode.VALOR_NO_PERMITIDO, e.getError());
        assertTrue(e.getMessage().contains("cupo_minimo"), e.getMessage());
    }

    @Test
    void inshorHoraFinAntesDeInicio307() {
        assertEquals(ErrorCode.VALOR_NO_PERMITIDO, falla("INSHOR[\"1\",\"LUNES\",\"21:00\",\"19:00\"]").getError());
    }

    @Test
    void insmatEnGrupoLleno405() {
        Grupo grupo = new Grupo();
        grupo.setId(1L);
        grupo.setCupoMaximo(2);
        when(personas.findById("87654321")).thenReturn(Optional.of(new Persona()));
        when(grupos.findById(1L)).thenReturn(Optional.of(grupo));
        when(matriculas.countByGrupoIdAndEstadoNot(1L, "ANULADA")).thenReturn(2L);

        UnexoException e = falla("INSMAT[\"87654321\",\"1\",\"PENDIENTE\",\"CORREO\",\"SI\"]");
        assertEquals(ErrorCode.OPERACION_NO_PERMITIDA, e.getError());
        assertTrue(e.getMessage().contains("ya esta lleno"), e.getMessage());
        verify(matriculas, never()).save(any());
    }

    @Test
    void insmatDuplicada401() {
        when(matriculas.existsByUsuarioCiAndGrupoId("87654321", 1L)).thenReturn(true);

        assertEquals(ErrorCode.REGISTRO_DUPLICADO,
                falla("INSMAT[\"87654321\",\"1\",\"PENDIENTE\",\"CORREO\",\"SI\"]").getError());
    }
}
