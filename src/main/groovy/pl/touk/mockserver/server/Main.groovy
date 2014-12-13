package pl.touk.mockserver.server

class Main {
    static void main(String[] args) {
        HttpMockServer httpMockServer = new HttpMockServer()

        Runtime.runtime.addShutdownHook(new Thread({
            println 'Http server is stopping...'
            httpMockServer.stop()
            println 'Http server is stopped'
        } as Runnable))

        while (true) {
            Thread.sleep(10000)
        }
    }
}
