package sssm.dev.sssm_backend.a01_common.errors;
public record ItemError(
    String message,
    String detail,
    String code
) {}
