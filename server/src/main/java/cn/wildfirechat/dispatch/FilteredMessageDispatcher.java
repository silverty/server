package cn.wildfirechat.dispatch;

import cn.wildfirechat.listener.IMessageFilter;
import cn.wildfirechat.bus.MessagePublication;

/**
 * A dispatcher that implements message filtering based on the filter configuration
 * of the associated message handler. It will delegate message delivery to another
 * message dispatcher after having performed the filtering logic.
 *
 * @author bennidi
 *         Date: 11/23/12
 */
public final class FilteredMessageDispatcher extends DelegatingMessageDispatcher {

    private final IMessageFilter[] filter;

    public FilteredMessageDispatcher(IMessageDispatcher dispatcher) {
        super(dispatcher);
        this.filter = dispatcher.getContext().getHandler().getFilter();
    }

    private boolean passesFilter(Object message) {

        if (filter == null) {
            return true;
        } else {
            for (IMessageFilter aFilter : filter) {
                if (!aFilter.accepts(message, getContext())) {
                    return false;
                }
            }
            return true;
        }
    }


    @Override
    public void dispatch(MessagePublication publication, Object message, Iterable listeners){
        if (passesFilter(message)) {
            getDelegate().dispatch(publication, message, listeners);
        }
    }

}
