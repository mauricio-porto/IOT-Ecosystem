package com.hp.myidea.obdproxy;

interface IMessageListener {
    void processMessage(in String from, in String msg);
}