#!/usr/bin/env groovy -cp "vendor/boomi/*"
package com.forivall.boomi

import com.boomi.connector.api.Payload
import com.boomi.connector.api.PayloadUtil
import com.boomi.document.api.InboundDocument
import com.boomi.document.scripting.DataContextImpl as BoomiDataContextImpl
import com.boomi.store.BaseData

import java.util.logging.Logger

class DataContextImpl {

  def logger = Logger.getLogger('script-runner')
  def foo = logger.info('Loading DataContextImpl')

  static interface DataContextOutput {
    OutputStream get(int index)
  }
  static class FileOutput implements DataContextOutput {
    String outPath = 'output%d.json'
    OutputStream get(int index) {
      return new FileOutputStream(String.format(outPath, index), true)
    }
  }
  static class PipeOutput implements DataContextOutput {
    OutputStream dest = System.out
    OutputStream get(int index) {
      return dest
    }
  }
  private static systemOut = new PipeOutput(dest: System.out)
  protected static logger = Logger.getLogger('DataContext')
  private boolean used;
  private List<InboundDocument> _documents
  private Integer storedCount = 0
  private DataContextOutput output

  DataContextImpl(List<InboundDocument> documents) {
    this(documents, systemOut)
  }
  DataContextImpl(List<InboundDocument> documents, String outPath) {
    this(documents, new FileOutput(outPath: outPath))
  }
  DataContextImpl(List<InboundDocument> documents, DataContextOutput output) {
    _documents = documents
    this.output = output
  }

  public void setCombineAll(boolean combineAll) { /* void */ }

  public List<Properties> getMetaDataList() {
    return new BoomiDataContextImpl.MetaDataList();
  }

  public List<InputStream> getStreamList() {
    return new BoomiDataContextImpl.StreamList();
  }

  public boolean isUsed() {
    return this.used;
  }

  public int getDataCount() {
    this.used = true;
    return this._documents.size()
  }

  public BaseData getData(int index) {
    throw new Error('Not Implemented')
  }

  public InputStream getStream(int index) {
    logger.info('getStream ' + index)
    logger.info('' + this._documents.get(index))
    return this._documents.get(index).getInputStream()
  }

  public Properties getProperties(int index) {
    this.used = true;
    def d = this._documents.get(index)
    def p = d.getProperties()
    logger.info("getProperties ${index} ${p} ${d}")
    return p
  }

  public void storeStream(InputStream stream, Properties properties) throws Exception {
    def payload = PayloadUtil.toPayload(stream)
    this.storePayload(payload, properties);
  }

  public void storePayload(Payload payload, Properties properties) throws Exception {
    println("Payload ${storedCount++}")
    properties.list(System.out)
    println('-- end properties --')

    def output = this.output.get(storedCount)
    logger.info(String.format('%s', output.toString()))

    println('-- dumping payload --')
    def input = payload.readFrom()
    int size = 0;
    byte[] buffer = new byte[1024];
    while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);

    println('\n-- end payload --')
  }

  void addUnusedDocuments() {
    println('addUnusedDocuments')
  }
}