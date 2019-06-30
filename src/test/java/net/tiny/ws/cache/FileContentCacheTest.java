package net.tiny.ws.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class FileContentCacheTest {

    @Test
    public void testGet() throws Exception {
        Path cachePath = Paths.get("src/test/resources/cache");
        if (!Files.exists(cachePath)) {
            cachePath = Files.createDirectory(cachePath);
        }
        assertTrue(Files.isDirectory(cachePath));

        for (int i=1; i<=5; i++) {
            final String name = "src/test/resources/cache/file" + i;
            Path p = Paths.get(name);
            if (!Files.exists(p)) {
                Files.createFile(p);
                Files.write(p, Arrays.asList(name),
                        Charset.forName("UTF-8"), StandardOpenOption.WRITE);
            }
        }

        // Cache max 3 files
        FileContentCache cache = new FileContentCache(3);
        assertTrue(cache.get("src/test/resources/cache/file1").length > 0);
        assertTrue(cache.get("src/test/resources/cache/file2").length > 0);
        assertTrue(cache.get("src/test/resources/cache/file3").length > 0);
        assertTrue(cache.get("src/test/resources/cache/file4").length > 0);
        assertTrue(cache.get("src/test/resources/cache/file5").length > 0);
        assertEquals("Cache(3/3)", cache.toString());

        try {
            cache.get("src/test/resources/cache/file6");
        } catch(Exception e) {
            assertTrue(e instanceof FileNotFoundException);
            assertTrue(e.getMessage().contains("file6"));
        }

        cache.clear();
    }
}
