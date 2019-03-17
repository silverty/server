package cn.wildfirechat.bus.publication;

import cn.wildfirechat.bus.IMessagePublication;

/**
 * A publication command is used as an intermediate object created by a call to the message bus' post method.
 * It encapsulates the message publication flavors provided by the message bus implementation that created the command.
 * Subclasses may extend this interface and add functionality, e.g. different dispatch schemes.
 */
public interface IPublicationCommand {

    /**
     * Execute the message publication immediately. This call blocks until every matching message handler
     * has been invoked.
     */
    IMessagePublication now();
}
