package com.jacafi.tech.shared.web;

import org.springframework.http.HttpStatus;

/**
 * The stable catalogue of error codes the API answers with.
 *
 * <p>A code exists so a client can branch on the failure without matching on prose. Text gets
 * reworded, translated and corrected; {@code VEI-002} does not. Anything a client is expected to
 * react to programmatically must have a code here.
 *
 * <p>The status lives on the code rather than on the exception, and there is no
 * {@code @ResponseStatus} anywhere: with the annotation, "what does this failure answer" is spread
 * across as many files as there are exceptions, and two exceptions meaning the same thing drift
 * apart without anyone noticing. Here the whole catalogue is one screen.
 *
 * <p>Messages are the ones the client sees, so they are in pt-BR and say what happened in business
 * terms. None of them interpolates a submitted value — that is what keeps a validation message
 * from becoming the place personal data leaks (LGPD Art. 6 VII).
 */
public enum ErrorCode {

    // Genéricos, para falhas que não pertencem a nenhuma fatia.
    INTERNAL_ERROR("GEN-001", HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Informe o identificador de rastreio."),
    MALFORMED_BODY("GEN-002", HttpStatus.BAD_REQUEST, "Corpo da requisição inválido."),
    INVALID_PARAMETER("GEN-003", HttpStatus.BAD_REQUEST, "Parâmetro da requisição inválido."),
    VALIDATION_FAILED("GEN-004", HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos."),
    DATA_CONFLICT("GEN-005", HttpStatus.CONFLICT, "A operação conflita com dados já registrados."),
    METHOD_NOT_ALLOWED("GEN-006", HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado por este recurso."),
    UNSUPPORTED_MEDIA_TYPE("GEN-007", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato de conteúdo não suportado."),
    RESOURCE_NOT_FOUND("GEN-008", HttpStatus.NOT_FOUND, "Recurso não encontrado."),

    // Autenticacao e autorizacao. Respondidos pelo filtro de seguranca, nao pelo advice: essas
    // falhas acontecem antes do DispatcherServlet e nunca chegam a um @ExceptionHandler.
    //
    // A mensagem de 401 nao diz se o usuario existe, se a senha esta errada ou se o token expirou.
    // Distinguir os casos entrega um oraculo de enumeracao: quem sonda usuarios aprende quais
    // existem pela diferenca de resposta.
    AUTHENTICATION_REQUIRED("SEG-001", HttpStatus.UNAUTHORIZED, "Autenticação necessária."),
    ACCESS_DENIED("SEG-002", HttpStatus.FORBIDDEN, "Acesso negado para esta operação."),

    // Paginação e ordenação, compartilhadas por toda listagem.
    INVALID_PAGING("PAG-001", HttpStatus.BAD_REQUEST, "Parâmetros de paginação ou ordenação inválidos."),

    // Fatia de veículos.
    VEHICLE_NOT_FOUND("VEI-001", HttpStatus.NOT_FOUND, "Veículo não encontrado."),
    DUPLICATE_LICENSE_PLATE("VEI-002", HttpStatus.CONFLICT, "Placa já cadastrada para outro veículo ativo."),
    // 400 e nao 422, apesar de a especificacao da tarefa sugerir 422 para excecao de dominio.
    // O motivo e consistencia observavel: o mesmo campo ja responde 400 quando chega vazio, pela
    // bean validation. Um cliente que precise tratar 400 para "faltando" e 422 para "formato
    // errado" no mesmo campo trata duas vezes a mesma coisa. O 422 se justifica quando a
    // requisicao e compreendida e valida isoladamente mas conflita com o estado — e esse caso ja
    // e 409 aqui.
    INVALID_LICENSE_PLATE("VEI-003", HttpStatus.BAD_REQUEST, "Placa inválida: use o formato ABC1234 ou ABC1D23."),
    VEHICLE_QUERY_AMBIGUOUS(
            "VEI-004", HttpStatus.BAD_REQUEST, "Informe exatamente um entre placa e identificador do cliente."),

    // Fatia de clientes.
    CUSTOMER_NOT_FOUND("CLI-001", HttpStatus.NOT_FOUND, "Cliente não encontrado."),
    CUSTOMER_ALREADY_EXISTS("CLI-002", HttpStatus.CONFLICT, "Já existe um cliente com este CPF ou CNPJ."),
    /** 400 pelo mesmo motivo de VEI-003. */
    INVALID_TAX_ID("CLI-003", HttpStatus.BAD_REQUEST, "CPF ou CNPJ inválido.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    /** The pt-BR text sent to the client. Never contains a submitted value. */
    public String message() {
        return message;
    }
}
