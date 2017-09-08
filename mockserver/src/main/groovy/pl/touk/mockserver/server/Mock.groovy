package pl.touk.mockserver.server

import groovy.transform.EqualsAndHashCode
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import pl.touk.mockserver.api.common.Method

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator
import java.util.concurrent.CopyOnWriteArrayList

@PackageScope
@EqualsAndHashCode(excludes = ["counter"])
@Slf4j
class Mock implements Comparable<Mock> {
    final String name
    final String path
    final int port
    String predicateClosureText = '{ _ -> true }'
    String responseClosureText = '''{ _ -> '' }'''
    String responseHeadersClosureText = '{ _ -> [:] }'
    Closure predicate = toClosure(predicateClosureText)
    Closure response = toClosure(responseClosureText)
    Closure responseHeaders = toClosure(responseHeadersClosureText)
    boolean soap = false
    int statusCode = 200
    Method method = Method.POST
    int counter = 0
    final List<MockEvent> history = new CopyOnWriteArrayList<>()
    String schema
    private Validator validator
    Map<String, String> imports = [:]
    boolean preserveHistory = true

    Mock(String name, String path, int port) {
        if (!(name)) {
            throw new RuntimeException("Mock name must be given")
        }
        this.name = name
        this.path = stripLeadingPath(path)
        this.port = port
    }

    private static String stripLeadingPath(String path) {
        if (path?.startsWith('/')) {
            return path - '/'
        } else {
            return path
        }
    }

    boolean match(Method method, MockRequest request) {
        return this.method == method && predicate(request)
    }

    MockResponse apply(MockRequest request) {
        log.debug("Mock $name invoked")
        if (validator) {
            try {
                log.debug('Validating...')
                if (soap) {
                    validator.validate(new StreamSource(new StringReader(request.textWithoutSoap)))
                } else {
                    validator.validate(new StreamSource(new StringReader(request.text)))
                }
            } catch (Exception e) {
                MockResponse response = new MockResponse(400, e.message, [:])
                if(preserveHistory) {
                    history << new MockEvent(request, response)
                }
                return response
            }
        }
        ++counter
        String responseText = response(request)
        String response = soap ? wrapSoap(responseText) : responseText
        Map<String, String> headers = responseHeaders(request)
        MockResponse mockResponse = new MockResponse(statusCode, response, headers)
        if(preserveHistory) {
            history << new MockEvent(request, mockResponse)
        }
        return mockResponse
    }

    private static String wrapSoap(String request) {
        """<?xml version='1.0' encoding='UTF-8'?>
            <soap-env:Envelope xmlns:soap-env='http://schemas.xmlsoap.org/soap/envelope/' xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing">
                <soap-env:Body>${request}</soap-env:Body>
            </soap-env:Envelope>"""
    }

    void setPredicate(String predicate) {
        if (predicate) {
            this.predicateClosureText = predicate
            this.predicate = toClosure(predicate)
        }
    }

    private Closure toClosure(String predicate) {
        if (predicate ==~ /(?m).*System\s*\.\s*exit\s*\(.*/) {
            throw new RuntimeException('System.exit is forbidden')
        }
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration()
        ImportCustomizer customizer = new ImportCustomizer()
        imports.each {
            customizer.addImport(it.key, it.value)
        }
        compilerConfiguration.addCompilationCustomizers(customizer)
        GroovyShell sh = new GroovyShell(this.class.classLoader, compilerConfiguration);
        Closure closure = sh.evaluate(predicate) as Closure
        sh.resetLoadedClasses()
        return closure
    }

    void setResponse(String response) {
        if (response) {
            this.responseClosureText = response
            this.response = toClosure(response)
        }
    }

    void setSoap(Boolean soap) {
        this.soap = soap ?: false
    }

    void setStatusCode(String statusCode) {
        if (statusCode) {
            this.statusCode = Integer.valueOf(statusCode)
        }
    }

    void setMethod(Method method) {
        if (method) {
            this.method = method
        }
    }

    void setResponseHeaders(String responseHeaders) {
        if (responseHeaders) {
            this.responseHeadersClosureText = responseHeaders
            this.responseHeaders = toClosure(responseHeaders)
        }
    }

    @Override
    int compareTo(Mock o) {
        return name.compareTo(o.name)
    }

    void setSchema(String schema) {
        this.schema = schema
        if (schema) {
            try {
                validator = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                        .newSchema(new File(this.class.getResource("/$schema").path))
                        .newValidator()
            } catch (Exception e) {
                throw new RuntimeException('mock request schema is invalid schema', e)
            }
        }
    }
}
