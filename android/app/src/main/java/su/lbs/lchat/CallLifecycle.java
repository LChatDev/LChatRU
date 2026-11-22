package su.lbs.lchat;

/** Thread-confined session lifetime. Every async completion must belong to its owner. */
final class CallLifecycle {
    enum Phase { STARTING, JOINING, CONNECTING, CONNECTED, RECONNECTING, STOPPING, ENDED }
    private Phase phase=Phase.STARTING;
    private long connectedAt;
    Phase phase(){return phase;}
    boolean live(){return phase!=Phase.STOPPING&&phase!=Phase.ENDED;}
    boolean join(){if(phase!=Phase.STARTING)return false;phase=Phase.JOINING;return true;}
    boolean admitted(){if(phase!=Phase.JOINING)return false;phase=Phase.CONNECTING;return true;}
    boolean connected(long now){if(!live())return false;if(connectedAt==0)connectedAt=now;phase=Phase.CONNECTED;return true;}
    boolean reconnect(){if(!live())return false;phase=Phase.RECONNECTING;return true;}
    boolean waiting(){if(!live())return false;phase=Phase.CONNECTING;return true;}
    boolean stop(){if(!live())return false;phase=Phase.STOPPING;return true;}
    void ended(){phase=Phase.ENDED;}
    long connectedAt(){return connectedAt;}
}
