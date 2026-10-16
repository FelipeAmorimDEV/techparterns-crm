package tech.techparterns.shared.domain.exception;

public enum ErroDominio {

    // ── Compartilhado ────────────────────────────────────────────────────────
    FAZENDA_OBRIGATORIA("Fazenda é obrigatória"),
    VARIEDADE_ID_OBRIGATORIO("Variedade ID é obrigatório"),

    // ── Produto ──────────────────────────────────────────────────────────────
    NOME_PRODUTO_OBRIGATORIO("Nome do produto é obrigatório"),
    UNIDADE_OBRIGATORIA("Unidade de medida é obrigatória"),

    // ── Quantidade ───────────────────────────────────────────────────────────
    QUANTIDADE_INVALIDA("Quantidade não pode ser negativa"),
    QUANTIDADE_ENTRADA_INVALIDA("Quantidade de entrada deve ser positiva"),
    QUANTIDADE_SAIDA_INVALIDA("Quantidade de saída deve ser positiva"),

    // ── Preço ────────────────────────────────────────────────────────────────
    PRECO_INVALIDO("Preço não pode ser negativo"),

    // ── Carência ─────────────────────────────────────────────────────────────
    CARENCIA_NEGATIVA("Carência não pode ser negativa"),

    // ── Estoque ──────────────────────────────────────────────────────────────
    ESTOQUE_INSUFICIENTE("Estoque insuficiente"),
    PRODUTO_NAO_ENCONTRADO("Produto não encontrado: %s"),
    ESTOQUE_INICIAL_JA_CONCLUIDO("Estoque inicial já foi concluído nesta cultura"),
    ESTOQUE_INICIAL_ITENS_OBRIGATORIOS("Informe ao menos um produto no estoque inicial"),
    ESTOQUE_INICIAL_VALOR_INVALIDO("Valor unitário do estoque inicial deve ser maior que zero"),

    // ── Nota Fiscal ──────────────────────────────────────────────────────────
    NOTA_FISCAL_FORNECEDOR_OBRIGATORIO("Fornecedor é obrigatório"),
    NOTA_FISCAL_DATA_OBRIGATORIA("Data da nota é obrigatória"),
    NOTA_FISCAL_VENCIMENTO_OBRIGATORIO("Data de vencimento da nota é obrigatória"),
    NOTA_FISCAL_VENCIMENTO_NO_PASSADO("Data de vencimento não pode ser anterior a hoje"),
    NOTA_FISCAL_HOJE_OBRIGATORIO("Data de referência (hoje) é obrigatória"),
    NOTA_FISCAL_USUARIO_OBRIGATORIO("Usuário é obrigatório"),
    NOTA_FISCAL_ITENS_OBRIGATORIOS("Nota fiscal deve conter pelo menos um item"),
    NOTA_FISCAL_SEM_ITENS("Nota fiscal deve possuir ao menos um produto."),
    NOTA_FISCAL_NAO_ENCONTRADA("Nota fiscal não encontrada: %s"),
    NOTA_FISCAL_PARCELAS_OBRIGATORIAS("Nota fiscal deve conter pelo menos uma parcela"),
    NOTA_FISCAL_PARCELAS_NUMERACAO_INVALIDA("Parcelas devem ser numeradas de 1 a N sem buracos"),
    NOTA_FISCAL_PARCELAS_SOMA_DIVERGENTE("Soma das parcelas deve ser igual ao total dos itens"),
    PARCELA_VALOR_INVALIDO("Valor da parcela deve ser positivo com no máximo 2 casas decimais"),
    PARCELA_JA_PAGA("Parcela já está paga"),
    PARCELA_NAO_ENCONTRADA("Parcela não encontrada"),

    // ── Item de Nota Fiscal ──────────────────────────────────────────────────
    ITEM_NOTA_FISCAL_PRODUTO_OBRIGATORIO("Produto do item é obrigatório"),
    ITEM_NOTA_FISCAL_QUANTIDADE_INVALIDA("Quantidade do item deve ser positiva"),
    ITEM_NOTA_FISCAL_VALOR_OBRIGATORIO("Valor do item é obrigatório"),

