package pl.touk.mockserver.client

class InvalidMockDefinitionException extends RuntimeException{
    InvalidMockDefinitionException(String s) {
        super(s)
    }
}
