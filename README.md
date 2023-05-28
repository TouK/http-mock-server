[![Build Status](https://img.shields.io/travis/TouK/http-mock-server/master.svg?style=flat)](https://travis-ci.org/TouK/http-mock-server)

HTTP MOCK SERVER
================

Http Mock Server allows to mock HTTP request using groovy closures.

Create server jar
-----------------

```
cd mockserver
mvn clean package assembly:single
```

Start server
------------

### Native start

```
java -jar mockserver-full.jar [PORT] [CONFIGURATION_FILE]
```

Default port is 9999.

If configuration file is passed then port must be defined.

Configuration file is groovy configuration script e.g. :

```groovy
testRest2 {
    port=9998
    response='{ req -> \'<response/>\' }'
    responseHeaders='{ _ -> [a: "b"] }'
    path='testEndpoint'
    predicate='{ req -> req.xml.name() == \'request1\'}'
    name='testRest2'
}
testRest4 {
    soap=true
    port=9999
    path='testEndpoint'
    name='testRest4'
    method='PUT'
    statusCode=204
}
testRest3 {
    port=9999
    path='testEndpoint2'
    name='testRest3'
}
testRest6 {
    port=9999
    path='testEndpoint2'
    name='testRest6'
    maxUses=1
    cyclic=true
}
testRest {
    imports {
        aaa='bbb'
        ccc='bla'
    }
    port=10001
    path='testEndpoint'
    name='testRest'
}
testHttps {
    soap=false
    port=10443
    path='testHttps'
    name='testHttps'
    method='GET'
    https={
        keystorePath='/tmp/keystore.jks'
        keystorePassword='keystorePass'
        keyPassword='keyPass'
        truststorePath='/tmp/truststore.jks'
        truststorePassword='truststorePass'
        requireClientAuth=true
    }
}
```

### Build with docker

Docker and docker-compose is needed.

```
./buildImage.sh
docker-compose up -d
```

### Docker repoository

Built image is available at https://hub.docker.com/r/alien11689/mockserver/

Create mock on server
---------------------

### Via client

```java
RemoteMockServer remoteMockServer = new RemoteMockServer('localhost', <PORT>)
remoteMockServer.addMock(new AddMock(
                    name: '...',
                    path: '...',
                    port: ...,
                    predicate: '''...''',
                    response: '''...''',
                    soap: ...,
                    statusCode: ...,
                    method: ...,
                    responseHeaders: ...,
                    schema: ...,
                    maxUses: ...,
                    cyclic: ...,
                    https: new Https(
                            keystorePath: '/tmp/keystore.jks',
                            keystorePassword: 'keystorePass',
                            keyPassword: 'keyPass',
                            truststorePath: '/tmp/truststore.jks',
                            truststorePassword: 'truststorePass',
                            requireClientAuth: true
                    )
            ))
```

### Via HTTP

Send POST request to localhost:<PORT>/serverControl

```xml
<addMock xmlns="http://touk.pl/mockserver/api/request">
    <name>...</name>
    <path>...</path>
    <port>...</port>
    <predicate>...</predicate>
    <response>...</response>
    <soap>...</soap>
    <statusCode>...</statusCode>
    <method>...</method>
    <responseHeaders>...</responseHeaders>
    <schema>...</schema>
    <imports alias="..." fullClassName="..."/>
    <maxUses>...</maxUses>
    <cyclic>...</cyclic>
    <https>
        <keystorePath>/tmp/keystore.jks</keystorePath>
        <keystorePassword>keystorePass</keystorePassword>
        <keyPassword>keyPass</keyPassword>
        <truststorePath>/tmp/truststore.jks</truststorePath>
        <truststorePassword>truststorePass</truststorePassword>
        <requireClientAuth>true</requireClientAuth>
    </https>
</addMock>
```

### Parameters

-	name - name of mock, must be unique
-	path - path on which mock should be created
-	port - inteer, port on which mock should be created, cannot be the same as mock server port
-	predicate - groovy closure as string which must evaluate to true, when request object will be given to satisfy mock, optional, default {_ -> true}
-	response - groovy closure as string which must evaluate to string which will be response of mock when predicate is satisfied, optional, default { _ -> '' }
-	soap - true or false, is request and response should be wrapped in soap Envelope and Body elements, default false
-	statusCode - integer, status code of response when predicate is satisfied, default 200
-	method - POST|PUT|DELETE|GET|TRACE|OPTION|HEAD|ANY_METHOD, expected http method of request, default `POST`, `ANY_METHOD` matches all HTTP methods
-	responseHeaders - groovyClosure as string which must evaluate to Map which will be added to response headers, default { _ -> \[:] }
-	schema - path to xsd schema file on mockserver classpath; default empty, so no vallidation of request is performed; if validation fails then response has got status 400 and response is raw message from validator
-	imports - list of imports for closures (each import is separate tag); `alias` is the name of `fullClassName` available in closure; `fullClassName` must be available on classpath of mock server
-   https - HTTPS configuration
-   maxUses - limit uses of mock to the specific number, after that mock is marked as ignored (any negative number means unlimited - default, cannot set value to 0), after this number of invocation mock history is still available, but mock does not apply to any request 
-   cyclic - should mock be added after `maxUses` uses at the end of the mock list (by default false)

#### HTTPS configuration

-   keystorePath - path to keystore in JKS format, keystore should contains only one privateKeyEntry
-   keystorePassword - keystore password
-   keyPassword - key password
-   truststorePath - path to truststore in JKS format
-   truststorePassword - truststore password
-   requireClientAuth - whether client auth is required (two-way SSL)

**HTTP** and **HTTPS** should be started on separated ports.

### Closures request properties

In closures input parameter (called req) contains properties:

-	text - request body as java.util.String
-	headers - java.util.Map with request headers
-	query - java.util.Map with query parameters
-	xml - groovy.util.slurpersupport.GPathResult created from request body (if request body is valid xml)
-	soap - groovy.util.slurpersupport.GPathResult created from request body without Envelope and Body elements (if request body is valid soap xml)
-	json - java.lang.Object created from request body (if request body is valid json)
-	path - java.util.List<String> with not empty parts of request path

Response if success:

```xml
<mockAdded xmlns="http://touk.pl/mockserver/api/response"/>
```

Response with error message if failure:

```xml
<exceptionOccured xmlns="http://touk.pl/mockserver/api/response">...</exceptionOccured>
```

Peek mock
---------

Mock could be peeked to get get report of its invocations.

### Via client

```java
List<MockEvent> mockEvents = remoteMockServer.peekMock('...')
```

### Via HTTP

Send POST request to localhost:<PORT>/serverControl

```xml
<peekMock xmlns="http://touk.pl/mockserver/api/request">
  <name>...</name>
</peekMock>
```

Response if success:

```xml
<mockPeeked xmlns="http://touk.pl/mockserver/api/response">
  <mockEvent>
    <request>
      <text>...</text>
      <headers>
        <header name="...">...</header>
        ...
      </headers>
      <queryParams>
        <queryParam name="...">...</queryParam>
        ...
      </queryParams>
      <path>
        <pathPart>...</pathPart>
        ...
      </path>
    </request>
    <response>
      <statusCode>...</statusCode>
      <text>...</text>
      <headers>
        <header name="...">...</header>
        ...
      </headers>
    </response>
  </mockEvent>
</mockPeeked>
```

Response with error message if failure:

```xml
<exceptionOccured xmlns="http://touk.pl/mockserver/api/response">...</exceptionOccured>
```

Remove mock
-----------

When mock was used it could be unregistered by name. It also optionally returns report of mock invocations if second parameter is true.

### Via client

```java
List<MockEvent> mockEvents = remoteMockServer.removeMock('...', ...)
```

### Via HTTP

Send POST request to localhost:<PORT>/serverControl

```xml
<removeMock xmlns="http://touk.pl/mockserver/api/request">
    <name>...</name>
    <skipReport>...</skipReport>
</removeMock>
```

Response if success (and skipReport not given or equal false):

```xml
<mockRemoved xmlns="http://touk.pl/mockserver/api/response">
  <mockEvent>
    <request>
      <text>...</text>
      <headers>
        <header name="...">...</header>
        ...
      </headers>
      <queryParams>
        <queryParam name="...">...</queryParam>
        ...
      </queryParams>
      <path>
        <pathPart>...</pathPart>
        ...
      </path>
    </request>
    <response>
      <statusCode>...</statusCode>
      <text>...</text>
      <headers>
        <header name="...">...</header>
        ...
      </headers>
    </response>
  </mockEvent>
</mockRemoved>
```

If skipReport is set to true then response will be:

```xml
<mockRemoved xmlns="http://touk.pl/mockserver/api/response"/>
```

Response with error message if failure:

```xml
<exceptionOccured xmlns="http://touk.pl/mockserver/api/response">...</exceptionOccured>
```

List mocks definitions
----------------------

### Via client

```java
List<RegisteredMock> mocks = remoteMockServer.listMocks()
```

### Via HTTP

Send GET request to localhost:<PORT>/serverControl

Response:

```xml
<mocks>
  <mock>
    <name>...</name>
    <path>...</path>
    <port>...</port>
    <predicate>...</predicate>
    <response>...</response>
    <responseHeaders>...</responseHeaders>
    <soap>...</soap>
    <method>...</method>
    <statusCode>...</statusCode>
    <imports alias="..." fullClassName="..."/>
  </mock>
  ...
</mocks>
```

Get mocks configuration
-----------------------

### Via client

```java
ConfigObject mocks = remoteMockServer.getConfiguration()
```

### Via HTTP

Send GET request to localhost:<PORT>/serverControl/configuration

Response:

```groovy
testRest2 {
    port=9998
    response='{ req -> \'<response/>\' }'
    responseHeaders='{ _ -> [a: "b"] }'
    path='testEndpoint'
    predicate='{ req -> req.xml.name() == \'request1\'}'
    name='testRest2'
}
testRest4 {
    soap=true
    port=9999
    path='testEndpoint'
    name='testRest4'
    method='PUT'
    statusCode=204
}
testRest3 {
    port=9999
    path='testEndpoint2'
    name='testRest3'
}
testRest6 {
    port=9999
    path='testEndpoint2'
    name='testRest6'
}
testRest {
    imports {
        aaa='bbb'
        ccc='bla'
    }
    port=10001
    path='testEndpoint'
    name='testRest'
}
```

This response could be saved to file and passed as it is during mock server creation.

Remote repository
-----------------

Mockserver is available at `philanthropist.touk.pl`.

Just add repository to maven pom:

```xml
<project>
    ...
    <repositories>
        ...
        <repository>
            <id>touk</id>
            <url>https://philanthropist.touk.pl/nexus/content/repositories/releases</url>
        </repository>
    ...
    </repositories>
    ...
</project>
```

FAQ
---

Q: *Can I have two mocks returning responses interchangeably for the same request?*
A: Yes, you can. Just set two mocks with `maxUses: 1` and `cyclic: true`.
