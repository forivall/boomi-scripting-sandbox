package com.forivall.boomi

class ExecutionManager {
  static def singleton = new ExecutionManager()
  static def singletonTask = new ExecutionTask(singleton)
  def _props = new Properties()
  public getProperties() {
    return _props
  }
  public static getCurrent() {
    return singletonTask
  }
}
