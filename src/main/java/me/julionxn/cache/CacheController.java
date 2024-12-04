package me.julionxn.cache;

import me.julionxn.CoreLogger;

import java.nio.file.Path;

public class CacheController {

    private final CoreLogger logger;
    private final Path cacheFolder;

    public CacheController(CoreLogger logger, Path cacheFolder){
        this.logger = logger;
        this.cacheFolder = cacheFolder;
    }

    public void initialize(){

    }


}
