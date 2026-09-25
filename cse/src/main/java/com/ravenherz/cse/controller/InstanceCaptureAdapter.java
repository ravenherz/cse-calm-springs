package com.ravenherz.cse.controller;

import com.ravenherz.cse.admin.InstanceCapture;
import com.ravenherz.cse.engine.io.InstanceProbe;
import org.springframework.stereotype.Component;

@Component
public class InstanceCaptureAdapter implements InstanceCapture {

    private final InstanceProbe instanceProbe;

    public InstanceCaptureAdapter(InstanceProbe instanceProbe) {
        this.instanceProbe = instanceProbe;
    }

    @Override
    public Object capture() {
        return instanceProbe.capture();
    }
}
