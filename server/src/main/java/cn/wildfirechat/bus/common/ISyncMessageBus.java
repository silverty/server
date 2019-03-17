package cn.wildfirechat.bus.common;

import cn.wildfirechat.bus.publication.IPublicationCommand;

/**
 * @author bennidi
 *         Date: 3/29/13
 */
public interface ISyncMessageBus<T, P extends IPublicationCommand> extends PubSubSupport<T>, ErrorHandlingSupport, GenericMessagePublicationSupport<T, P>{


}
