package me.dingtou.options.gateway.futu;

import com.google.protobuf.GeneratedMessageV3;

public class ReqContext {
    public final Object syncEvent = new Object();
    public int seqNo;
    public int protoID;
    public boolean done = false;
    public GeneratedMessageV3 resp;

    ReqContext() {
    }
}