    // ── Fornecedor ───────────────────────────────────────────────────────────
    NOME_FORNECEDOR_OBRIGATORIO("Nome do fornecedor é obrigatório"),
    NOME_CONTATO_FORNECEDOR_OBRIGATORIO("Nome do contato do fornecedor é obrigatório"),
    TELEFONE_FORNECEDOR_OBRIGATORIO("Telefone do fornecedor é obrigatório"),
    FORNECEDOR_NAO_ENCONTRADO("Fornecedor não encontrado: %s"),
    FORNECEDOR_JA_ATIVO("Fornecedor já está ativo: %s"),
    CNPJ_FORNECEDOR_OBRIGATORIO("CNPJ do fornecedor é obrigatório"),
    CNPJ_FORNECEDOR_INVALIDO("CNPJ do fornecedor é inválido"),

    // ── Tipo ─────────────────────────────────────────────────────────────────
    NOME_TIPO_OBRIGATORIO("Nome do tipo é obrigatório"),
    TIPO_NAO_ENCONTRADO("Tipo não encontrado: %s"),

    // ── Fazenda ──────────────────────────────────────────────────────────────
    PRODUTO_DE_OUTRA_FAZENDA("Produto não pertence à fazenda da nota fiscal: %s"),

    // ── Aplicação fitossanitária ──────────────────────────────────────────────
    APLICACAO_APLICADOR_OBRIGATORIO("Aplicador é obrigatório"),
    APLICACAO_CICLO_OBRIGATORIO("Ciclo produtivo da aplicação é obrigatório"),
    APLICACAO_VOLUME_CALDA_INVALIDO("Volume de calda deve ser positivo"),
    APLICACAO_ITENS_OBRIGATORIOS("Aplicação deve conter pelo menos um produto"),
    APLICACAO_NAO_ENCONTRADA("Aplicação não encontrada: %s"),
    ITEM_APLICACAO_PRODUTO_OBRIGATORIO("Produto do item de aplicação é obrigatório"),
    ITEM_APLICACAO_DOSAGEM_INVALIDA("Dosagem do item de aplicação deve ser positiva"),
    ITEM_APLICACAO_QUANTIDADE_INVALIDA("Quantidade do item de aplicação deve ser positiva"),
    LANCAMENTO_JA_FINALIZADO("Lançamento já finalizado"),
    LANCAMENTO_CANCELADO("Lançamento cancelado"),
    LANCAMENTO_USUARIO_OBRIGATORIO("Usuário da confirmação é obrigatório"),

    // ── Fertirrigação / Irrigação ─────────────────────────────────────────────
    FERTIRRIGACAO_SEMANA_OBRIGATORIA("Semana da fertirrigação é obrigatória"),
    FERTIRRIGACAO_APLICADOR_OBRIGATORIO("Aplicador da fertirrigação é obrigatório"),
    FERTIRRIGACAO_SETOR_OBRIGATORIO("Setor da fertirrigação é obrigatório"),
    FERTIRRIGACAO_CICLO_OBRIGATORIO("Ciclo produtivo da fertirrigação é obrigatório"),
    FERTIRRIGACAO_ITENS_OBRIGATORIOS("Fertirrigação deve conter pelo menos um produto"),
    FERTIRRIGACAO_NAO_ENCONTRADA("Fertirrigação não encontrada: %s"),
    IRRIGACAO_NAO_ENCONTRADA("Irrigação não encontrada: %s"),
    FERTIRRIGACAO_TIPO_OBRIGATORIO("Tipo de irrigação é obrigatório"),
    FERTIRRIGACAO_INICIO_OBRIGATORIO("Data/hora de início da irrigação é obrigatória"),
    FERTIRRIGACAO_FIM_OBRIGATORIO("Data/hora de fim da irrigação é obrigatória"),
    FERTIRRIGACAO_PERIODO_INVALIDO("Fim da irrigação deve ser posterior ao início"),
    FERTIRRIGACAO_VAZAO_SETOR_INVALIDA("Vazão do setor deve ser positiva"),
    FERTIRRIGACAO_SOMENTE_AGUA_SEM_PRODUTOS("Irrigação somente com água não deve conter produtos"),
    FERTIRRIGACAO_SETOR_SEM_HIDRAULICA("Setor não possui configuração hidráulica; cadastre a vazão antes de registrar irrigação"),
    ITEM_FERTIRRIGACAO_PRODUTO_OBRIGATORIO("Produto do item de fertirrigação é obrigatório"),
    ITEM_FERTIRRIGACAO_QUANTIDADE_INVALIDA("Quantidade do item de fertirrigação deve ser positiva"),

