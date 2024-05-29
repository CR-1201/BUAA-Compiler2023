package error;

import java.util.ArrayList;
import java.util.Collections;

class Error implements Comparable<Error> {
    public ErrorType type;
    public Integer ErrorLine;

    public Error(ErrorType type, Integer errorLine) {
        this.type = type;
        ErrorLine = errorLine;
    }

    @Override
    public int compareTo(Error error) {
        return this.ErrorLine - error.ErrorLine;
    }

    @Override
    public String toString() {
        return this.ErrorLine + " " + ErrorType.toCode(this.type) + "\n";
    }
}


public class SyntaxError {
    ArrayList<Error> errors;

    public SyntaxError() {
        this.errors = new ArrayList<>();
    }

    public void addError(ErrorType type, Integer line) {
        this.errors.add(new Error(type, line));
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        Collections.sort(errors);
        for (Error error : errors) {
            res.append(error);
        }
        return res.toString();
    }
}

