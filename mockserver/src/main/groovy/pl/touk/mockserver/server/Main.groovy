package pl.touk.mockserver.server

import groovy.util.logging.Slf4j

@Slf4j
class Main {
    static void main(String[] args) {
        HttpMockServer httpMockServer = new HttpMockServer()

        Runtime.runtime.addShutdownHook(new Thread({
            log.info('Http server is stopping...')
            httpMockServer.stop()
            log.info('Http server is stopped')
        } as Runnable))

        while (true) {
            Thread.sleep(10000)
        }
    }
}
