[![Build Status](https://img.shields.io/travis/TouK/http-mock-server/master.svg?style=flat)](https://travis-ci.org/TouK/http-mock-server)

# HTTP MOCK SERVER

## Create server jar (in mockserver directory)

```
mvn clean package assembly:single
```

## Start server on port (default 9999)

```
java -jar mockserver-<VERSION>-jar-with-dependencies.jar  [PORT]
```

## Create mock on server via client

```java
RemoteMockServer remoteMockServer = new RemoteMockServer('localhost', <PORT>)
remoteMockServer.addMock(new AddMockRequestData(
                    name: '...',
                    path: '...',
                    port: ...,
                    predicate: '''...''',
                    response: '''...''',
                    soap: ...,
                    statusCode: ...,
                    method: ...,
                    responseHeaders: ...
            ))
```
  
or via sending POST request to localhost:<PORT>/serverControl


```xml
<addMock>
    <name>...</name>
    <path>...</path>
    <port>...</port>
    <predicate>...</predicate>
    <response>...</response>
    <soap>...</soap>
    <statusCode>...</statusCode>
    <method>...</method>
    <responseHeaders>...</responseHeaders>
</addMock>
```

* name - name of mock, must be unique
* path - path on which mock should be created
* port - inteer, port on which mock should be created, cannot be the same as mock server port
* predicate - groovy closure as string which must evaluate to true, when request object will be given to satisfy mock, optional, default {_ -> true}
* response - groovy closure as string which must evaluate to string which will be response of mock when predicate is satisfied, optional, default { _ -> '' }
* soap - true or false, is request and response should be wrapped in soap Envelope and Body elements, default false
* statusCode - integer, status code of response when predicate is satisfied, default 200
* method - POST|PUT|DELETE|GET|TRACE|OPTION|HEAD, expected http method of request, default POST
* responseHeaders - groovyClosure as string which must evaluate to Map which will be added to response headers, default { _ -> [:] }

In closures input parameter (called req) contains properties:


* text - request body as java.util.String
* headers - java.util.Map with request headers
* query - java.util.Map with query parameters
* xml - groovy.util.slurpersupport.GPathResult created from request body (if request body is valid xml)
* soap - groovy.util.slurpersupport.GPathResult created from request body without Envelope and Body elements (if request body is valid soap xml)
* json - java.lang.Object created from request body (if request body is valid json)
* path - java.util.List<String> with not empty parts of request path 

Response if success:

```xml
<mockAdded/>
```

Response with error message if failure:

```xml
<exceptionOccured>...</exceptionOccured>
```

## Mock could be peeked to get get report of its invocations.
Via client:

```java
List<MockEvent> mockEvents = remoteMockServer.peekMock('...')
```

Via sending POST request to localhost:<PORT>/serverControl

```xml
<peekMock>
    <name>...</name>
</peekMock>
```

Response if success:

```xml
<mockPeeked>
  <mockEvent>
    <request>
      <text>...</text>
      <headers>
        <param name='...'>...</param>
        ...
      </headers>
      <query>
        <param name='...'>...</param>
        ...
      </query>
      <path>
        <elem>...</elem>
        ...
      </path>
    </request>
    <response>
      <text>...</text>
      <headers>
        <param name='...'>...</param>
        ...
      </headers>
      <statusCode>...</statusCode>
    </response>
  </mockEvent>
  ...
</mockPeeked>
```

Response with error message if failure:

```xml
<exceptionOccured>...</exceptionOccured>
```

## When mock was used it could be unregistered by name. It also returns report of mock invocations.
Via client:

```java
List<MockEvent> mockEvents = remoteMockServer.removeMock('...')
```

Via sending POST request to localhost:<PORT>/serverControl

```xml
<removeMock>
    <name>...</name>
</removeMock>
```

Response if success:

```xml
<mockRemoved>
  <mockEvent>
    <request>
      <text>...</text>
      <headers>
        <param name='...'>...</param>
        ...
      </headers>
      <query>
        <param name='...'>...</param>
        ...
      </query>
      <path>
        <elem>...</elem>
        ...
      </path>
    </request>
    <response>
      <text>...</text>
      <headers>
        <param name='...'>...</param>
        ...
      </headers>
      <statusCode>...</statusCode>
    </response>
  </mockEvent>
  ...
</mockRemoved>
```

Response with error message if failure:

```xml
<exceptionOccured>...</exceptionOccured>
```


## List of current registered mocks could be retrieved:
Via client:

```java
List<RegisteredMock> mocks = remoteMockServer.listMocks()
```

or via sending GET request to localhost:<PORT>/serverControl

Response:

```xml
<mocks>
  <mock>
    <name>...</name>
    <path>...</path>
    <port>...</port>
  </mock>
  ...
</mocks>
```
