package nsdw.vertiv.utils.sockets

import org.slf4j.LoggerFactory

class SocketUtil {
    static final def log = LoggerFactory.getLogger(this.class)

    // -- port allocation --
    static Integer tryAllocatePort(boolean throwOnError) {
        try (ServerSocket socket = new ServerSocket(0);) {
            return socket.getLocalPort();
        } catch(Throwable t) {
            log.error("Failed to allocate port", t)
            if(throwOnError) {
                throw t;
            } else return null;
        }
    }

    static Integer tryAllocatePort(int attempts, boolean throwOnError) {
        def caught = null;
        for(int i = 0; i<attempts; i++) {
            try {
                def port = tryAllocatePort(true)
                if(port!=null) {
                    return port;
                }
            } catch(Throwable t) {
                caught = t;
            }
        }
        if(throwOnError){
            throw caught;
        }
        return null;
    }
}
