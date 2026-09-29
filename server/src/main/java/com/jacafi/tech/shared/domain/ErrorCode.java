package com.jacafi.tech.shared.domain;

public enum ErrorCode {
    INTERNAL_ERROR("GEN-001", "Erro interno. Informe o identificador de rastreio."),
    MALFORMED_BODY("GEN-002", "Corpo da requisição inválido."),
    INVALID_PARAMETER("GEN-003", "Parâmetro da requisição inválido."),
    VALIDATION_FAILED("GEN-004", "Um ou mais campos estão inválidos."),
    DATA_CONFLICT("GEN-005", "A operação conflita com dados já registrados."),
    METHOD_NOT_ALLOWED("GEN-006", "Método não suportado por este recurso."),
    UNSUPPORTED_MEDIA_TYPE("GEN-007", "Formato de conteúdo não suportado."),
    RESOURCE_NOT_FOUND("GEN-008", "Recurso não encontrado."),

    AUTHENTICATION_REQUIRED("SEG-001", "Autenticação necessária."),
    ACCESS_DENIED("SEG-002", "Acesso negado para esta operação."),

    INVALID_PAGING("PAG-001", "Parâmetros de paginação ou ordenação inválidos."),

    VEHICLE_NOT_FOUND("VEI-001", "Veículo não encontrado."),
    DUPLICATE_LICENSE_PLATE("VEI-002", "Placa já cadastrada para outro veículo ativo."),

    INVALID_LICENSE_PLATE("VEI-003", "Placa inválida: use o formato ABC1234 ou ABC1D23."),
    VEHICLE_QUERY_AMBIGUOUS("VEI-004", "Informe exatamente um entre placa e identificador do cliente."),

    CUSTOMER_NOT_FOUND("CLI-001", "Cliente não encontrado."),
    CUSTOMER_ALREADY_EXISTS("CLI-002", "Já existe um cliente com este CPF ou CNPJ."),

    INVALID_TAX_ID("CLI-003", "CPF ou CNPJ inválido."),

    INVENTORY_ITEM_NOT_FOUND("INV-001", "Item de estoque não encontrado."),
    RESERVATION_NOT_FOUND("INV-002", "Reserva de estoque não encontrada."),
    DUPLICATE_MATERIAL("INV-003", "Material já cadastrado."),
    INSUFFICIENT_STOCK("INV-004", "Estoque insuficiente."),

    SERVICE_CATALOG_ITEM_NOT_FOUND("CAT-001", "Item de catálogo não encontrado."),
    DUPLICATE_SERVICE_CATALOG_ITEM("CAT-002", "Já existe um item ativo com este nome."),

    SERVICE_ORDER_NOT_FOUND("OS-001", "Ordem de serviço não encontrada.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
