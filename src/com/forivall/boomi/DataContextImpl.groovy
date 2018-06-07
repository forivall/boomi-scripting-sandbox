#!/usr/bin/env groovy -cp "vendor/boomi/*"
package com.forivall.boomi

import com.boomi.connector.api.Payload
import com.boomi.store.BaseData

interface DataContextImpl {
  public void setCombineAll(boolean combineAll)

  public List<Properties> getMetaDataList()

  public List<InputStream> getStreamList()

  public boolean isUsed()

  public int getDataCount()
  public BaseData getData(int index)

  public InputStream getStream(int index)

  public Properties getProperties(int index)

  public void storeStream(InputStream stream, Properties properties) throws Exception

  public void storePayload(Payload payload, Properties properties) throws Exception

  void addUnusedDocuments()
}
