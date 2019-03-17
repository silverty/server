package cn.wildfirechat.bus.common;

import cn.wildfirechat.bus.BusRuntime;

/**
 * Each message bus provides a runtime object to access its dynamic features and runtime configuration.
 */
public interface RuntimeProvider {

    BusRuntime getRuntime();
}
