package cn.wildfirechat.dispatch;

import cn.wildfirechat.subscription.AbstractSubscriptionContextAware;
import cn.wildfirechat.subscription.SubscriptionContext;
import cn.wildfirechat.bus.MessagePublication;

/**
 * Standard implementation for direct, unfiltered message delivery.
 * <p/>
 * For each message delivery, this dispatcher iterates over the listeners
 * and uses the previously provided handler invocation to deliver the message
 * to each listener
 *
 * @author bennidi
 *         Date: 11/23/12
 */
public class MessageDispatcher extends AbstractSubscriptionContextAware implements IMessageDispatcher {

    private final IHandlerInvocation invocation;

    public MessageDispatcher(SubscriptionContext context, IHandlerInvocation invocation) {
        super(context);
        this.invocation = invocation;
    }

    @Override
    public void dispatch(final MessagePublication publication, final Object message, final Iterable listeners){
        publication.markDispatched();
        for (Object listener : listeners) {
            getInvocation().invoke(listener, message, publication);
        }
    }

    @Override
    public IHandlerInvocation getInvocation() {
        return invocation;
    }
}
