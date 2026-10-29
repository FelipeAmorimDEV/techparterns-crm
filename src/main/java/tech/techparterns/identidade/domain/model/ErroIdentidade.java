package tech.techparterns.identidade.domain.model;

/** Mensagens e códigos de erro do contexto de identidade e acesso (em português). */
public enum ErroIdentidade {

    EMAIL_OBRIGATORIO("E-mail é obrigatório"),
    EMAIL_INVALIDO("E-mail inválido"),
    EMAIL_MUITO_LONGO("E-mail deve ter no máximo 255 caracteres"),
    NOME_OBRIGATORIO("Nome é obrigatório"),
    NOME_MUITO_LONGO("Nome deve ter no máximo 120 caracteres"),
    TELEFONE_INVALIDO("Telefone inválido: use o formato internacional, por exemplo +5511999998888"),
    EMPRESA_NOME_OBRIGATORIO("Nome da empresa é obrigatório"),
    EMPRESA_NOME_MUITO_LONGO("Nome da empresa deve ter no máximo 120 caracteres"),
    MOTIVO_MUITO_LONGO("Motivo da suspensão deve ter no máximo 255 caracteres"),
    SENHA_OBRIGATORIA("Senha é obrigatória"),
    SENHA_CURTA("Senha deve ter no mínimo 8 caracteres"),
    SENHA_SEM_LETRA("Senha deve conter ao menos uma letra"),
    SENHA_SEM_NUMERO("Senha deve conter ao menos um número"),
    SENHA_MUITO_LONGA("Senha deve ter no máximo 72 bytes"),
    HASH_TOKEN_INVALIDO("Hash de token inválido"),
    PAPEL_INVALIDO("Papel inválido: use ADMIN ou SELLER"),
    STATUS_INVALIDO("Status inválido"),
    TOKEN_SENHA_INVALIDO("Link inválido, expirado ou já utilizado"),
    SENHA_ATUAL_INCORRETA("Senha atual incorreta"),
    EMAIL_PROPRIO_NAO_ALTERAVEL("O próprio e-mail não pode ser alterado"),
    EMAIL_INDISPONIVEL("E-mail indisponível"),
    ULTIMO_ADMIN("A empresa não pode ficar sem um administrador ativo"),
    USUARIO_NAO_PENDENTE("Operação permitida apenas para usuário pendente"),
    USUARIO_NAO_INATIVO("Operação permitida apenas para usuário inativo"),
    USUARIO_NAO_ENCONTRADO("Usuário não encontrado"),
    EMPRESA_NAO_ENCONTRADA("Empresa não encontrada"),
    CONVITE_NAO_ENCONTRADO("Convite não encontrado"),
    CONVITE_INDISPONIVEL("Convite inválido ou indisponível. Solicite um novo link ao administrador."),
    CREDENCIAIS_INVALIDAS("E-mail ou senha inválidos, ou acesso indisponível."),
    PAGINACAO_INVALIDA("Paginação inválida: page deve ser >= 0 e size entre 1 e 100"),
    EXPIRACAO_INVALIDA("A validade do convite deve estar no futuro"),
    USOS_INVALIDOS("O limite de usos deve ser no mínimo 1"),
    ENVIO_MUITO_FREQUENTE("Aguarde antes de reenviar o e-mail"),
    EMPRESA_SUSPENSA("Empresa suspensa"),
    EMPRESA_COM_ADMIN_ATIVO("A empresa possui administrador ativo"),
    ESTADO_EMPRESA_INVALIDO("Operação inválida para o estado atual da empresa"),
    DONO_INEXISTENTE("Nenhum dono da plataforma cadastrado"),
    ORIGEM_NAO_PERMITIDA("Origem não permitida");

    private final String mensagem;

    ErroIdentidade(String mensagem) { this.mensagem = mensagem; }

    public String mensagem() { return mensagem; }

    @Override
    public String toString() { return mensagem; }
}
