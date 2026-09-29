package com.ravenherz.optideployer;

import java.io.IOException;
import java.nio.file.Path;

public interface ManagerClient {

    String deploy(Path warFile) throws IOException;
}
