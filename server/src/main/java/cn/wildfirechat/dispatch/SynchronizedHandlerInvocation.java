package cn.wildfirechat.dispatch;

import cn.wildfirechat.subscription.AbstractSubscriptionContextAware;
import cn.wildfirechat.bus.MessagePublication;

/**
 * Synchronizes message handler invocations for all handlers that specify @Synchronized
 *
 * @author bennidi
 *         Date: 3/31/13
 */
public class SynchronizedHandlerInvocation extends AbstractSubscriptionContextAware implements IHandlerInvocation<Object,Object>  {

    private IHandlerInvocation delegate;

    public SynchronizedHandlerInvocation(IHandlerInvocation delegate) {
        super(delegate.getContext());
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(final Object listener, final Object message, MessagePublication publication){
        synchronized (listener){
            delegate.invoke(listener, message, publication);
        }
    }

}
