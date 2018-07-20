package com.forivall.util

class ResettableFileInputStream extends InputStream {
  protected String name
  protected FileInputStream s

  ResettableFileInputStream(String name) {
    this.name = name
    s = new FileInputStream(name)
  }

  int read() throws IOException {
    return s.read()
  }

  public int read(byte[] b) {
    return s.read(b)
  }

  public int read(byte[] b, int off, int len) {
    return s.read(b, off, len)
  }

  public long skip(long n) {
    return s.skip(n)
  }

  public int available() {
    return s.available()
  }

  public void close() {
    s.close()
  }

  public final FileDescriptor getFD() {
    return s.getFD()
  }

  public void reset() {
    s.close()
    s = new FileInputStream(name)
  }
}
