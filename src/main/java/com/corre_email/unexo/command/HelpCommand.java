package com.corre_email.unexo.command;

import com.corre_email.unexo.validation.DefinicionComando;
import com.corre_email.unexo.validation.ParamDef;
import com.corre_email.unexo.validation.Parametros;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** La ayuda se arma sola con las definiciones de los comandos, asi nunca queda desactualizada. */
@Component
public class HelpCommand implements CommandStrategy {

    private static final DefinicionComando DEFINICION = DefinicionComando
            .de("HELP", "Muestra esta ayuda.")
            .alias("AYUDA");

    private final Supplier<List<CommandStrategy>> comandos;

    // ObjectProvider y no List: HelpCommand tambien es un CommandStrategy y
    // pedir la lista en el constructor seria una dependencia circular
    @Autowired
    public HelpCommand(ObjectProvider<CommandStrategy> comandos) {
        this(() -> comandos.orderedStream().toList());
    }

    public HelpCommand(Supplier<List<CommandStrategy>> comandos) {
        this.comandos = comandos;
    }

    @Override
    public DefinicionComando definicion() {
        return DEFINICION;
    }

    @Override
    public String execute(Parametros params) {
        StringBuilder sb = new StringBuilder();
        sb.append("SISTEMA UNEXO - COMANDOS DISPONIBLES\n");
        sb.append("====================================\n\n");
        sb.append("Escriba el comando en el ASUNTO del correo.\n");
        sb.append("Cada parametro va entre comillas dobles y separado por comas.\n\n");

        // HELP primero y despues agrupados por tabla: los nombres son operacion + tabla
        // (LISPRG, INSPRG...), asi que se ordena primero por la tabla (lo que va despues de las 3 letras)
        List<DefinicionComando> definiciones = comandos.get().stream()
                .map(CommandStrategy::definicion)
                .sorted(Comparator.comparing((DefinicionComando d) -> d != DEFINICION)
                        .thenComparing(d -> d.nombre().length() > 3 ? d.nombre().substring(3) : "")
                        .thenComparing(DefinicionComando::nombre))
                .toList();

        for (DefinicionComando d : definiciones) {
            sb.append(d.formato());
            if (!d.alias().isEmpty()) {
                sb.append("  (tambien: ").append(String.join(", ", d.alias())).append(')');
            }
            sb.append('\n');
            sb.append("    ").append(d.descripcion()).append('\n');

            String opcionales = d.parametros().stream()
                    .filter(p -> !p.obligatorio() && !p.aceptaComodin())
                    .map(ParamDef::nombre)
                    .collect(Collectors.joining(", "));
            if (!opcionales.isEmpty()) {
                sb.append("    Opcionales (pueden ir vacios \"\"): ").append(opcionales).append('\n');
            }
            if (d.ejemplo() != null && !d.ejemplo().equals(d.formato())) {
                sb.append("    Ejemplo:\n    ").append(d.ejemplo()).append('\n');
            }
            sb.append('\n');
        }

        sb.append("NOTA: solo se puede responder a direcciones @tecnoweb.org.bo.\n");
        return sb.toString();
    }
}
