package com.forivall.boomi

import java.util.logging.Logger

class ExecutionUtil {
  static Logger _logger = Logger.getGlobal()

  public static getBaseLogger() {
    return _logger
  }
  public static getDynamicProcessProperty(String key) {
    return ExecutionManager.getCurrent().getProperties().getProperty(key)
  }
  public static void setDynamicProcessProperty(String key, String value, boolean persist) {
    if (persist) _logger.info("Will persist DPP ${key}")
    if (value != null) ExecutionManager.getCurrent().getProperties().setProperty(key, value);
  }
}