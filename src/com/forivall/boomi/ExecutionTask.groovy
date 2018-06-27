package com.forivall.boomi

class ExecutionTask {
  private ExecutionManager manager
  public ExecutionTask(ExecutionManager manager_) {
    manager = manager_
  }
  public Properties getProperties() {
    return this.manager._props
  }
}
