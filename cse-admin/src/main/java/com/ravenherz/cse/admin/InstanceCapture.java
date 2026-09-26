package com.ravenherz.cse.admin;

/**
 * One sample of host, CPU, memory, disks, and GPUs. The WAR probes the machine.
 * The returned object is the template and JSON body; this module does not read its fields.
 */
public interface InstanceCapture {

    Object capture();
}
