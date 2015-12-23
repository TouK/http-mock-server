package pl.touk.mockserver.server

import groovy.util.logging.Slf4j

@Slf4j
class Main {
    static void main(String[] args) {
        HttpMockServer httpMockServer = startMockServer(args)

        Runtime.runtime.addShutdownHook(new Thread({
            log.info('Http server is stopping...')
            httpMockServer.stop()
            log.info('Http server is stopped')
        } as Runnable))

        while (true) {
            Thread.sleep(10000)
        }
    }

    private static HttpMockServer startMockServer(String... args) {
        switch (args.length) {
            case 1:
                return new HttpMockServer(args[0] as int)
            case 2:
                return new HttpMockServer(args[0] as int, new ConfigSlurper().parse(new File(args[1]).toURI().toURL()))
            default:
                return new HttpMockServer()
        }
    }
}
