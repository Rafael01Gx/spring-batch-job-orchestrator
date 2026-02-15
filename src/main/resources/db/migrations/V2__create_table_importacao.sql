CREATE TABLE importacao (
    id BIGSERIAL PRIMARY KEY,
    cpf VARCHAR(14) NOT NULL,
    cliente VARCHAR(150) NOT NULL,
    nascimento DATE NOT NULL,
    evento VARCHAR(150) NOT NULL,
    data DATE NOT NULL,
    tipo_ingresso VARCHAR(100) NOT NULL,
    valor NUMERIC(10,2) NOT NULL,
    hora_importacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    taxa_admin NUMERIC(10,2) NOT NULL DEFAULT 0
);
