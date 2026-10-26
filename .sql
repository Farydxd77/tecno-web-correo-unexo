-- Elimina por completo el esquema public junto con todas sus tablas, restricciones y dependencias
DROP SCHEMA public CASCADE;

-- Vuelve a crear el esquema public limpio desde cero
CREATE SCHEMA public;

-- Restaura los permisos por defecto del esquema
GRANT ALL ON SCHEMA public TO public;

-- 1. Crear la base de datos (Ejecutar esto conectado a postgres o por separado si prefieres)
-- CREATE DATABASE unexo;

-- \c unexo; -- Conectarse a la base de datos creada (si usas psql)

-- ==========================================================
-- 2. TABLAS INDEPENDIENTES (Sin dependencias externas)
-- ==========================================================

-- Tabla: programa
CREATE TABLE programa (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(50),
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    cantidad_modulos INTEGER,
    precio_contado NUMERIC(10, 2),
    estado_comercial BOOLEAN
);

-- Tabla: modulo
CREATE TABLE modulo (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(50),
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    requisitos TEXT,
    temario TEXT,
    horas INTEGER,
    precio NUMERIC(10, 2),
    se_oferta_por_separado BOOLEAN,
    estado_comercial BOOLEAN
);

-- Tabla: persona (El ID es 'ci' de tipo VARCHAR)
CREATE TABLE persona (
    ci VARCHAR(50) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    tipo VARCHAR(50),
    telefono VARCHAR(50),
    celular VARCHAR(50),
    email VARCHAR(150),
    correo_institucional VARCHAR(150),
    password VARCHAR(255),
    activo BOOLEAN
);

-- Tabla: rol
CREATE TABLE rol (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL
);


-- ==========================================================
-- 3. TABLAS CON DEPENDENCIAS DE NIVEL 1
-- ==========================================================

-- Tabla: cohorte (Depende de programa)
CREATE TABLE cohorte (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    fecha_inicio DATE,
    fecha_fin DATE,
    estado VARCHAR(50), -- PLANIFICADA, EN_CURSO o FINALIZADA
    programa_id BIGINT,
    CONSTRAINT fk_cohorte_programa FOREIGN KEY (programa_id) REFERENCES programa(id)
);

-- Tabla intermedia: rol_usuario (ManyToMany entre persona y rol)
CREATE TABLE rol_usuario (
    user_id VARCHAR(50) NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, rol_id),
    CONSTRAINT fk_rol_usuario_persona FOREIGN KEY (user_id) REFERENCES persona(ci) ON DELETE CASCADE,
    CONSTRAINT fk_rol_usuario_rol FOREIGN KEY (rol_id) REFERENCES rol(id) ON DELETE CASCADE
);

-- Tabla intermedia: programa_modulo (Relación con orden)
CREATE TABLE programa_modulo (
    id SERIAL PRIMARY KEY,
    programa_id BIGINT,
    modulo_id BIGINT,
    orden INTEGER,
    CONSTRAINT uk_programa_modulo UNIQUE (programa_id, modulo_id),
    CONSTRAINT fk_programa_modulo_programa FOREIGN KEY (programa_id) REFERENCES programa(id) ON DELETE CASCADE,
    CONSTRAINT fk_programa_modulo_modulo FOREIGN KEY (modulo_id) REFERENCES modulo(id) ON DELETE CASCADE
);


-- ==========================================================
-- 4. TABLAS CON DEPENDENCIAS DE NIVEL 2
-- ==========================================================

-- Tabla: grupo (Depende de cohorte, modulo y persona como docente)
CREATE TABLE grupo (
    id SERIAL PRIMARY KEY,
    version VARCHAR(50),
    fecha_inicio DATE,
    fecha_fin DATE,
    cupo_minimo INTEGER,
    cupo_maximo INTEGER,
    enlace_meet VARCHAR(255),
    estado VARCHAR(50), -- ABIERTO, EN_CURSO o CERRADO
    cohorte_id BIGINT,
    modulo_id BIGINT,
    docente_id VARCHAR(50), -- Apunta a persona.ci
    CONSTRAINT fk_grupo_cohorte FOREIGN KEY (cohorte_id) REFERENCES cohorte(id),
    CONSTRAINT fk_grupo_modulo FOREIGN KEY (modulo_id) REFERENCES modulo(id),
    CONSTRAINT fk_grupo_docente FOREIGN KEY (docente_id) REFERENCES persona(ci)
);


-- ==========================================================
-- 5. TABLAS CON DEPENDENCIAS FINALES
-- ==========================================================

-- Tabla: horario (Depende de grupo)
CREATE TABLE horario (
    id SERIAL PRIMARY KEY,
    dia_semana VARCHAR(50), -- LUNES, MARTES, etc.
    hora_inicio TIME,
    hora_fin TIME,
    grupo_id BIGINT,
    CONSTRAINT fk_horario_grupo FOREIGN KEY (grupo_id) REFERENCES grupo(id) ON DELETE CASCADE
);

-- Tabla: matricula (Depende de persona y grupo con restricción única)
CREATE TABLE matricula (
    id SERIAL PRIMARY KEY,
    estado VARCHAR(50), -- PENDIENTE, ACTIVA o ANULADA
    origen VARCHAR(100),
    acepto_requisitos BOOLEAN,
    nota_final NUMERIC(5, 2),
    resultado VARCHAR(50), -- APROBADO o REPROBADO
    usuario_id VARCHAR(50), -- Apunta a persona.ci
    grupo_id BIGINT,
    CONSTRAINT uk_matricula_usuario_grupo UNIQUE (usuario_id, grupo_id),
    CONSTRAINT fk_matricula_usuario FOREIGN KEY (usuario_id) REFERENCES persona(ci),
    CONSTRAINT fk_matricula_grupo FOREIGN KEY (grupo_id) REFERENCES grupo(id)
);


