package cn.wildfirechat.Server;

import cn.wildfirechat.bus.MBassador;
import cn.wildfirechat.bus.SyncMessageBus;
import cn.wildfirechat.bus.config.BusConfiguration;
import cn.wildfirechat.bus.config.Feature;
import cn.wildfirechat.bus.config.IBusConfiguration;
import cn.wildfirechat.bus.error.IPublicationErrorHandler;
import cn.wildfirechat.bus.error.PublicationError;

public class Server {
    private final static String BANNER =
        "            _  _      _   __  _                    _             _   \n" +
            " __      __(_)| |  __| | / _|(_) _ __  ___    ___ | |__    __ _ | |_ \n" +
            " \\ \\ /\\ / /| || | / _` || |_ | || '__|/ _ \\  / __|| '_ \\  / _` || __|\n" +
            "  \\ V  V / | || || (_| ||  _|| || |  |  __/ | (__ | | | || (_| || |_ \n" +
            "   \\_/\\_/  |_||_| \\__,_||_|  |_||_|   \\___|  \\___||_| |_| \\__,_| \\__|\n";


    public static void main(String[] args) {
        System.out.println(BANNER);

        // Create a bus instance configured with reasonable defaults
        // NOTE: Since there is no publication error handler provided, the bus will fall back to
        // ConsoleLogger and print a hint about how to add publication error handlers
        MBassador unboundBus = new MBassador();

        // Create a bus bound to handle messages of type String.class only
        // with a custom publication error handler
        MBassador<String> stringOnlyBus = new MBassador<String>(new IPublicationErrorHandler() {
            @Override
            public void handleError(PublicationError error) {
                // custom error handling logic here
            }
        });


        // Use feature driven configuration to have more control over the configuration details
        MBassador featureDrivenBus = new MBassador(new BusConfiguration()
            .addFeature(Feature.SyncPubSub.Default())
            .addFeature(Feature.AsynchronousHandlerInvocation.Default())
            .addFeature(Feature.AsynchronousMessageDispatch.Default())
            .addPublicationErrorHandler(new IPublicationErrorHandler.ConsoleLogger())
            .setProperty(IBusConfiguration.Properties.BusId, "global bus")); // this is used for identification in #toString

        // The same construction patterns work for the synchronous message bus
        SyncMessageBus synchronousOnly = new SyncMessageBus();
    }
}
