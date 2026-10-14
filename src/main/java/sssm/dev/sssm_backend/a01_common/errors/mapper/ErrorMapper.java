
package sssm.dev.sssm_backend.a01_common.errors.mapper;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.stereotype.Component;

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

            if (serverError != null) {
                return new ItemError(
                    serverError.getMessage(),
                    serverError.getDetail(),
                    ex.getSQLState()
                );
            }

            return new ItemError(
                ex.getMessage(),
                null,
                ex.getSQLState()
            );
        }

        return new ItemError(
            exception.getMessage(),
            null,
            null
        );
    }
}
