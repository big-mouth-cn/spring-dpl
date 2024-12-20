package io.github.bigmouthcn.spring.dpl.dir;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author allen
 * @date 2019/6/28
 * @since 1.0.0
 */
@Slf4j
@ToString
public final class Snapshot {

    private static final String DEFAULT_INCLUDE_SUFFIX = ".jar";

    private final Map<String, Long> files = new HashMap<>();

    public Snapshot(String dirName) {
        File dir = new File(dirName);
        if (dir.isDirectory()) {
            File[] includes = dir.listFiles((dir1, name) -> isPlugInFile(name));
            if (ArrayUtils.isEmpty(includes)) {
                return;
            }
            for (File file : includes) {
                files.put(file.getAbsolutePath(), file.lastModified());
            }
        } else {
            log.error("Dir Snapshot: [" + dirName + "] is !NOT! directory.");
            throw new RuntimeException("[" + dirName + "] is !NOT! directory.");
        }
    }

    public Map<String, Long> getFiles() {
        return files;
    }

    private boolean isPlugInFile(String fileName) {
        return fileName.endsWith(DEFAULT_INCLUDE_SUFFIX);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((files == null) ? 0 : files.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Snapshot)) {
            return false;
        }
        final Snapshot other = (Snapshot) obj;

        if (files == null) {
            if (other.files != null) {
                return false;
            }
        } else if (!files.equals(other.files)) {
            return false;
        }
        return true;
    }
}
