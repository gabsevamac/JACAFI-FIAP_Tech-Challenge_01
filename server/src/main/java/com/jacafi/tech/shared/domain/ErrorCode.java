package com.jacafi.tech.shared.domain;

public enum ErrorCode {
    INTERNAL_ERROR("GEN-001", "Internal error. Please provide the trace identifier."),
    MALFORMED_BODY("GEN-002", "Malformed request body."),
    INVALID_PARAMETER("GEN-003", "Invalid request parameter."),
    VALIDATION_FAILED("GEN-004", "One or more fields are invalid."),
    DATA_CONFLICT("GEN-005", "The operation conflicts with existing data."),
    METHOD_NOT_ALLOWED("GEN-006", "Method not supported by this resource."),
    UNSUPPORTED_MEDIA_TYPE("GEN-007", "Unsupported content type."),
    RESOURCE_NOT_FOUND("GEN-008", "Resource not found."),

    AUTHENTICATION_REQUIRED("SEC-001", "Authentication required."),
    ACCESS_DENIED("SEC-002", "Access denied for this operation."),

    INVALID_PAGING("PAG-001", "Invalid paging or sorting parameters."),

    VEHICLE_NOT_FOUND("VEH-001", "Vehicle not found."),
    DUPLICATE_LICENSE_PLATE("VEH-002", "License plate already registered to another active vehicle."),

    INVALID_LICENSE_PLATE("VEH-003", "Invalid license plate: use the ABC1234 or ABC1D23 format."),
    VEHICLE_QUERY_AMBIGUOUS("VEH-004", "Provide exactly one of license plate or customer identifier."),

    CUSTOMER_NOT_FOUND("CUS-001", "Customer not found."),
    CUSTOMER_ALREADY_EXISTS("CUS-002", "A customer with this CPF or CNPJ already exists."),

    INVALID_TAX_ID("CUS-003", "Invalid CPF or CNPJ."),

    INVENTORY_ITEM_NOT_FOUND("INV-001", "Inventory item not found."),
    RESERVATION_NOT_FOUND("INV-002", "Inventory reservation not found."),
    DUPLICATE_MATERIAL("INV-003", "Material already registered."),
    INSUFFICIENT_STOCK("INV-004", "Insufficient stock."),

    LABOR_OPERATION_NOT_FOUND("LAB-001", "Labor operation not found."),
    DUPLICATE_LABOR_OPERATION("LAB-002", "An active labor operation with this name already exists."),

    SERVICE_ORDER_NOT_FOUND("SO-001", "Service order not found.");

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
