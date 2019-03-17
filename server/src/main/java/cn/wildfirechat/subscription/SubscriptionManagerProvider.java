package cn.wildfirechat.subscription;

import cn.wildfirechat.bus.BusRuntime;
import cn.wildfirechat.listener.MetadataReader;

public class SubscriptionManagerProvider implements ISubscriptionManagerProvider {
	@Override
	public SubscriptionManager createManager(MetadataReader reader,
			SubscriptionFactory factory, BusRuntime runtime) {
		return new SubscriptionManager(reader, factory, runtime);
	}
}
