package com.ravenherz.cse.transfer;

/**
 * Rebuild the in-memory resource group tree after an import.
 * {@code ResourceGroupIndex} in the WAR is the implementation.
 */
public interface ResourceGroupRebuild {

    void rebuild();
}
