package net.tiny.ws.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileContentCache {

    private final BarakbCache<String, byte[]> cache;

    /**
     * Create cache for the last capacity number used file.
     *
     * @param capacity
     */
    public FileContentCache(int capacity) {
        this.cache = new BarakbCache<>(key -> readContents(key), capacity);
        // Have an error from the file system that a file was deleted.
        this.cache.setRemoveableException(RuntimeException.class);
    }

    public void clear() {
        cache.clear();
    }

    public byte[] get(String file) throws IOException {
        try {
            return cache.get(file);
        } catch (Throwable e) {
            Throwable cause = findErrorCause(e);
            if(cause instanceof IOException) {
                throw (IOException)cause;
            } else {
                throw new IOException(cause.getMessage(), cause);
            }
        }
    }

    @Override
    public String toString() {
        return cache.toString();
    }

    private Throwable findErrorCause(Throwable err) {
        if(err instanceof IOException)
            return err;
        Throwable cause = err.getCause();
        if (null != cause) {
            return findErrorCause(cause);
        } else {
            return err;
        }
    }

    private byte[] readContents(String file) {
        try {
            return readFile(new File(file));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private byte[] readFile(File file) throws IOException {
      InputStream in = null;
      try {
          long length = file.length();
          if (length > Integer.MAX_VALUE) {
              throw new IllegalArgumentException("File is too large " + length);
          }

          in = new FileInputStream(file);
          byte[] bytes = new byte[(int) length];

          int offset = 0;
          int len;
          while (offset < bytes.length
                  && (len = in.read(bytes, offset, bytes.length - offset)) >= 0) {
              offset += len;
          }

          if (offset < bytes.length) {
              throw new IOException("Could not completely read file " + file.getName());
          }
          return bytes;
      } finally {
          if (in != null) {
              in.close();
          }
      }
  }
}