    // ── Produção — Cultura / Variedade / Setor ────────────────────────────────
    CULTURA_OBRIGATORIA("Cultura é obrigatória"),
    CULTURA_NAO_ENCONTRADA("Cultura não encontrada: %s"),
    CULTURA_NAO_HABILITADA("Cultura não está habilitada na fazenda"),
    CULTURA_PRINCIPAL_OBRIGATORIA("Cultura principal é obrigatória"),
    CULTURA_PRINCIPAL_NAO_LISTADA("Cultura principal deve estar entre as culturas informadas"),
    NOME_VARIEDADE_OBRIGATORIO("Nome da variedade é obrigatório"),
    VARIEDADE_OBRIGATORIA("Variedade é obrigatória"),
    VARIEDADE_NAO_ENCONTRADA("Variedade não encontrada: %s"),
    RECEITAS_ATIVIDADES_ORIGEM_IGUAL_DESTINO("Variedade de origem e destino devem ser diferentes"),
    RECEITAS_ATIVIDADES_ORIGEM_SEM_ITENS("Variedade de origem não tem receita de atividades para clonar"),
    FASES_FENOLOGICAS_ORIGEM_IGUAL_DESTINO("Variedade de origem e destino devem ser diferentes"),
    FASES_FENOLOGICAS_ORIGEM_SEM_FASES("Variedade de origem não tem fases fenológicas para clonar"),
    NOME_SETOR_OBRIGATORIO("Nome do setor é obrigatório"),
    TAMANHO_SETOR_INVALIDO("Tamanho do setor deve ser positivo"),
    SETOR_NAO_ENCONTRADO("Setor não encontrado: %s"),
    SETOR_COM_CICLO_ATIVO_NAO_PODE_TROCAR_VARIEDADE(
            "Não é possível trocar a variedade do setor enquanto houver ciclo planejado, em manejo ou em colheita"),
    HIDRAULICA_VAZAO_GOTEJADOR_INVALIDA("Vazão do gotejador deve ser positiva"),
    HIDRAULICA_GOTEJADORES_POR_PLANTA_INVALIDO("Número de gotejadores por planta deve ser positivo"),
    HIDRAULICA_NUM_PLANTAS_INVALIDO("Número de plantas do setor deve ser positivo"),
    HIDRAULICA_VAZAO_MEDIDA_INVALIDA("Vazão medida do setor deve ser positiva quando informada"),
    CICLO_PRODUTIVO_ID_OBRIGATORIO("Ciclo produtivo ID é obrigatório"),

    // ── Identidade ───────────────────────────────────────────────────────────
    NOME_FAZENDA_OBRIGATORIO("Nome da fazenda é obrigatório"),
    EMAIL_OBRIGATORIO("E-mail é obrigatório"),
    EMAIL_INVALIDO("E-mail inválido"),
    EMAIL_JA_CADASTRADO("E-mail já cadastrado: %s"),
    SENHA_OBRIGATORIA("Senha é obrigatória"),
    SENHA_CURTA("Senha deve ter pelo menos 8 caracteres"),
    ROLE_OBRIGATORIA("Role é obrigatória"),
    ROLE_INVALIDA("Role inválida"),
    USUARIO_NAO_ENCONTRADO("Usuário não encontrado: %s"),
    ULTIMO_ADMIN("Este é o último administrador da fazenda; não é possível remover ou rebaixar"),
    TIPO_ENCARGO_INVALIDO("Tipo do encargo sugerido inválido: %s"),
    POLITICA_DIARIA_META_INVALIDA("Política de diária por meta inválida: %s"),
    DIA_TRABALHO_INVALIDO("Dia de trabalho inválido: %s"),

    // ── Equipe ───────────────────────────────────────────────────────────────
    FUNCIONARIO_NAO_ENCONTRADO("Funcionário não encontrado: %s");

    private final String mensagem;

    ErroDominio(String mensagem) {
        this.mensagem = mensagem;
    }

    public String mensagem() {
        return mensagem;
    }

    public String mensagem(Object... args) {
        return String.format(mensagem, args);
    }

    @Override
    public String toString() {
        return mensagem;
    }
}
