package sssm.dev.sssm_backend.a11_inventory.item.mapper;

public record ItemError(
    String message,
    String detail,
    String code
) {}