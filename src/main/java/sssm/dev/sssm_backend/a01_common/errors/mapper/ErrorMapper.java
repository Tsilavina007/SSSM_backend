package sssm.dev.sssm_backend.a01_common.errors.mapper;

import java.util.NoSuchElementException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;

@Component
public class ErrorMapper {

    public static ItemError toItemError(Throwable exception) {
        Throwable cause = exception;

        while (cause != null && !(cause instanceof PSQLException)) {
            cause = cause.getCause();
        }

        if (cause instanceof PSQLException ex) {
            ServerErrorMessage serverError = ex.getServerErrorMessage();
            String message =
                serverError != null ? serverError.getMessage() : ex.getMessage();
            String detail = serverError != null ? serverError.getDetail() : null;

            return new ItemError(
                withDefault(message, exception.getClass().getSimpleName()),
                detail,
                withDefault(ex.getSQLState(), "DATABASE_ERROR")
            );
        }

        return new ItemError(
            withDefault(exception.getMessage(), exception.getClass().getSimpleName()),
            null,
            fallbackCode(exception)
        );
    }

    private static String fallbackCode(Throwable exception) {
        if (exception instanceof ClientException) {
            return "CLIENT_ERROR";
        }
        if (exception instanceof NotFoundException) {
            return "NOT_FOUND";
        }
        if (exception instanceof ServerException) {
            return "SERVER_ERROR";
        }
        if (exception instanceof NoSuchElementException) {
            return "NOT_FOUND";
        }
        if (exception instanceof IllegalArgumentException) {
            return "BAD_REQUEST";
        }
        return "INTERNAL_ERROR";
    }

    private static String withDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
