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
  public getDefinedProperty(String componentId, String propId) {
    return this._props.get(buildPropertyName(componentId, propId));
  }
  private static String buildPropertyName(String componentId, String propertyKey) {
    return componentId + propertyKey;
  }
}
