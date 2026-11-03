package interfaces;

import exceptions.ValidationException;

public interface Validatable {
    void validate() throws ValidationException;
}
