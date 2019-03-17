package cn.wildfirechat.subscription;

import cn.wildfirechat.bus.BusRuntime;
import cn.wildfirechat.listener.MetadataReader;

public interface ISubscriptionManagerProvider {
	SubscriptionManager createManager(MetadataReader reader,
			SubscriptionFactory factory, BusRuntime runtime);
}
